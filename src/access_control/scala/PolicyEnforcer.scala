package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object EnforcerDB {
  var resources: Map[String, ActorRef[ResourceTypes.Message]] = Map()
  var pendingRequests: Map[Int, Request] = Map()
  var nextId: Int = 0
}

object PolicyEnforcer {
  trait Message {}
  case class RegisterResource(resource: Resource, senderRef: ActorRef[ResourceTypes.Message]) extends Message
  case class RequestAccess(request: Request) extends Message
  case class AccessDenied(requestId: Int) extends Message
  case class AccessGranted(requestId: Int) extends Message

  def apply(reasoner: ActorRef[PolicyReasoner.Message]): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m: RegisterResource =>
        context.log.info(s"(Enforcer) Registering resource ${m.resource.name}")
        EnforcerDB.resources += (m.resource.name -> m.senderRef)
        reasoner ! PolicyReasoner.RegisterResource(m.resource)
        Behaviors.same
      case m: RequestAccess =>
        val internalRequest = new InternalRequest(m.request, EnforcerDB.nextId)
        EnforcerDB.nextId += 1
        EnforcerDB.pendingRequests += (internalRequest.id -> m.request)
        reasoner ! PolicyReasoner.RequestAccess(internalRequest, context.self)
        Behaviors.same
      case m: AccessDenied =>
        val request = EnforcerDB.pendingRequests(m.requestId)
        request.subject ! Client.AccessDenied("Access denied by eFLINT")
        Behaviors.same
      case m: AccessGranted =>
        val request = EnforcerDB.pendingRequests(m.requestId)
        request.subject ! Client.AccessGranted("Access granted by eFLINT")
        Behaviors.same
    }
  }
}
