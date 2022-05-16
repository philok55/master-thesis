package access_control

import scala.collection.mutable._
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.ViolatedAction
import norms.ExecutedAction
import norms.ActionValue
import norms.NormActor
import norms.ViolatedDuty
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReasonerDB {
  var resources: List[Resource] = List()
}

object PolicyReasoner {
  trait Message {}
  case class RegisterResource(resource: Resource) extends Message
  case class RequestAccess(internalRequest: InternalRequest, enforcer: ActorRef[PolicyEnforcer.Message]) extends Message
  private case class AdaptedQueryResponse(requestId: Int, success: Boolean, reason: String) extends NormActor.QueryResponse

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.setup { context => 
    val eflint_actor = new NormActor("src/access_control/eflint/XACML/XACML-test.eflint", debug_mode=true)
    val eflint_server = context.spawn(eflint_actor.listen(), "eFLINT-actor")
    listen(eflint_server)
  }

  def listen(eflint_server: ActorRef[NormActor.Message])
            (implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) => 
    message match {
      case m: RegisterResource =>
        context.log.info(s"(Reasoner) Registering resource ${m.resource.name}")
        ReasonerDB.resources = m.resource :: ReasonerDB.resources
        Behaviors.same
      case m: RequestAccess =>
        val hour = LocalDateTime.now(ZoneId.of("Europe/Amsterdam")).format(DateTimeFormatter.ofPattern("HH"))
        eflint_server ! NormActor.Phrase(s"+hour-of-the-day($hour)")
        val handler_ref = context.spawn(response_handler(m, eflint_server), s"response-handler-${m.internalRequest.id}")
        Behaviors.same
    }
  }

  def response_handler(m: RequestAccess, eflint_server: ActorRef[NormActor.Message])
      (implicit resolver: ActorRefResolver): Behavior[norms.NormActor.QueryResponse] = Behaviors.setup { context =>
    implicit val timeout: Timeout = 3.seconds
    val subject_id = "\"" + resolver.toSerializationFormat(m.internalRequest.request.subject) + "\""
    val action = "\"" + m.internalRequest.request.action + "\""
    context.ask(eflint_server, (ref: ActorRef[norms.NormActor.QueryResponse]) => NormActor.Query(
                replyTo = ref,
                phrase = s"?login-rule(subject(${subject_id}), resource(${m.internalRequest.request.resource.name}), action(${action}))")) {
      case Success(NormActor.Response(success, reason)) => AdaptedQueryResponse(m.internalRequest.id, success, reason)
      case Failure(_)                                   => AdaptedQueryResponse(m.internalRequest.id, false, "Timeout")
    }
    Behaviors.receiveMessage {
      case AdaptedQueryResponse(id, success, reason) => {
        if (success)
          m.enforcer ! PolicyEnforcer.AccessGranted(id)
        else
          m.enforcer ! PolicyEnforcer.AccessDenied(id)
        Behaviors.stopped
      }
    }
  }
}
