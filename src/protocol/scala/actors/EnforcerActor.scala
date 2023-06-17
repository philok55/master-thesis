package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Set
import java.time.LocalDateTime
import scala.collection.Map

trait EnforcerActor {
  def blockedActions = Set[String]()

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
        case m: Inform => {
          handleInform(m.proposition)
          Behaviors.same
        }
        case m: InformDuty => {
          handleInformDuty(m.duty)
          Behaviors.same
        }
        case m: InformDutyTerminated => {
          handleTerminatedDuty(m.duty)
          Behaviors.same
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
        case m: RequestDuty => {
          if (acceptOrSendReject(message)) {
            context.spawn(
              dutyRequestHandler(m, reasoner, contacts),
              s"enf-dutyrequest-handler-${java.util.UUID.randomUUID.toString()}"
            )
          }
          Behaviors.same
        }
        case m: InformViolatedAct => {
          violatedAct(m.act, contacts)
          Behaviors.same
        }
        case m: InformViolatedDuty => {
          violatedDuty(m.duty, contacts)
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
        case m: Permitted => {
          if (blockedActions contains message.act.name)
            message.replyTo ! Permit(message.act)
          else
            message.replyTo ! Permitted(message.act)
          actPermitted(message.act, contacts)
          Behaviors.stopped
        }
        case m: Forbidden => {
          if (blockedActions contains message.act.name)
            message.replyTo ! Forbid(message.act)
          else
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

  def dutyRequestHandler(
      message: RequestDuty,
      reasoner: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Behavior[Message] =
    Behaviors.setup { context =>
      context.log.error(
        "Duty requests are not yet supported"
      )
      Behaviors.stopped
    }

  def handleInform(proposition: Proposition): Unit = {}

  def handleInformDuty(duty: Duty): Unit = {}

  def handleTerminatedDuty(duty: Duty): Unit = {}

  def acceptOrSendReject(message: Message): Boolean = true

  def actPermitted(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}

  def actForbidden(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}

  def violatedAct(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}

  def violatedDuty(
      duty: Duty,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {}
}
