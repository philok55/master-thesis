package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object Client {
  trait Message {}
  case class TryAccessResource(resource: Resource, action: String) extends Message
  case class AccessGranted(message: String) extends Message
  case class AccessDenied(message: String) extends Message

  def apply(): Behavior[Message] = Behaviors.setup { context =>
    listen()
  }

  def listen(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: TryAccessResource =>
        m.resource.enforcedBy ! PolicyEnforcer.RequestAccess(new Request(context.self, m.resource, m.action))
        Behaviors.same
      case m: AccessGranted =>
        println(s"Access granted: ${m.message}")
        Behaviors.same
      case m: AccessDenied =>
        println(s"Access denied: ${m.message}")
        Behaviors.same
    }
  }
}
