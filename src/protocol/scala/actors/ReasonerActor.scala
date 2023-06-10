package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Set
import norms.NormActor

trait ReasonerActor {
  protected def blockedActions = Set[String]()

  final case class RegisterEnforcer(enforcer: ActorRef[Message]) extends Message

  def apply(
      eflintFile: String
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.setup { context =>
      val eflintActor = new NormActor(
        eflintFile,
        debug_mode = true
      )
      val eflintServer = context.spawn(eflintActor.listen(), "eFLINT-actor")
      listen(eflintServer)
    }

  def listen(
      eflint_server: ActorRef[NormActor.Message],
      enforcer: Option[ActorRef[Message]] = None
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case RegisterEnforcer(enf) => listen(eflint_server, Some(enf))
        case m: QueryMessage =>
          context.spawn(
            queryResponseHandler(m, eflint_server, enforcer),
            s"qresponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          Behaviors.same
        case m: Message =>
          context.spawn(
            phraseResponseHandler(m, eflint_server, enforcer),
            s"presponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          Behaviors.same
        case _ =>
          println("Error: reasoner received unknown message")
          Behaviors.same
      }
    }

  def queryResponseHandler(
      msg: QueryMessage,
      eflint_server: ActorRef[NormActor.Message],
      enforcer: Option[ActorRef[Message]] = None
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[NormActor.QueryResponse] = Behaviors.setup { context =>
    val phrase = EflintAdapter(msg)
    eflint_server ! NormActor.Query(
      replyTo = context.self,
      phrase = phrase
    )

    enforcer match {
      case None =>
        context.log.error(
          "Reasoner received query before enforcer was registered."
        )
        Behaviors.stopped
      case Some(myValue) =>
        Behaviors.receiveMessage {
          case response: norms.NormActor.Response => {
            println(
              s"Reasoner received response. Query: $phrase; Response: $response"
            )
            msg match {
              case m: RequestAct => {
                if (response.success) m.replyTo ! Permit(m.act)
                else m.replyTo ! Forbid(m.act)
              }
              case _ => println("NotImplementedError")
            }
            Behaviors.stopped
          }
          case response: norms.Message => {
            println(
              s"Reasoner received unknown response. Query: $phrase; Response: $response"
            )
            Behaviors.same
          }
        }
    }
  }

  def phraseResponseHandler(
      msg: Message,
      eflint_server: ActorRef[NormActor.Message],
      enforcer: Option[ActorRef[Message]] = None
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[norms.Message] = Behaviors.setup { context =>
    val phrase = EflintAdapter(msg)
    println(s"Reasoner evaluating phrase: $phrase")
    eflint_server ! NormActor.Phrase(
      phrase = phrase,
      handler = context.self
    )

    enforcer match {
      case None =>
        context.log.error(
          "Reasoner received query before enforcer was registered."
        )
        Behaviors.stopped
      case Some(enf) =>
        Behaviors.receiveMessage {
          case norms.ViolatedAction(action) => {
            println(s"Reasoner received violated action: $action")
            // TODO: better to actually parse the action instead of
            // assuming it is the act we evaluated
            msg match {
              case m: InformAct => enf ! InformViolatedAct(m.act)
              case _ =>
                context.log.error(
                  s"Unexpected response to ${phrase}: ViolatedAction"
                )
            }
            Behaviors.same
          }
          case norms.ViolatedDuty(duty) => {
            println(s"Reasoner received violated duty: $duty")
            // TODO parse duty
            // enf ! InformViolatedDuty(duty)
            Behaviors.same
          }
          case norms.ActiveDuty(duty) => {
            println(s"Reasoner received active duty: $duty")
            // TODO parse duty
            // enf ! InformDuty(duty)
            Behaviors.same
          }
          case norms.ExecutedAction(action) => {
            println(s"Reasoner received executed action: $action")
            Behaviors.same
          }
          case response: norms.Message => {
            println(
              s"Reasoner received unknown message from eFLINT. Phrase: $phrase; Response: $response"
            )
            Behaviors.same
          }
        }
    }
  }
}
