package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.NormActor

trait ReasonerActor {
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
        case _ => println("Error: reasoner received unknown message")
      }
      Behaviors.same
    }

  def queryResponseHandler(
      msg: QueryMessage,
      eflint_server: ActorRef[NormActor.Message]
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[NormActor.QueryResponse] = Behaviors.setup { context =>
    val phrase = EflintAdapter(msg)
    eflint_server ! NormActor.Query(
      replyTo = context.self,
      phrase = phrase
    )

    Behaviors.receiveMessage {
      case response: norms.NormActor.Response => {
        println(
          s"Reasoner received response. Query: $phrase; Response: $response"
        )
        msg match {
          case m: RequestAct => {
            if (response.success) m.replyTo ! Permit(m.act)
            else m.replyTo ! Forbid(m.act)
          }
          case _ => println("NotImplementedError")
        }
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
    val phrase = EflintAdapter(msg)
    println(s"Reasoner sending phrase: $phrase")
    eflint_server ! NormActor.Phrase(
      phrase = phrase,
      handler = context.self
    )

    Behaviors.receiveMessage {
      case response: norms.NormActor.Message => {
        println(
          s"Reasoner received response. Query: $phrase; Response: $response"
        )
        Behaviors.stopped
      }
    }
  }
}
