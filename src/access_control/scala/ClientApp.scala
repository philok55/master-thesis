package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object ClientApp {
  trait Message {}
  case class TryAccessResource(resource: String, action: String) extends Message
  case class AccessGranted(message: String) extends Message
  case class AccessDenied(message: String) extends Message

  def apply(enforcer: ActorRef[PolicyEnforcer.Message]): Behavior[Message] = Behaviors.setup { context =>
    // enforcer ! PolicyReasoner.RegisterSubject(context.self)
    listen(enforcer)
  }

  def listen(enforcer: ActorRef[PolicyEnforcer.Message]): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: TryAccessResource =>
        enforcer ! PolicyEnforcer.RequestAccess(new Request(context.self, m.resource, m.action))
        Behaviors.same
      case m: AccessGranted =>
        println(s"Access granted: $message")
        Behaviors.same
      case m: AccessDenied =>
        println(s"Access denied: $message")
        Behaviors.same
    }
  }
}
