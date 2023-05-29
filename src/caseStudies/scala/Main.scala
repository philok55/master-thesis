package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime
import scala.collection.Map

import protocol._

object CaseStudies {
  sealed trait Scenario { val resolver: ActorRefResolver }
  final case class AccessControlCase(resolver: ActorRefResolver)
      extends Scenario
  final case class ExPostCase(resolver: ActorRefResolver) extends Scenario

  def apply(): Behavior[Scenario] = Behaviors.receive { (context, message) =>
    implicit val resolver: ActorRefResolver = message.resolver
    message match {
      case m: AccessControlCase => {
        println("----------------------------------")
        println("Access control case study")
        println("----------------------------------")

        val reasoner = context.spawn(
          Reasoner("src/caseStudies/eflint/accessControl.eflint"),
          "reasoner"
        )
        val enforcer = context.spawn(
          Enforcer(reasoner),
          "enforcer"
        )
        reasoner ! Reasoner.RegisterEnforcer(enforcer)
        val database = context.spawn(Database(enforcer), "database")
        enforcer ! Enforcer.AddContact("db", database)
        val monitor =
          context.spawn(Monitor(reasoner, enforcer, MonitorActors), "monitor")

        val owner = context.spawn(
          Owner(enforcer, contacts = Map("db" -> database)),
          "owner"
        )

        val tenant1 = context.spawn(Tenant(enforcer), "tenant1")
        val tenant2 = context.spawn(Tenant(enforcer), "tenant2")
        val tenant3 = context.spawn(Tenant(enforcer), "tenant3")
        owner ! Owner.CreateTenant(tenant1)
        owner ! Owner.CreateTenant(tenant2)

        Thread.sleep(1000)

        owner ! Owner.CreateAgreement(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1.",
          tenantName = tenant1.path.name
        )

        Thread.sleep(1000)

        // Access allowed
        tenant1 ! Tenant.FetchAgreement("agreement1")
        // Access not allowed
        tenant2 ! Tenant.FetchAgreement("agreement1")
        // Reject (unregistered tenant, not created by owner)
        tenant3 ! Tenant.FetchAgreement("agreement1")

        // Violation:
        reasoner ! InformAct(new AccessDocument(new PTenant("tenant2"), "agreement1"))
      }

      case m: ExPostCase => {
        println("----------------------------------")
        println("Ex-post enforcement case study")
        println("----------------------------------")
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
