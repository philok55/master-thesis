package caseStudies

import akka.actor.typed.ActorRef
import protocol._

final class AccessDocument(
    override val actor: PTenant,
    val documentId: String
) extends Act(
      "access-document",
      actor,
      relatedTo = List(new PDocument(documentId))
    ) {}

final class IndexAgreement(
    override val actor: POwner,
    val current: PRentPrice,
    val percentage: Int
) extends Act(
      "index-agreement",
      actor,
      relatedTo = List(
        current,
        Proposition(
          "percentage",
          List(PInt(percentage)),
          True
        )
      )
    ) {}
