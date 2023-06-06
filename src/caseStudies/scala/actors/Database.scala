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
      Map() // (owner, tenant)
    var agreements: Map[String, (Document, String, Int)] =
      Map() // (document, tenantName, price)
  }

  final case class AddTenant(
      tenant: ActorRef[Message],
      owner: ActorRef[Message]
  ) extends ApplicationMessage

  final case class AddAgreement(
      document: Document,
      tenantName: String,
      price: Int
  ) extends ApplicationMessage

  final case class GetAgreement(
      documentId: String,
      requester: PTenant
  ) extends ApplicationMessage

  final case class AgreementFetched(document: Document)
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
  ): Behavior[Message] =
    message match {
      case m: AddTenant => {
        KnowledgeBase.tenants =
          KnowledgeBase.tenants + (m.tenant.path.name -> (m.owner, m.tenant))
        TenantCreated(m.tenant.path.name)()
        Behaviors.same
      }
      case m: AddAgreement => {
        KnowledgeBase.agreements =
          KnowledgeBase.agreements + (m.document.id -> (m.document, m.tenantName, m.price))
        RentalAgreementCreated(m.document.id, m.tenantName, m.price)()
        Behaviors.same
      }
      case m: GetAgreement => {
        val agreement = KnowledgeBase.agreements(m.documentId)
        val recipient = KnowledgeBase.tenants(m.requester.name)._2
        recipient ! AgreementFetched(agreement._1)
        Behaviors.same
      }
      case m: IndexAgreement => {
        val agreement = KnowledgeBase.agreements(m.documentId)
        val newPrice = (agreement._3.toFloat * (1.0 + m.percentage.toFloat / 100.0)).toInt
        KnowledgeBase.agreements = KnowledgeBase.agreements - m.documentId
        KnowledgeBase.agreements =
          KnowledgeBase.agreements + (m.documentId -> (agreement._1, agreement._2, newPrice))

        val tenantName = agreement._2
        val owner = KnowledgeBase.tenants(tenantName)._1
        val pAgreement = new PRentalAgreement(m.documentId, tenantName)
        val pCurrRentPrice = new PRentPrice(pAgreement, agreement._3)
        RentalAgreementIndexed(owner.path.name, pCurrRentPrice, m.percentage)()
        Behaviors.same
      }
    }
}
