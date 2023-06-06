package caseStudies

import protocol._

case class OwnerCreated(name: String) extends SystemEvent {}
case class TenantCreated(name: String) extends SystemEvent {}
case class RentalAgreementCreated(id: String, tenantName: String, price: Int) extends SystemEvent {}
case class RentalAgreementIndexed(owner: String, current: PRentPrice, percentage: Int) extends SystemEvent {}
