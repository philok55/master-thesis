package caseStudies

import akka.actor.typed.ActorRef
import protocol._

case class OwnerCreated(name: String) extends SystemEvent {}
case class TenantCreated(name: String) extends SystemEvent {}
case class RentalAgreementCreated(id: String, tenantName: String, price: Int) extends SystemEvent {}
case class RentalAgreementIndexed(actor: ActorRef[Message], current: PRentPrice, percentage: Int) extends SystemEvent {}
