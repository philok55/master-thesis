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
        enforcer ! Inform(p)
        reasoner ! Inform(p)
      }
      case event: TenantCreated => {
        val p = new PTenant(event.name)
        enforcer ! Inform(p)
        reasoner ! Inform(p)
      }
      case event: RentalAgreementCreated => {
        val agr = new PRentalAgreement(event.id, event.tenantAddress)
        val p = new PRentPrice(agr, event.price)
        enforcer ! Inform(p)
        reasoner ! Inform(p)
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
    }
  }
}
