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
        ReasonerDB.resources = m.resource :: ReasonerDB.resources
        Behaviors.same
      case m: RequestAccess =>
        val hour = LocalDateTime.now(ZoneId.of("Europe/Amsterdam")).format(DateTimeFormatter.ofPattern("HH"))
        eflint_server ! NormActor.Phrase(s"+hour-of-the-day($hour)")
        context.spawn(responseHandler(m, eflint_server), s"response-handler-${m.internalRequest.id}")
        Behaviors.same
    }
  }

  def responseHandler(mReq: RequestAccess, eflint_server: ActorRef[NormActor.Message])
      (implicit resolver: ActorRefResolver): Behavior[norms.NormActor.QueryResponse] = Behaviors.setup { context =>
    val subject_id = "\"" + resolver.toSerializationFormat(mReq.internalRequest.request.subject) + "\""
    val action = "\"" + mReq.internalRequest.request.action + "\""
        
    eflint_server ! NormActor.Query(
      replyTo = context.self, 
      phrase = s"?login-rule(subject(${subject_id}), resource(${mReq.internalRequest.request.resource.name}), action(${action}))"
    )

    Behaviors.receiveMessage {
      case mResp: norms.NormActor.Response => {
        if (mResp.success)
          mReq.enforcer ! PolicyEnforcer.AccessGranted(mReq.internalRequest.id)
        else
          mReq.enforcer ! PolicyEnforcer.AccessDenied(mReq.internalRequest.id)
        Behaviors.stopped
      }
    }
  }
}
