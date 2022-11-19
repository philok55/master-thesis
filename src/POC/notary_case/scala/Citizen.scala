package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import norms.Message
import norms.ViolatedAction
import norms.ActionValue

object Citizen {
  case class MortgageRequestDenied(reason: String) extends norms.Message
  case class MortgageRequestApproved() extends norms.Message

  def apply(notary: ActorRef[norms.Message]): Behavior[norms.Message] = Behaviors.setup { context =>
    notary ! Notary.RequestCoveredMortgage(context.self, new Property("Beresteinseweg", 750000), 750000)
    listen()
  }

  def listen(): Behavior[norms.Message] = Behaviors.receive { (context, message) =>
    message match {
      case m:MortgageRequestDenied =>
        println(s"Mortgage Request denied: ${m.reason}")
        Behaviors.same
      case m:MortgageRequestApproved =>
        println(s"Yeayy Mortgage Request approved!")
        Behaviors.stopped
      case ViolatedAction(ActionValue(Right(p), Right(r), action)) =>
        println(s"(citizen) VIOLATED by $p affecting $r: $action")
        Behaviors.same
    }
  }
}

