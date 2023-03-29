package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

final case class Act(
    name: String,
    actor: Predicate,
    recipient: Predicate = null,
    relatedTo: List[Predicate] = List[Predicate](),
    executionTime: LocalDateTime = LocalDateTime.now()
)
