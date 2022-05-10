package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object ResourceTypes {
  trait Message {}
  case class ReceiveRequest(request: Request) extends Message
}

class Resource(val enforcedBy: ActorRef[PolicyEnforcer.Message], val name: String) {
  import ResourceTypes._

  def start(): Behavior[Message] = Behaviors.setup { context =>
    enforcedBy ! PolicyEnforcer.RegisterResource(this, context.self)
    listen()
  }

  def listen(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: ReceiveRequest =>
        context.log.info(s"Received request from ${m.request.subject}: ${m.request.action}")
        Behaviors.same
    }
  }
}
