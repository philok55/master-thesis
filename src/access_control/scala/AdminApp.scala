package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object AdminApp {
  case class CreateClientApp(name: String) extends norms.Message
  case class CreateResource(resource: String) extends norms.Message

  def apply(reasoner: ActorRef[PolicyReasoner.Message], 
            enforcer: ActorRef[PolicyEnforcer.Message]): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: CreateClientApp =>
        val clientApp = context.spawn(ClientApp(enforcer), m.name)
        reasoner ! PolicyReasoner.RegisterSubject(clientApp)
        clientApp ! ClientApp.TryAccessResource("protected-resource", "login")
        Behaviors.same
      case m: CreateResource =>
        reasoner ! PolicyReasoner.RegisterResource(m.resource)
        Behaviors.same
    }
  }
}
