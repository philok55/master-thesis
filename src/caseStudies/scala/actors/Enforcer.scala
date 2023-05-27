package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import protocol._

object KnowledgeBase {
  var tenants: List[String] = List()
  var agreements: List[(Document, String)] = List()
}

object Enforcer extends EnforcerActor {
  final case class RegisterTenant(tenant: ActorRef[Message]) extends Message
  final case class RegisterAgreement(
      document: Document,
      tenant: ActorRef[Message]
  ) extends Message

  def apply(
      reasoner: ActorRef[Message]
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.setup { context =>
      val listener = context.spawn(
        protocolRequestsLoop(reasoner),
        s"enf-protocol-loop-${java.util.UUID.randomUUID.toString()}"
      )

      Behaviors.receiveMessage {
        case m: RegisterTenant => {
          KnowledgeBase.tenants = m.tenant.path.name :: KnowledgeBase.tenants
          reasoner ! Inform(Tenant.getPredicate(m.tenant.path.name))
          Behaviors.same
        }
        case m: RegisterAgreement => {
          KnowledgeBase.agreements =
            (m.document, m.tenant.path.name) :: KnowledgeBase.agreements
          reasoner ! Inform(
            Predicate(
              "rental-agreement",
              List(
                Tenant.getPredicate(m.tenant.path.name),
                m.document.getPredicate()
              )
            )
          )
          Behaviors.same
        }
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
}
