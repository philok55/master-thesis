package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

trait ApplicationMessage extends Message {}

trait ApplicationActor {
  def apply(
      enforcer: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      {
        message match {
          case m: ApplicationMessage =>
            handleApplicationMessage(m, enforcer, context.self, contacts)
          case m: Permitted            => actPermitted(m.act)
          case m: Permit               => actPermitted(m.act, true)
          case m: Forbidden            => actForbidden(m.act)
          case m: Forbid               => actForbidden(m.act, true)
          case m: InformDuty           => dutyReceived(m.duty)
          case m: InformDutyTerminated => dutyTerminated(m.duty)
          case m: Reject               => requestRejected(m.obj)
          case _ =>
            println(
              "Protocol violated: invalid message received in response to RequestAct"
            )
        }
        Behaviors.same
      }
    }

  def sendQuery(
      content: Either[Act, Duty],
      enforcer: ActorRef[Message],
      replyTo: ActorRef[Message]
  ): Unit =
    content match {
      case Left(act)   => enforcer ! RequestAct(act, replyTo)
      case Right(duty) => enforcer ! RequestDuty(duty, replyTo)
    }

  def handleApplicationMessage(
      message: ApplicationMessage,
      enforcer: ActorRef[Message],
      self: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  )(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.same

  def actPermitted(act: Act, enforced: Boolean = false): Unit = {}

  def actForbidden(act: Act, enforced: Boolean = false): Unit = {}

  def requestRejected(obj: Either[Act, Duty]): Unit = {}

  def dutyReceived(duty: Duty): Unit = {}

  def dutyTerminated(duty: Duty): Unit = {}
}
