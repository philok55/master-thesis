package access_control

import akka.actor.typed.ActorRef

class Request(val subject: ActorRef[Client.Message], val resource: Resource, var action: String)
