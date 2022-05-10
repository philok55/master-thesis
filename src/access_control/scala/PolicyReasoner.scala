package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.ViolatedAction
import norms.ExecutedAction
import norms.ActionValue
import norms.NormActor

object ReasonerDB {
  var subjects: List[ActorRef[Client.Message]] = List()
  var resources: List[Resource] = List()
}

object PolicyReasoner {
  trait Message {}
  case class RegisterResource(resource: Resource) extends Message
  case class RequestAccess(internalRequest: InternalRequest) extends Message

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.setup { context => 
    val eflint_actor = new NormActor("src/access_control/eflint/XACML/XACML-test.eflint", debug_mode=true)
    val eflint_server = context.spawn(eflint_actor.listen(), "eFLINT-actor")

    val handler_ref = context.spawn(response_handler(), "response-handler")

    listen(eflint_server, handler_ref)
  }

  def listen(eflint_server: ActorRef[NormActor.Message], handler_ref: ActorRef[norms.Message])
            (implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) => 
    message match {
      case m: RegisterResource =>
        context.log.info(s"(Reasoner) Registering resource ${m.resource.name}")
        ReasonerDB.resources = m.resource :: ReasonerDB.resources
        Behaviors.same
      case m: RequestAccess =>
        val subject_id = "\"" + resolver.toSerializationFormat(m.internalRequest.request.subject) + "\""
        val action = "\"" + m.internalRequest.request.action + "\""
        eflint_server ! NormActor.Phrase(s"+hour-of-the-day(15)")
        eflint_server ! NormActor.Phrase(
          handler = handler_ref,
          phrase = s"login-rule(subject(${subject_id}), resource(${m.internalRequest.request.resource.name}), action(${action}))"
        )
        Behaviors.same
    }
  }

  def response_handler(): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    message match {
      case ViolatedAction(ActionValue(Right(p), Left(r), action)) => {
        println(s"VIOLATED by $p affecting $r: $action")
        Behaviors.same
      }
      case ExecutedAction(ActionValue(Right(p), Left(r), action)) => {
        println(s"EXECUTED ACTION by $p affecting $r: $action")
        Behaviors.same
      }
    }
  }
}
