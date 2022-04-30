package notary

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object NotaryMain {
  sealed trait Scenarios { val resolver:ActorRefResolver }
  final case class TestCase1(resolver:ActorRefResolver) extends Scenarios

  def apply(): Behavior[Scenarios] =
    Behaviors.setup { context => Behaviors.receive { (context, message) =>
      Behaviors.stopped
    }}
}

object FirstTestCase extends App {
  val notaryMain: ActorSystem[NotaryMain.Scenarios] = ActorSystem(NotaryMain(), "NotaryExample")
  val resolver = ActorRefResolver(notaryMain)
}