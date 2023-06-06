package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object Owner extends ApplicationActor {
  final case class CreateTenant(tenant: ActorRef[Message])
      extends ApplicationMessage

  final case class CreateAgreement(
      id: String,
      fileName: String,
      content: String,
      tenantName: String,
      price: Int
  ) extends ApplicationMessage

  final case class IndexAgreement(
      id: String,
      percentage: Int
  ) extends ApplicationMessage

  final case class SetPaymentDeadline(
      tenantName: String,
      price: Int,
      deadline: String
  ) extends ApplicationMessage

  final case class MarkRentAsDue(
      tenantName: String,
      price: Int,
      deadline: String
  ) extends ApplicationMessage

  final case class DisplayViolation(message: String) extends ApplicationMessage

  override def handleApplicationMessage(
      message: ApplicationMessage,
      enforcer: ActorRef[Message],
      self: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Behavior[Message] =
    message match {
      case m: CreateTenant => {
        contacts("db") ! Database.AddTenant(m.tenant, self)
        Behaviors.same
      }
      case m: CreateAgreement => {
        val agreement = new Document(m.id, m.fileName, m.content)
        contacts("db") ! Database.AddAgreement(agreement, m.tenantName, m.price)
        Behaviors.same
      }
      case m: IndexAgreement => {
        contacts("db") ! Database.IndexAgreement(m.id, m.percentage)
        Behaviors.same
      }
      case m: SetPaymentDeadline => {
        RentPaymentCreated(m.tenantName, m.price, m.deadline)()
        Behaviors.same
      }
      case m: MarkRentAsDue => {
        RentPaymentDue(m.tenantName, m.price, m.deadline)()
        Behaviors.same
      }
      case m: DisplayViolation => {
        println(s"Owner portal displaying violation to user: ${m.message}")
        Behaviors.same
      }
    }
}
