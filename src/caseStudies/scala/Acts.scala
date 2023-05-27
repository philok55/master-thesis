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
