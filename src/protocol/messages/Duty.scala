package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

final case class Duty(
    name: String,
    holder: ActorRef[Message],
    claimant: ActorRef[Message]
)
