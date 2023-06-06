package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

case class Act(
    name: String,
    actor: ActorRef[Message],
    pActor: Proposition,
    recipient: ActorRef[Message] = null,
    pRecipient: Proposition = null,
    relatedTo: List[Proposition] = List(),
    executionTime: LocalDateTime = LocalDateTime.now()
)
