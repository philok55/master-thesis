package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Set
import scala.collection.Map

import protocol._

object Enforcer extends EnforcerActor {
  override def blockedActions = Set("access-document")

  object KnowledgeBase {
    var tenants: List[String] = List()
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
      val tenantAddress = m.act.pActor match {
        case a: PTenant => a.name
        case _          => ""
      }
      if (KnowledgeBase.tenants.contains(tenantAddress)) {
        true
      } else {
        m.replyTo ! Reject(Left(m.act))
        false
      }
    }
    case RequestDuty(duty, replyTo) => {
      duty match {
        case Duty(_, Some(holder), _, _, _, _) => {
          if (holder == replyTo) {
            true
          } else {
            replyTo ! Reject(Right(duty))
            false
          }
        }
        case _ => {
          replyTo ! Reject(Right(duty))
          false
        }
      }
    }
    case _ => true
  }

  override def handleInform(proposition: Proposition): Unit = {
    proposition match {
      case tenant: PTenant => {
        KnowledgeBase.tenants = tenant.name :: KnowledgeBase.tenants
      }
      case _ => println(s"Enforcer: unhandled proposition: ${proposition}")
    }
  }

  override def handleInformDuty(duty: Duty): Unit = {
    println(s"Enforcer: received newly active duty: ${duty.name}")
    duty.holder match {
      case Some(holder) => holder ! InformDuty(duty)
    }
    duty.claimant match {
      case Some(claimant) => claimant ! InformDuty(duty)
    }
  }

  override def handleTerminatedDuty(duty: Duty): Unit = {
    println(s"Enforcer: received terminated duty: ${duty.name}")
    duty.holder match {
      case Some(holder) => holder ! InformDutyTerminated(duty)
    }
    duty.claimant match {
      case Some(claimant) => claimant ! InformDutyTerminated(duty)
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

  override def violatedAct(
      act: Act,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {
    println(s"Enforcer: act violated. Sending violation report to ${act.actor}")
    act match {
      case Act("index-agreement", actor, _, _, _, _, _) => {
        actor ! Owner.DisplayViolation(
          s"Indexing of agreement was against the rules. Please reconsider."
        )
      }
      case _ => {
        act.actor ! Owner.DisplayViolation(
          s"Act ${act.name} caused violation"
        )
      }
    }
  }

  override def violatedDuty(
      duty: Duty,
      contacts: Map[String, ActorRef[Message]] = Map()
  ): Unit = {
    duty match {
      case Duty("pay-rent", Some(actor), _, _, _, _) => {
        println(
          s"Enforcer received violated duty: rent was not paid by tenant ${actor.path.name}. Sending email to tenant."
        )
        // Actual email sending would happen here
      }
      case _ => {
        println(s"Enforcer received unknown duty violation: ${duty.name}")
      }
    }
  }
}
