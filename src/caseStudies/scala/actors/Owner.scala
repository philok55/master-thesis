package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object Owner extends ApplicationActor {
  final case class CreateTenant(tenant: ActorRef[Message]) extends ApplicationMessage

  final case class CreateAgreement(
      id: String,
      fileName: String,
      content: String,
      tenantName: String
  ) extends ApplicationMessage

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
        contacts("db") ! Database.AddAgreement(agreement, m.tenantName)
        Behaviors.same
      }
    }
}
