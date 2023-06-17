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
  final case class DutyMonitoringCase(resolver: ActorRefResolver)
      extends Scenario
  final case class QueriesCase(resolver: ActorRefResolver) extends Scenario
  final case class InformationFetchCase(resolver: ActorRefResolver)
      extends Scenario

  def genericSetup(
      context: ActorContext[CaseStudies.Scenario]
  )(implicit resolver: ActorRefResolver): List[ActorRef[Message]] = {
    val reasoner = context.spawn(
      Reasoner("src/caseStudies/eflint/caseStudies.eflint"),
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
      "owner1"
    )
    OwnerCreated(resolver.toSerializationFormat(owner))()
    return List(enforcer, owner, database)
  }

  def apply(): Behavior[Scenario] = Behaviors.receive { (context, message) =>
    implicit val resolver: ActorRefResolver = message.resolver
    message match {
      case m: AccessControlCase => {
        println("----------------------------------")
        println("Access control case study")
        println("----------------------------------")

        val setup = genericSetup(context)
        val enforcer = setup(0)
        val owner = setup(1)
        val database = setup(2)

        val tenant1 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant1"
        )
        val tenant2 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant2"
        )
        val tenant3 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant3"
        )
        owner ! Owner.CreateTenant(tenant1)
        owner ! Owner.CreateTenant(tenant2)

        Thread.sleep(1000)

        owner ! Owner.CreateAgreement(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1.",
          tenantAddress = resolver.toSerializationFormat(tenant1),
          price = 1250
        )

        Thread.sleep(1000)

        // Access allowed
        tenant1 ! Tenant.FetchAgreement("agreement1")
        // Access not allowed
        tenant2 ! Tenant.FetchAgreement("agreement1")
        // Reject (unregistered tenant, not created by owner)
        tenant3 ! Tenant.FetchAgreement("agreement1")
      }
      case m: ExPostCase => {
        println("----------------------------------")
        println("Ex-post enforcement case study")
        println("----------------------------------")

        val setup = genericSetup(context)
        val enforcer = setup(0)
        val owner = setup(1)
        val database = setup(2)

        val tenant1 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant1"
        )
        owner ! Owner.CreateTenant(tenant1)

        Thread.sleep(1000)

        owner ! Owner.CreateAgreement(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1.",
          tenantAddress = resolver.toSerializationFormat(tenant1),
          price = 1250
        )

        Thread.sleep(1000)

        // ACTION VIOLATION
        // ----------------------------------
        // Allowed
        owner ! Owner.IndexAgreement("agreement1", 3)
        // Violation
        owner ! Owner.IndexAgreement("agreement1", 5)

        Thread.sleep(1000)

        // DUTY VIOLATION
        // ----------------------------------
        owner ! Owner.SetPaymentDeadline(
          resolver.toSerializationFormat(tenant1),
          1200,
          "01-07-2023"
        )
        // Causes violation, tenant is sent a reminder
        owner ! Owner.MarkRentAsDue(
          resolver.toSerializationFormat(tenant1),
          1200,
          "01-07-2023"
        )
      }
      case m: DutyMonitoringCase => {
        println("----------------------------------")
        println("Duty monitoring case study")
        println("----------------------------------")

        val setup = genericSetup(context)
        val enforcer = setup(0)
        val owner = setup(1)
        val database = setup(2)

        val tenant1 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant1"
        )
        owner ! Owner.CreateTenant(tenant1)

        Thread.sleep(1000)

        owner ! Owner.CreateAgreement(
          id = "agreement1",
          fileName = "agreement1.pdf",
          content = "Content of agreement 1.",
          tenantAddress = resolver.toSerializationFormat(tenant1),
          price = 1250
        )
        Thread.sleep(1000)
        // Deposit collected after agreement started
        owner ! Owner.CollectDeposit("agreement1", 2500)
        Thread.sleep(1000)
        // Creates duty to pay back deposit
        owner ! Owner.RegisterAgreementTermination("agreement1")
        Thread.sleep(1000)
        // Terminates duty to pay back deposit
        owner ! Owner.RefundDeposit("agreement1", 2500)
      }
      case m: QueriesCase => {
        println("----------------------------------")
        println("Queries case study")
        println("----------------------------------")

        val setup = genericSetup(context)
        val enforcer = setup(0)
        val owner = setup(1)
        val database = setup(2)
      }
      case m: InformationFetchCase => {
        println("----------------------------------")
        println("Information fetch case study")
        println("----------------------------------")

        val setup = genericSetup(context)
        val enforcer = setup(0)
        val owner = setup(1)
        val database = setup(2)

        val taxAuthority = context.spawn(TaxAuthority(), "taxAuth")
        val tenant1 = context.spawn(
          Tenant(enforcer, contacts = Map("owner" -> owner)),
          "tenant1"
        )

        Thread.sleep(1000)

        // Register tenant income (would happen from outside system)
        taxAuthority ! TaxAuthority.AddObject(
          s"income-${resolver.toSerializationFormat(tenant1)}",
          58000
        )

        Thread.sleep(1000)

        taxAuthority ! Request(
          new PIncome(new PTenant(resolver.toSerializationFormat(tenant1)), 0),
          tenant1
        )
        // TODO: go through reasoner for request
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

  // NOTE: currently only one case can be run at a time.
  // Uncomment the one that should be run.

  // system ! CaseStudies.AccessControlCase(resolver)
  system ! CaseStudies.ExPostCase(resolver)
  // system ! CaseStudies.DutyMonitoringCase(resolver)
  // system ! CaseStudies.QueriesCase(resolver)
  // system ! CaseStudies.InformationFetchCase(resolver)
}
