package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object Tenant extends ApplicationActor {
  final case class FetchAgreement(documentId: String) extends ApplicationMessage

  override def handleApplicationMessage(
      message: ApplicationMessage,
      enforcer: ActorRef[Message],
      self: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Behavior[Message] =
    message match {
      case m: FetchAgreement => {
        val act = new AccessDocument(new PTenant(self.path.name), m.documentId)
        sendQuery(Left(act), enforcer, self)
        Behaviors.same
      }
      case m: Database.AgreementFetched => {
        println(s"Agreement received by ${self.path.name}: ${m.document.fileName}. Content: ${m.document.content}")
        Behaviors.same
      }
    }

  override def actPermitted(act: Act): Unit = act match {
    case a: AccessDocument =>
      println(s"Tenant: access request to ${a.documentId} permitted")
  }

  override def actForbidden(act: Act): Unit = act match {
    case a: AccessDocument =>
      println(s"Tenant: access request to ${a.documentId} forbidden")
  }

  override def actRejected(act: Act): Unit = act match {
    case a: AccessDocument =>
      println(s"Tenant: access request to ${a.documentId} rejected")
  }

  override def dutyReceived(duty: Duty): Unit = println("Duty received")
}
