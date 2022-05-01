package notary_case

import akka.actor.typed.ActorRefResolver
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef

object Citizen {
  trait Message extends norms.Message
  case class MortgageRequestDenied(reason: String) extends Message
  case class MortgageRequestApproved() extends Message

  def apply(notary: ActorRef[Notary.Message]): Behavior[Message] = Behaviors.setup { context =>
    notary ! Notary.RequestCoveredMortgage(context.self, new Property("Beresteinseweg", 750000), 750000)
    listen()
  }

  def listen(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case m:MortgageRequestDenied =>
        println(s"Mortgage Request denied: ${m.reason}")
        Behaviors.same
      case m:MortgageRequestApproved =>
        println(s"Yeayy Mortgage Request approved!")
        Behaviors.stopped
    }
  }
}

