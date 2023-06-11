package caseStudies

import akka.actor.typed.ActorRef
import protocol._

case class OwnerCreated(name: String) extends SystemEvent {}
case class TenantCreated(name: String) extends SystemEvent {}
case class RentalAgreementCreated(id: String, tenantAddress: String, price: Int) extends SystemEvent {}
case class RentalAgreementIndexed(actor: ActorRef[Message], current: PRentPrice, percentage: Int) extends SystemEvent {}
case class RentPaymentCreated(tenantAddress: String, price: Int, deadline: String) extends SystemEvent {}
case class RentPaymentDue(tenantAddress: String, price: Int, deadline: String) extends SystemEvent {}
case class AgreementTerminated(tenant: ActorRef[Message], owner: ActorRef[Message], agreementId: String) extends SystemEvent {}
case class DepositRegistered(agreementId: String, tenantAddress: String, amount: Int) extends SystemEvent {}
case class DepositRefunded(owner: ActorRef[Message], tenant: ActorRef[Message], agreementId: String, amount: Int) extends SystemEvent {}