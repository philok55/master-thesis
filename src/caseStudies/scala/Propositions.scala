package caseStudies

import protocol._

final class POwner(val name: String, override val state: TruthValue = True)
    extends Proposition("owner", List(PString(name)), state) {}

final class PTenant(val name: String, override val state: TruthValue = True)
    extends Proposition("tenant", List(PString(name)), state) {}

final class PDocument(val id: String, override val state: TruthValue = True)
    extends Proposition("document", List(PString(id)), state) {}

final class PRentalAgreement(
    val id: String,
    val tenantName: String,
    override val state: TruthValue = True
) extends Proposition(
      "rental-agreement",
      List(new PTenant(tenantName), PString(id)),
      state
    ) {}

final class PRentPrice(
    val agreement: PRentalAgreement,
    val price: Int,
    override val state: TruthValue = True
) extends Proposition(
      "rent-price",
      List(agreement, PInt(price)),
      state
    ) {}

final class PRentPayment(
    val tenant: PTenant,
    val price: Int,
    val deadline: String,
    override val state: TruthValue = True
) extends Proposition(
      "rent-payment",
      List(tenant, PInt(price), PString(deadline)),
      state
    ) {}

final class PRentDue(val payment: PRentPayment)
    extends Proposition("rent-due", List(payment), True) {}
