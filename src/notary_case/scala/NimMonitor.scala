package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import akka.actor.typed.ActorRef
import norms.NormActor
import notary_case.data.Input
import norms.ViolatedAction
import norms.ActionValue

object NimMonitor {
  trait Message extends norms.Message
  case class RegisterNotary(notary_id: String) extends Message
  case class RegisterProperty(property: Property) extends Message
  case class AddCoveredMortgage(notary_id: String, mortgage: Mortgage) extends Message

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.setup { context => 
    val eflint_actor = new NormActor("src/notary_case/eflint/notary.eflint")
    val eflint_server = context.spawn(eflint_actor.listen(), "eFLINT-actor")

    val handler_ref = context.spawn(response_handler(), "response-handler")

    listen(eflint_server, handler_ref)
  }

  def listen(eflint_server: ActorRef[NormActor.Message], handler_ref: ActorRef[norms.Message])
            (implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m:RegisterNotary =>
        context.log.info(s"Registering notary ${m.notary_id}")
        eflint_server ! NormActor.Phrase(s"Fact notary Identified by ${m.notary_id}")
        Behaviors.same
      case m:RegisterProperty =>
        context.log.info(s"Registering property ${m.property.address}")
        eflint_server ! NormActor.Phrase(s"+property(address(${m.property.address}), value(${m.property.value}))")
        Behaviors.same
      case m:AddCoveredMortgage =>
        context.log.info(s"${m.mortgage.citizen} requested covered mortgage for property ${m.mortgage.property.address} with value ${m.mortgage.value}")
        val citizen_id = "\"" + resolver.toSerializationFormat(m.mortgage.citizen) + "\""
        eflint_server ! NormActor.Phrase(
          handler = handler_ref,
          phrase = s"create-covered-mortgage(${m.notary_id}, ${citizen_id}, property(${m.mortgage.property.address}, ${m.mortgage.property.value}), value(${m.mortgage.value}))"
        )
        Behaviors.same
    }
  }

  def response_handler(): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    message match {
      case ViolatedAction(ActionValue(Right(p), Right(r), action)) => {
        println(s"VIOLATED by $p affecting $r: $action")
        response_handler()
      }
    }
  }
}
