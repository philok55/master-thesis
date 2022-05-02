package notary_case

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import notary_case.data.Input

object NotaryMain {
  sealed trait Scenarios { val resolver:ActorRefResolver }
  final case class TestCase1(resolver:ActorRefResolver) extends Scenarios

  def apply(): Behavior[Scenarios] = Behaviors.setup { context => {
    Behaviors.receive { (context, message) =>
      implicit val resolver:ActorRefResolver = message.resolver
      message match {
        case m:TestCase1 => {
          println("Starting NotaryMain: TestCase1")
          val monitor = context.spawn(NimMonitor(), "NimMonitor")
          val notary = context.spawn(Notary(monitor), "HansDeNotaris")
          val alice = context.spawn(Citizen(notary), "Alice")
          Thread.sleep(5000)
        }
      }
      Behaviors.stopped
    }
  }}
}

object FirstTestCase extends App {
  val notaryMain: ActorSystem[NotaryMain.Scenarios] = ActorSystem(NotaryMain(), "NotaryExample")
  val resolver = ActorRefResolver(notaryMain)
  notaryMain ! NotaryMain.TestCase1(resolver)
}