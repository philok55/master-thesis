package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object Reasoner {
  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: Message => {
        println(s"Reasoner received message: $m")
        println(s"Translation to eFLINT: ${EFLINTAdapter(m)}")
      }
    }
    Behaviors.stopped
  }
}
