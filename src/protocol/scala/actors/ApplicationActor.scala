package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

trait ApplicationActor {
  def actRequestHandler(
      act: Act,
      enforcer: ActorRef[Message]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      enforcer ! RequestAct(act, context.self)

      Behaviors.receiveMessage {
        case m: Permitted => {
          actPermitted(m.act)
          Behaviors.stopped
        } 
        case m: Forbidden => {
          actForbidden(m.act)
          Behaviors.stopped
        }
        case m: Rejected => {
          actRejected(m.act)
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

  def actPermitted(act: Act): Unit

  def actForbidden(act: Act): Unit

  def actRejected(act: Act): Unit
}
