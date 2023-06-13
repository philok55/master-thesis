package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import protocol._

object Monitor extends MonitorActor {
  override def eventReceived(
      reasoner: ActorRef[Message],
      enforcer: ActorRef[Message],
      event: SystemEvent
  )(implicit resolver: ActorRefResolver): Unit = {
    event match {
      case event: OwnerCreated => {
        val p = new POwner(event.name)
        reasoner ! Inform(p)
      }
      case event: TenantCreated => {
        val p = new PTenant(event.name)
        enforcer ! Inform(p)
        reasoner ! Inform(p)
      }
      case event: RentalAgreementCreated => {
        val agr = new PRentalAgreement(event.id, event.tenantAddress)
        reasoner ! Inform(agr)
        val p = new PRentPrice(agr, event.price)
        reasoner ! Inform(p)
        if (event.social) {
          val social = new PSocialAgreement(agr)
          reasoner ! Inform(social)
        }
      }
      case event: RentalAgreementIndexed => {
        val a = new IndexAgreement(
          event.actor,
          new POwner(resolver.toSerializationFormat(event.actor)),
          event.current,
          event.percentage
        )
        reasoner ! InformAct(a)
      }
      case event: RentPaymentCreated => {
        val p = new PRentPayment(
          new PTenant(event.tenantAddress),
          event.price,
          event.deadline
        )
        reasoner ! Inform(p)
      }
      case event: RentPaymentDue => {
        val payment = new PRentPayment(
          new PTenant(event.tenantAddress),
          event.price,
          event.deadline
        )
        reasoner ! Inform(new PRentDue(payment))
      }
      case event: AgreementTerminated => {
        val tenantAddr = resolver.toSerializationFormat(event.tenant)
        val agr = new PRentalAgreement(event.agreementId, tenantAddr)
        val a = new TeminateAgreement(
          event.tenant,
          new PTenant(tenantAddr),
          event.owner,
          new POwner(resolver.toSerializationFormat(event.owner)),
          agr
        )
        reasoner ! InformAct(a)
      }
      case event: DepositRegistered => {
        val agr = new PRentalAgreement(event.agreementId, event.tenantAddress)
        val p = new PDeposit(agr, event.amount)
        reasoner ! Inform(p)
      }
      case event: DepositRefunded => {
        val tenantAddr = resolver.toSerializationFormat(event.tenant)
        val agr = new PRentalAgreement(event.agreementId, tenantAddr)
        val deposit = new PDeposit(agr, event.amount)
        val a = new RefundDeposit(
          event.owner,
          new POwner(resolver.toSerializationFormat(event.owner)),
          event.tenant,
          new PTenant(tenantAddr),
          deposit
        )
        reasoner ! InformAct(a)
      }
    }
  }
}
