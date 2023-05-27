package caseStudies

import protocol._

case class TenantCreated(name: String) extends SystemEvent {}
case class RentalAgreementCreated(id: String, tenantName: String) extends SystemEvent {}
