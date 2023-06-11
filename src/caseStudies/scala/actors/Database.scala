package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object Database extends ApplicationActor {
  object KnowledgeBase {
    var tenants: Map[String, (ActorRef[Message], ActorRef[Message])] =
      Map() // tenantAddress -> (owner, tenant)
    var agreements: Map[String, (Document, String, Int)] =
      Map() // agreementId -> (document, tenantAddress, price)
  }

  final case class AddTenant(
      tenant: ActorRef[Message],
      owner: ActorRef[Message]
  ) extends ApplicationMessage

  final case class AddAgreement(
      document: Document,
      tenantAddress: String,
      price: Int
  ) extends ApplicationMessage

  final case class GetAgreement(
      documentId: String,
      requester: ActorRef[Message]
  ) extends ApplicationMessage

  final case class AgreementFetched(document: Document)
      extends ApplicationMessage

  final case class TerminateAgreement(agreementId: String)
      extends ApplicationMessage

  final case class RegisterDeposit(agreementId: String, amount: Int)
      extends ApplicationMessage

  final case class RegisterDepositRefund(agreementId: String, amount: Int)
      extends ApplicationMessage

  final case class IndexAgreement(
      documentId: String,
      percentage: Int
  ) extends ApplicationMessage

  override def handleApplicationMessage(
      message: ApplicationMessage,
      enforcer: ActorRef[Message],
      self: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    message match {
      case m: AddTenant => {
        val address = resolver.toSerializationFormat(m.tenant)
        KnowledgeBase.tenants =
          KnowledgeBase.tenants + (address -> (m.owner, m.tenant))
        TenantCreated(address)()
        Behaviors.same
      }
      case m: AddAgreement => {
        KnowledgeBase.agreements =
          KnowledgeBase.agreements + (m.document.id -> (m.document, m.tenantAddress, m.price))
        RentalAgreementCreated(m.document.id, m.tenantAddress, m.price)()
        Behaviors.same
      }
      case m: GetAgreement => {
        val agreement = KnowledgeBase.agreements(m.documentId)
        m.requester ! AgreementFetched(agreement._1)
        Behaviors.same
      }
      case m: IndexAgreement => {
        val agreement = KnowledgeBase.agreements(m.documentId)
        val newPrice =
          (agreement._3.toFloat * (1.0 + m.percentage.toFloat / 100.0)).toInt
        KnowledgeBase.agreements = KnowledgeBase.agreements - m.documentId
        KnowledgeBase.agreements =
          KnowledgeBase.agreements + (m.documentId -> (agreement._1, agreement._2, newPrice))

        val tenantAddress = agreement._2
        val owner = KnowledgeBase.tenants(tenantAddress)._1
        val pAgreement = new PRentalAgreement(m.documentId, tenantAddress)
        val pCurrRentPrice = new PRentPrice(pAgreement, agreement._3)
        RentalAgreementIndexed(owner, pCurrRentPrice, m.percentage)()
        Behaviors.same
      }
      case m: TerminateAgreement => {
        val agreement = KnowledgeBase.agreements(m.agreementId)
        val tenantAddr = agreement._2
        val tenantRecord = KnowledgeBase.tenants(tenantAddr)
        val owner = tenantRecord._1
        val tenant = tenantRecord._2
        AgreementTerminated(tenant, owner, m.agreementId)()
        Behaviors.same
      }
      case m: RegisterDeposit => {
        val agreement = KnowledgeBase.agreements(m.agreementId)
        val tenantAddr = agreement._2
        DepositRegistered(m.agreementId, tenantAddr, m.amount)()
        Behaviors.same
      }
      case m: RegisterDepositRefund => {
        val agreement = KnowledgeBase.agreements(m.agreementId)
        val tenantAddr = agreement._2
        val tenantRecord = KnowledgeBase.tenants(tenantAddr)
        val owner = tenantRecord._1
        val tenant = tenantRecord._2
        DepositRefunded(owner, tenant, m.agreementId, m.amount)()
        Behaviors.same
      }
    }
}
