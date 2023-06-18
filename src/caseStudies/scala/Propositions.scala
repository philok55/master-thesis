package caseStudies

import protocol._

final class PUser(val name: String, override val state: TruthValue = True)
    extends Proposition("user", List(PString(name)), state) {}

final class POwner(val name: String, override val state: TruthValue = True)
    extends Proposition("owner", List(new PUser(name)), state) {}

final class PTenant(val name: String, override val state: TruthValue = True)
    extends Proposition("tenant", List(new PUser(name)), state) {}

final class PDocument(val id: String, override val state: TruthValue = True)
    extends Proposition("document", List(PString(id)), state) {}

final class PRentalAgreement(
    val id: String,
    val tenantAddress: String,
    override val state: TruthValue = True
) extends Proposition(
      "rental-agreement",
      List(new PTenant(tenantAddress), PString(id)),
      state
    ) {}

final class PSocialAgreement(
    val agreement: PRentalAgreement,
    override val state: TruthValue = True
) extends Proposition(
      "is-social-housing",
      List(agreement),
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

final class PRentDue(
    val payment: PRentPayment,
    override val state: TruthValue = True
) extends Proposition("rent-due", List(payment), state) {}

final class PDeposit(
    val agreement: PRentalAgreement,
    val price: Int,
    override val state: TruthValue = True
) extends Proposition("deposit", List(agreement, PInt(price)), state) {}

final class PIncome(
    val tenant: PTenant,
    val income: Int,
    override val state: TruthValue = True
) extends Proposition("yearly-income", List(tenant, PInt(income)), state) {}
