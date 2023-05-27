package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import protocol._

object Monitor extends MonitorActor {
  override def eventReceived(
      reasoner: ActorRef[Message],
      enforcer: ActorRef[Message],
      event: SystemEvent
  ): Unit = {
    event match {
      case event: TenantCreated => {
        val p = new PTenant(event.name)
        enforcer ! Inform(p)
        reasoner ! Inform(p)
      }
      case event: RentalAgreementCreated => {
        val p = new PRentalAgreement(event.id, event.tenantName)
        enforcer ! Inform(p)
        reasoner ! Inform(p)
      }
    }
  }
}
