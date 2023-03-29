package caseStudy

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime

import protocol._

object CaseStudy {
  sealed trait Scenario { val resolver: ActorRefResolver }
  final case class AccessControlCase(resolver: ActorRefResolver)
      extends Scenario

  def apply(): Behavior[Scenario] = Behaviors.receive { (context, message) =>
    implicit val resolver: ActorRefResolver = message.resolver
    message match {
      case m: AccessControlCase => {
        val reasoner = context.spawn(
          Reasoner("src/caseStudy/eflint/caseStudy.eflint"),
          "reasoner"
        )
        val enforcer = context.spawn(
          Enforcer(reasoner),
          "enforcer"
        )
        val tenant1 = context.spawn(
          Tenant(enforcer),
          "tenant1"
        )
        val tenant2 = context.spawn(
          Tenant(enforcer),
          "tenant2"
        )
        val tenant3 = context.spawn(
          Tenant(enforcer),
          "tenant3"
        )
        enforcer ! Enforcer.RegisterTenant(tenant1)
        enforcer ! Enforcer.RegisterTenant(tenant2)
        // reasoner ! Request(Predicate("tenant", List(PString("tenant1"))), enforcer)
        val agreementDocument = new Document(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1."
        )
        enforcer ! Enforcer.RegisterAgreement(agreementDocument, tenant1)
        Thread.sleep(2000)
        // Access allowed
        tenant1 ! Tenant.AccessDocument("agreement1")
        // Access not allowed
        tenant2 ! Tenant.AccessDocument("agreement1")
        // Reject (unregistered tenant)
        tenant3 ! Tenant.AccessDocument("agreement1")
      }
    }
    Thread.sleep(5000)
    Behaviors.stopped
  }
}

object CaseStudyMain extends App {
  val system: ActorSystem[CaseStudy.Scenario] =
    ActorSystem(CaseStudy(), "CaseStudy")
  val resolver = ActorRefResolver(system)

  system ! CaseStudy.AccessControlCase(resolver)
}
