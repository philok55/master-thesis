package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

case class Act(
    name: String,
    actor: Predicate,
    recipient: Predicate = null,
    relatedTo: List[Predicate] = List(),
    executionTime: LocalDateTime = LocalDateTime.now()
)
