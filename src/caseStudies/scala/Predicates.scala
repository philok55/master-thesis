package caseStudies

import protocol._

final class PTenant(val name: String, override val state: TruthValue = True)
    extends Predicate("tenant", List(PString(name)), state) {}

final class PDocument(val id: String, override val state: TruthValue = True)
    extends Predicate("document", List(PString(id)), state) {}

final class PRentalAgreement(
    val id: String,
    val tenantName: String,
    override val state: TruthValue = True
) extends Predicate(
      "rental-agreement",
      List(new PTenant(tenantName), PString(id)),
      state
    ) {}
