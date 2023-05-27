package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

trait SystemEvent {
  def apply(): Unit = {
    MonitorActors.receive(this)
  }
}

trait MonitorSubscription {
  def addMonitor(m: ActorRef[SystemEvent]): Unit

  def receive(event: SystemEvent): Unit
}

object MonitorActors extends MonitorSubscription {
  private var active: List[ActorRef[SystemEvent]] = List()

  override def addMonitor(m: ActorRef[SystemEvent]): Unit = {
    this.active = m :: this.active
  }

  override def receive(event: SystemEvent): Unit = {
    for (monitor <- this.active) {
      monitor ! event
    }
  }
}

case class MMessage[M](from: ActorRef[_], to: ActorRef[M], msg: M)
    extends SystemEvent {
  override def apply(): Unit = {
    to ! msg
    MonitorActors.receive(this)
  }
}
