package access_control

import akka.actor.typed.ActorRef

class Request (val subject: ActorRef[ClientApp.Message], val resource: String, val action: String)
