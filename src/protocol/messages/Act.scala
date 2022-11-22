package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

// Problem to solve: RelatedTo

final case class Act(
    name: String,
    actor: ActorRef[Message],
    recipient: ActorRef[Message],
    executionTime: LocalDateTime
)
