package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime
import scala.collection.Map

trait EnforcerActor {
  final case class AddContact(name: String, contact: ActorRef[Message])
      extends Message

  def protocolRequestsLoop(
      reasoner: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case m: AddContact => {
          protocolRequestsLoop(reasoner, contacts + (m.name -> m.contact))
        }
        case m: RequestAct => {
          if (acceptOrSendReject(message)) {
            context.spawn(
              actRequestHandler(m, reasoner, contacts),
              s"enf-actrequest-handler-${java.util.UUID.randomUUID.toString()}"
            )
          }
          Behaviors.same
        }
        case m: Inform => {
          handleInform(m.predicate)
          Behaviors.same
        }
        case _ => {
          println("Protocol violated: invalid message received by Enforcer")
          Behaviors.same
        }
      }
    }

  def actRequestHandler(
      message: RequestAct,
      reasoner: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Behavior[Message] =
    Behaviors.setup { context =>
      reasoner ! RequestAct(message.act, context.self)

      Behaviors.receiveMessage {
        case m: Permit => {
          message.replyTo ! Permitted(message.act)
          actPermitted(message.act, contacts)
          Behaviors.stopped
        }
        case m: Forbid => {
          message.replyTo ! Forbidden(message.act)
          actForbidden(message.act, contacts)
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

  def acceptOrSendReject(message: Message): Boolean = true

  def actPermitted(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}

  def actForbidden(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}

  def handleInform(predicate: Predicate): Unit = {}
}
