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

  def apply(): Behavior[Scenario] = Behaviors.receive { (context, message) =>
    implicit val resolver: ActorRefResolver = message.resolver
    message match {
      case m: TestAdapter => {
        val reasoner = context.spawn(
          Reasoner("src/protocol/eflint/accessControl.eflint"),
          "reasoner"
        )
        reasoner ! Inform(Predicate("subject", List(PString("alice"))))
        reasoner ! Inform(
          Predicate(
            "target",
            List(
              Predicate("subject", List(PString("alice"))),
              Predicate("resource", List(PString("SampleServer"))),
              Predicate("action", List(PString("read")))
            )
          )
        )
        reasoner ! Request(
          Predicate(
            "target",
            List(
              Predicate("subject", List(PString("alice"))),
              Predicate("resource", List(PString("SampleServer"))),
              Predicate("action", List(PString("read")))
            )
          )
        )
        reasoner ! RequestAct(
          Act(
            "access-resource",
            reasoner,
            reasoner,
            List[Predicate](),
            LocalDateTime.now()
          )
        )
      }
    }
    Thread.sleep(5000)
    Behaviors.stopped
  }
}

object DevTestCase extends App {
  val system: ActorSystem[ProtocolTestMain.Scenario] =
    ActorSystem(ProtocolTestMain(), "ProtocolTestSystem")
  val resolver = ActorRefResolver(system)
  system ! ProtocolTestMain.TestAdapter(resolver)
}
