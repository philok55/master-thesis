package caseStudies

import protocol._

class PTenant(val name: String, override val state: TruthValue = True)
    extends Predicate("tenant", List(PString(name)), state) {}

class PRentalAgreement(
    val id: String,
    val tenantName: String,
    override val state: TruthValue = True
) extends Predicate(
      "rental-agreement",
      List(new PTenant(tenantName), PString(id)),
      state
    ) {}
