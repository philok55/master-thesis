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
          val client1 = context.spawn(Client(), "Philo")
          val client2 = context.spawn(Client(), "ScraperBot")

          val reasoner = context.spawn(PolicyReasoner(), "policy-reasoner")
          val enforcer = context.spawn(PolicyEnforcer(reasoner), "policy-enforcer")

          val resource1 = new Resource(enforcer, "SampleServer")
          resource1.ref = Some(context.spawn(resource1.start(), resource1.name))
          val resource2 = new Resource(enforcer, "SuperSectretServer")
          resource2.ref = Some(context.spawn(resource2.start(), resource2.name))

          client1 ! Client.TryAccessResource(resource1, "login")
          client1 ! Client.TryAccessResource(resource2, "login")

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