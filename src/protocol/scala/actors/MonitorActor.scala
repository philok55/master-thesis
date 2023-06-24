package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

/**
 * Abstract monitor actor implementation.
 * 
 * Implement eventReceived to define the monitor actor's behavior.
 * 
 * @param reasoner The reasoner actor
 * @param enforcer The enforcer actor
 * @param subscription The subscription to subscribe to
 */
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
