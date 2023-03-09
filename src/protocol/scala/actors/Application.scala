package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

trait Application {
  def actResponseListener(enforcer: ActorRef[Message]): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case m: Permitted => actPermitted(m.act)
        case m: Forbidden => actForbidden(m.act)
        case m: Rejected  => actRejected(m.act)
        case _            => println("Send reject")
      }
      Behaviors.same
    }

  def sendActRequest(act: Act): Unit = {
    enforcer ! RequestAct(act)
  }

  def actPermitted(act: Act): Unit

  def actForbidden(act: Act): Unit

  def actRejected(act: Act): Unit
}

object TestApplication extends Application {
  def apply(
      enforcer: ActorRef[Message]
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    actResponseListener(enforcer)

  override def actPermitted(act: Act): Unit = println("Act permitted")

  override def actForbidden(act: Act): Unit = println("Act forbidden")

  override def actRejected(act: Act): Unit = println("Act rejected")
}
