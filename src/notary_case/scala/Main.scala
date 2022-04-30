package notary

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior

object NotaryMain {
  sealed trait Scenarios { val resolver:ActorRefResolver }
  final case class TestCase1(resolver:ActorRefResolver) extends Scenarios

  def apply(): Behavior[Scenarios] =
    Behaviors.setup { context =>
      context.log.info("NotaryMain started")
    }
}

object FirstTestCase extends App {
  val notaryMain: ActorSystem[BankingMain.Scenarios] = ActorSystem(NotaryMain(), "NotaryExample")
  val resolver = ActorRefResolver(NotaryMain)
}