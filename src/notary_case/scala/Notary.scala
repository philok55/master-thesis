package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import akka.actor.typed.ActorRef
import norms.NormActor
import notary_case.data.Input

object NotaryDB {
  var properties: List[Property] = List()
  var mortgages: List[Mortgage] = List()
}

object Notary {
  trait Message extends norms.Message
  case class RequestCoveredMortgage(citizen: ActorRef[Citizen.Message], property: Property, value: Int) extends Message

  def apply(monitor: ActorRef[NimMonitor.Message])(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) =>
    val self_id = "\"" + resolver.toSerializationFormat(context.self) + "\""
    monitor ! NimMonitor.RegisterNotary(self_id)
    Input.properties.foreach(p => { monitor ! NimMonitor.RegisterProperty(p) })
    message match {
      case m:RequestCoveredMortgage =>
        context.log.info(s"${m.citizen} requested covered mortgage for property ${m.property.address} with value ${m.value}")
        val citizen_id = "\"" + resolver.toSerializationFormat(m.citizen) + "\""
        val mortgage = new Mortgage(m.property, m.citizen, m.value)
        NotaryDB.mortgages = mortgage :: NotaryDB.mortgages
        monitor ! NimMonitor.AddCoveredMortgage(self_id, mortgage)
        Behaviors.same
    }
  }
}
