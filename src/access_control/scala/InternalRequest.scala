package access_control

import akka.actor.typed.ActorRef

class InternalRequest(val request: Request, val id: Int)
