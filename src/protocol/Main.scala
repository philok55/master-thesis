package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import java.time.LocalDateTime

object ProtocolTestMain {
  sealed trait Scenario { val resolver: ActorRefResolver }
  final case class TestAdapter(resolver: ActorRefResolver) extends Scenario

  def apply(): Behavior[Scenario] = Behaviors.setup { context =>
    Behaviors.receive { (context, message) =>
      implicit val resolver: ActorRefResolver = message.resolver
      message match {
        case m: TestAdapter => {
          val reasoner = context.spawn(Reasoner(), "reasoner")
          reasoner ! Inform(
            Predicate(True, "resource", List(PredicateString("test")))
          )
          reasoner ! InformAct(Act("act", reasoner, reasoner, LocalDateTime.now()))
        }
      }
      Behaviors.stopped
    }
  }
}

object DevTestCase extends App {
  val system: ActorSystem[ProtocolTestMain.Scenario] =
    ActorSystem(ProtocolTestMain(), "ProtocolTestSystem")
  val resolver = ActorRefResolver(system)
  system ! ProtocolTestMain.TestAdapter(resolver)
}
