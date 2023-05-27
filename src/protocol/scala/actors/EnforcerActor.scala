package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime

trait EnforcerActor {
  def protocolRequestsLoop(reasoner: ActorRef[Message]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case m: RequestAct =>
          if (acceptOrSendReject(message)) {
            context.spawn(
              actRequestHandler(m, reasoner),
              s"enf-actrequest-handler-${java.util.UUID.randomUUID.toString()}"
            )
          }
        case m: Inform => handleInform(m.predicate)
        case _ =>
          println("Protocol violated: invalid message received by Enforcer")
      }
      Behaviors.same
    }

  def actRequestHandler(
      message: RequestAct,
      reasoner: ActorRef[Message]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      reasoner ! RequestAct(message.act, context.self)

      Behaviors.receiveMessage {
        case m: Permit => {
          message.replyTo ! Permitted(message.act)
          Behaviors.stopped
        }
        case m: Forbid => {
          message.replyTo ! Forbidden(message.act)
          Behaviors.stopped
        }
        case _ => {
          println(
            "Protocol violated: invalid message received in response to RequestAct"
          )
          Behaviors.stopped
        }
      }
    }

  def acceptOrSendReject(message: Message): Boolean

  def handleInform(predicate: Predicate): Unit
}
