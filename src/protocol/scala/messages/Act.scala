package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

case class Act(
    name: String,
    actor: Proposition,
    recipient: Proposition = null,
    relatedTo: List[Proposition] = List(),
    executionTime: LocalDateTime = LocalDateTime.now()
)
