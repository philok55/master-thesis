package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

trait MonitorActor {
  def apply(
      reasoner: ActorRef[Message],
      enforcer: ActorRef[Message],
      subscription: MonitorSubscription
  )(implicit resolver: ActorRefResolver): Behavior[SystemEvent] =
    Behaviors.setup { context =>
      subscription.addMonitor(context.self)
      Behaviors.receiveMessage { message =>
        eventReceived(reasoner, enforcer, message)
        Behaviors.same
      }
    }

  def eventReceived(
      reasoner: ActorRef[Message],
      enforcer: ActorRef[Message],
      event: SystemEvent
  )(implicit resolver: ActorRefResolver): Unit
}
