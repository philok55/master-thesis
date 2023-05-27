package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import protocol._

object KnowledgeBase {
  var tenants: List[String] = List()
  var agreements: List[PRentalAgreement] = List()
}

object Enforcer extends EnforcerActor {
  def apply(
      reasoner: ActorRef[Message]
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.setup { context =>
      val listener = context.spawn(
        protocolRequestsLoop(reasoner),
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

  override def handleInform(predicate: Predicate): Unit = {
    predicate match {
      case tenant: PTenant => {
        KnowledgeBase.tenants = tenant.name :: KnowledgeBase.tenants
      }
      case agreement: PRentalAgreement => {
        println("Enforcer: received agreement")
        KnowledgeBase.agreements = agreement :: KnowledgeBase.agreements
      }
    }
  }
}
