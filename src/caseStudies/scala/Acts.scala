package caseStudies

import akka.actor.typed.ActorRef
import protocol._

final class AccessDocument(
    override val actor: ActorRef[Message],
    override val pActor: PTenant,
    val documentId: String
) extends Act(
      "access-document",
      actor,
      pActor,
      relatedTo = List(new PDocument(documentId))
    ) {}

final class IndexAgreement(
    override val actor: ActorRef[Message],
    override val pActor: POwner,
    val current: PRentPrice,
    val percentage: Int
) extends Act(
      "index-agreement",
      actor,
      pActor,
      relatedTo = List(
        current,
        Proposition(
          "percentage",
          List(PInt(percentage)),
          True
        )
      )
    ) {}

final class TeminateAgreement(
    override val actor: ActorRef[Message],
    override val pActor: PTenant,
    override val recipient: ActorRef[Message],
    override val pRecipient: POwner,
    val agreement: PRentalAgreement
) extends Act(
      "terminate-agreement",
      actor,
      pActor,
      recipient,
      pRecipient,
      relatedTo = List(agreement)
    )

final class RefundDeposit(
    override val actor: ActorRef[Message],
    override val pActor: POwner,
    override val recipient: ActorRef[Message],
    override val pRecipient: PTenant,
    val deposit: PDeposit
) extends Act(
      "refund",
      actor,
      pActor,
      recipient,
      pRecipient,
      relatedTo = List(deposit)
    )
