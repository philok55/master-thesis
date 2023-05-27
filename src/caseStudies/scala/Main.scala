package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime

import protocol._

object CaseStudies {
  sealed trait Scenario { val resolver: ActorRefResolver }
  final case class AccessControlCase(resolver: ActorRefResolver)
      extends Scenario

  def apply(): Behavior[Scenario] = Behaviors.receive { (context, message) =>
    implicit val resolver: ActorRefResolver = message.resolver
    message match {
      case m: AccessControlCase => {
        val reasoner = context.spawn(
          Reasoner("src/caseStudies/eflint/accessControl.eflint"),
          "reasoner"
        )
        val enforcer = context.spawn(
          Enforcer(reasoner),
          "enforcer"
        )
        val monitor = context.spawn(
          Monitor(reasoner, enforcer, MonitorActors),
          "monitor"
        )
        val tenant1 = context.spawn(
          Tenant(enforcer),
          "tenant1"
        )
        TenantCreated(tenant1.path.name)()
        val tenant2 = context.spawn(
          Tenant(enforcer),
          "tenant2"
        )
        TenantCreated(tenant2.path.name)()
        val tenant3 = context.spawn(
          Tenant(enforcer),
          "tenant3"
        )
        val agreement = new Document(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1."
        )
        RentalAgreementCreated(agreement.id, tenant1.path.name)()
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

object CaseStudiesMain extends App {
  val system: ActorSystem[CaseStudies.Scenario] =
    ActorSystem(CaseStudies(), "CaseStudies")
  val resolver = ActorRefResolver(system)

  system ! CaseStudies.AccessControlCase(resolver)
}
