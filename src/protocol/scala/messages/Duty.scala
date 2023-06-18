package protocol

import akka.actor.typed.ActorRef
import java.time.LocalDateTime

final case class Duty(
    name: String,
    holder: Option[ActorRef[Message]],
    pHolder: Option[Proposition],
    claimant: Option[ActorRef[Message]],
    pClaimant: Option[Proposition],
    relatedTo: List[Proposition]
)
