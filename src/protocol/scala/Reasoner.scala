package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.NormActor

object Reasoner {
  

  def apply(
      eflintFile: String
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.setup { context =>
      val eflintActor = new NormActor(
        eflintFile,
        debug_mode = true
      )
      val eflintServer = context.spawn(eflintActor.listen(), "eFLINT-actor")
      listen(eflintServer)
    }

  def listen(
      eflint_server: ActorRef[NormActor.Message]
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case m: QueryMessage =>
          context.spawn(
            queryResponseHandler(m, eflint_server),
            s"qresponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
        case m: Message =>
          context.spawn(
            phraseResponseHandler(m, eflint_server),
            s"presponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
        case _ => println("Reasoner received unknown message")
      }
      Behaviors.same
    }

  def queryResponseHandler(
      msg: QueryMessage,
      eflint_server: ActorRef[NormActor.Message]
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[NormActor.QueryResponse] = Behaviors.setup { context =>
    eflint_server ! NormActor.Query(
      replyTo = context.self,
      phrase = EflintAdapter(msg)
    )

    Behaviors.receiveMessage {
      case response: norms.NormActor.Response => {
        println(s"Reasoner received response: $response")
        Behaviors.stopped
      }
    }
  }

  def phraseResponseHandler(
      msg: Message,
      eflint_server: ActorRef[NormActor.Message]
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[norms.Message] = Behaviors.setup { context =>
    eflint_server ! NormActor.Phrase(
      phrase = EflintAdapter(msg),
      handler = context.self
    )

    Behaviors.receiveMessage {
      case response: norms.NormActor.Message => {
        println(s"Reasoner received response: $response")
        Behaviors.stopped
      }
    }
  }
}
