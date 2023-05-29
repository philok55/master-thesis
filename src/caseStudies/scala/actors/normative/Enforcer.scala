package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object Enforcer extends EnforcerActor {
  object KnowledgeBase {
    var tenants: List[String] = List()
    var agreements: List[PRentalAgreement] = List()
  }

  def apply(
      reasoner: ActorRef[Message],
      contacts: Map[String, ActorRef[Message]] = Map()
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.setup { context =>
      val listener = context.spawn(
        protocolRequestsLoop(reasoner, contacts),
        s"enf-protocol-loop-${java.util.UUID.randomUUID.toString()}"
      )

      Behaviors.receiveMessage {
        case m: Message => {
          listener ! m
          Behaviors.same
        }
      }
    }

  override def acceptOrSendReject(message: Message): Boolean = message match {
    case m: RequestAct => {
      val tenantName = m.act.actor.instance(0) match {
        case PString(name) => name
        case _             => ""
      }
      if (KnowledgeBase.tenants.contains(tenantName)) {
        true
      } else {
        m.replyTo ! Rejected(m.act)
        false
      }
    }
    case _ => false
  }

  override def handleInform(proposition: Proposition): Unit = {
    proposition match {
      case tenant: PTenant => {
        KnowledgeBase.tenants = tenant.name :: KnowledgeBase.tenants
      }
      case agreement: PRentalAgreement => {
        KnowledgeBase.agreements = agreement :: KnowledgeBase.agreements
      }
    }
  }

  override def actPermitted(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {
    val database = contacts("db")
    act match {
      case a: AccessDocument => {
        println(s"Enforcer: facilitating access to ${a.documentId}")	
        database ! Database.GetAgreement(a.documentId, a.actor)
      }
    }
  }
}
