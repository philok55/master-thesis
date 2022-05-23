package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object ResourceTypes {
  trait Message {}
  case class ClientRequest(request: Request, replyTo: ActorRef[PolicyEnforcer.ResourceResponse]) extends Message
}

class Resource(val enforcedBy: ActorRef[PolicyEnforcer.Message], val name: String, var ref: Option[ActorRef[ResourceTypes.Message]] = None) {
  import ResourceTypes._

  def start(): Behavior[Message] = Behaviors.setup { context =>
    enforcedBy ! PolicyEnforcer.RegisterResource(this, context.self)
    listen()
  }

  def listen(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: ClientRequest =>
        context.log.info(s"Received request from ${m.request.subject}: ${m.request.action}")
        m.replyTo ! PolicyEnforcer.ResourceResponse(s"Log in for client ${m.request.subject} succesful")
        Behaviors.same
    }
  }
}
