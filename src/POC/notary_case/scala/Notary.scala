package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import akka.actor.typed.ActorRef
import norms.NormActor
import norms.ViolatedAction
import norms.ActionValue
import notary_case.data.Input

object NotaryDB {
  var properties: List[Property] = List()
  var mortgages: List[Mortgage] = List()
}

object Notary {
  case class RequestCoveredMortgage(citizen: ActorRef[norms.Message], property: Property, value: Int) extends norms.Message

  def apply(monitor: ActorRef[NimMonitor.Message])(implicit resolver: ActorRefResolver): Behavior[norms.Message] = Behaviors.setup { context =>
    val self_id = "\"" + resolver.toSerializationFormat(context.self) + "\""
    monitor ! NimMonitor.RegisterNotary(self_id)
    Input.properties.foreach(p => { monitor ! NimMonitor.RegisterProperty(p) })
    
    listen(monitor, self_id)
  }

  def listen(monitor: ActorRef[NimMonitor.Message], self_id: String)
            (implicit resolver: ActorRefResolver): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    context.log.info("Received message: {}", message)
    message match {
      case m:RequestCoveredMortgage =>
        val citizen_id = "\"" + resolver.toSerializationFormat(m.citizen) + "\""
        val mortgage = new Mortgage(m.property, m.citizen, m.value)
        NotaryDB.mortgages = mortgage :: NotaryDB.mortgages
        monitor ! NimMonitor.AddCoveredMortgage(self_id, mortgage)
        listen(monitor, self_id)
      case ViolatedAction(ActionValue(Right(p), Right(r), action)) => {
        println(s"(notary) VIOLATED by $p affecting $r: $action")
        listen(monitor, self_id)
      }
    }
  }
}
