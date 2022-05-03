package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.ViolatedAction
import norms.ActionValue
import norms.NormActor

object DB {
  var subjects: List[ActorRef[ClientApp.Message]] = List()
  var resources: List[String] = List()
}

object PolicyReasoner {
  trait Message {}
  case class RegisterSubject(subject: ActorRef[ClientApp.Message]) extends Message
  case class RegisterResource(resource: String) extends Message
  case class RequestAccess(request: Request) extends Message

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.setup { context => 
    val eflint_actor = new NormActor("src/access_control/eflint/XACML/XACML-test.eflint")
    val eflint_server = context.spawn(eflint_actor.listen(), "eFLINT-actor")

    val handler_ref = context.spawn(response_handler(), "response-handler")

    listen(eflint_server, handler_ref)
  }

  def listen(eflint_server: ActorRef[NormActor.Message], handler_ref: ActorRef[norms.Message])
            (implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) => 
    message match {
      case m: RegisterSubject =>
        context.log.info(s"Registering subject ${m.subject}")
        DB.subjects = m.subject :: DB.subjects
        Behaviors.same
      case m: RegisterResource =>
        context.log.info(s"Registering resource ${m.resource}")
        DB.resources = m.resource :: DB.resources
        Behaviors.same
      case m: RequestAccess =>
        val subject_id = "\"" + resolver.toSerializationFormat(m.request.subject) + "\""
        eflint_server ! NormActor.Phrase(
          phrase = s"login-rule(${subject_id}, ${m.request.resource}, ${m.request.action})",
          handler = handler_ref
        )
        Behaviors.same
    }
  }

  def response_handler(): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: ViolatedAction =>
        println(s"Policy violation: ${m}")
        Behaviors.same
    }
  }
}
