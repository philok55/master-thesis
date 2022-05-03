package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object PolicyEnforcer {
  trait Message {}
  case class RequestAccess(request: Request) extends Message

  def apply(reasoner: ActorRef[PolicyReasoner.Message]): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case RequestAccess(request) =>
        reasoner ! PolicyReasoner.RequestAccess(request)
        Behaviors.same
    }
  }
}
