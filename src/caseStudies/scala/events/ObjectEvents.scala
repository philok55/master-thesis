package caseStudies

import akka.actor.typed.ActorRef
import protocol._

case class OwnerCreated(name: String) extends SystemEvent {}
case class TenantCreated(name: String) extends SystemEvent {}
case class RentalAgreementCreated(id: String, tenantAddress: String, price: Int) extends SystemEvent {}
case class RentalAgreementIndexed(actor: ActorRef[Message], current: PRentPrice, percentage: Int) extends SystemEvent {}
case class RentPaymentCreated(tenantAddress: String, price: Int, deadline: String) extends SystemEvent {}
case class RentPaymentDue(tenantAddress: String, price: Int, deadline: String) extends SystemEvent {}
case class RentPaymentMade(actor: ActorRef[Message], tenantAddress: String, price: Int, deadline: String) extends SystemEvent {}
