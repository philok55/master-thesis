package access_control

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

object AccessControlMain {
  sealed trait Scenario { val resolver:ActorRefResolver }
  final case class TestCase1(resolver:ActorRefResolver) extends Scenario

  def apply(): Behavior[Scenario] = Behaviors.setup { context =>
    Behaviors.receive { (context, message) =>
      implicit val resolver: ActorRefResolver = message.resolver
      message match {
        case m: TestCase1 => {
          println("Starting Access Control system: TestCase1")
          val reasoner = context.spawn(PolicyReasoner(), "policy-reasoner")
          val enforcer = context.spawn(PolicyEnforcer(reasoner), "policy-enforcer")
          val admin = context.spawn(AdminApp(reasoner, enforcer), "admin-app")
          admin ! AdminApp.CreateClientApp("client-app")
          admin ! AdminApp.CreateResource("protected-resource")
          Thread.sleep(5000)
        }
      }
      Behaviors.stopped
    }
  }
}

object DevTestCase extends App {
  val system: ActorSystem[AccessControlMain.Scenario] = ActorSystem(AccessControlMain(), "AccessControlSystem")
  val resolver = ActorRefResolver(system)
  system ! AccessControlMain.TestCase1(resolver)
}