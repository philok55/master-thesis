package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

final case class Act(
    name: String,
    actor: ActorRef[Message],
    recipient: ActorRef[Message],
    relatedTo: List[Predicate],
    executionTime: LocalDateTime
)
