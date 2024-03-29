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
      tenantAddress: String,
      price: Int,
      social: Boolean = false
  ) extends ApplicationMessage

  final case class RegisterAgreementTermination(
      agreementId: String
  ) extends ApplicationMessage

  final case class CollectDeposit(
      agreementId: String,
      amount: Int
  ) extends ApplicationMessage

  final case class RefundDeposit(
      agreementId: String,
      amount: Int
  ) extends ApplicationMessage

  final case class IndexAgreement(
      id: String,
      percentage: Int
  ) extends ApplicationMessage

  final case class SetPaymentDeadline(
      tenantAddress: String,
      price: Int,
      deadline: String
  ) extends ApplicationMessage

  final case class MarkRentAsDue(
      tenantAddress: String,
      price: Int,
      deadline: String
  ) extends ApplicationMessage

  final case class DisplayViolation(message: String) extends ApplicationMessage

  final case class QueryDuties() extends ApplicationMessage

  override def handleApplicationMessage(
      message: ApplicationMessage,
      enforcer: ActorRef[Message],
      self: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    message match {
      case m: CreateTenant => {
        contacts("db") ! Database.AddTenant(m.tenant, self)
        Behaviors.same
      }
      case m: CreateAgreement => {
        val agreement = new Document(m.id, m.fileName, m.content)
        contacts("db") ! Database.AddAgreement(
          agreement,
          m.tenantAddress,
          m.price,
          m.social
        )
        Behaviors.same
      }
      case m: RegisterAgreementTermination => {
        contacts("db") ! Database.TerminateAgreement(m.agreementId)
        Behaviors.same
      }
      case m: CollectDeposit => {
        contacts("db") ! Database.RegisterDeposit(m.agreementId, m.amount)
        Behaviors.same
      }
      case m: RefundDeposit => {
        contacts("db") ! Database.RegisterDepositRefund(m.agreementId, m.amount)
        Behaviors.same
      }
      case m: IndexAgreement => {
        contacts("db") ! Database.IndexAgreement(m.id, m.percentage)
        Behaviors.same
      }
      case m: SetPaymentDeadline => {
        RentPaymentCreated(m.tenantAddress, m.price, m.deadline)()
        Behaviors.same
      }
      case m: MarkRentAsDue => {
        RentPaymentDue(m.tenantAddress, m.price, m.deadline)()
        Behaviors.same
      }
      case m: DisplayViolation => {
        println(s"Owner portal displaying violation to user: ${m.message}")
        Behaviors.same
      }
      case m: QueryDuties => {
        println(s"Owner querying for active duties")
        val dObj = Duty(
          "",
          Some(self),
          Some(new POwner(resolver.toSerializationFormat(self))),
          None,
          None,
          List()
        )
        sendQuery(Right(dObj), enforcer, self)
        Behaviors.same
      }
    }

  override def dutyReceived(duty: Duty): Unit = println(
    s"Active duty ${duty.name} received by owner"
  )

  override def dutyTerminated(duty: Duty): Unit = println(
    s"Teminated duty ${duty.name} received by owner"
  )

  override def requestRejected(obj: Either[Act, Duty]): Unit = obj match {
    case Right(duty) => {
      println(s"Owner: duty request rejected")
    }
  }
}
