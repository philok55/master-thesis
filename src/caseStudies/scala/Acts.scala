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

final class MakeRentPayment(
    override val actor: ActorRef[Message],
    override val pActor: PTenant,
    val payment: PRentPayment
) extends Act(
      "make-rent-payment",
      actor,
      pActor,
      relatedTo = List(payment)
    ) {}
