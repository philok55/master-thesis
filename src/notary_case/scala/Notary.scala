package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import akka.actor.typed.ActorRef
import norms.NormActor
import notary_case.data.Input

object Notary {
  trait Message extends norms.Message
  case class LoadCity() extends Message
  case class RequestCoveredMortgage(sender: ActorRef[Citizen.Message], property: Property, value: Int) extends Message

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.setup { context => 
    val eflint_actor = new NormActor("src/notary_case/eflint/notary.eflint")
    val eflint_server = context.spawn(eflint_actor.listen(), "eFLINT-actor")

    val self_id = "\"" + resolver.toSerializationFormat(context.self) + "\""
    eflint_server ! NormActor.Phrase(s"Fact notary Identified by $self_id")

    listen(eflint_server, self_id)
  }

  def listen(eflint_server: ActorRef[NormActor.Message], self_id: String)(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m:LoadCity =>
        context.log.info(s"Loading city properties into application")
        Input.properties.foreach(b => {
          eflint_server ! NormActor.Phrase(s"+property(address(${b("Address")}), value(${b("Value")}))")
        })
        Behaviors.same
      case m:RequestCoveredMortgage =>
        context.log.info(s"${m.sender} requested covered mortgage for property ${m.property.address} with value ${m.value}")
        val sender_id = "\"" + resolver.toSerializationFormat(m.sender) + "\""
        eflint_server ! NormActor.Phrase(s"+citizen(${sender_id})")
        // XXX: Violation reporting for this action results in an exception now, but calling eFLINT works correctly
        eflint_server ! NormActor.Phrase(s"create-covered-mortgage(${self_id}, ${sender_id}, property(${m.property.address}, ${m.property.value}), value(${m.value}))")
        m.sender ! Citizen.MortgageRequestDenied("NO!")
        Behaviors.same
    }
  }
}
