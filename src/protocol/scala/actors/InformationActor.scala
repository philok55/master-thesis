package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Set
import norms.NormActor

trait InformationActor {
  protected object KnowledgeBase {
    var objects: Map[String, Any] = Map()
  }

  final case class AddObject(
      id: String,
      obj: Any
  ) extends Message

  def apply()(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case AddObject(id, obj) => {
          KnowledgeBase.objects = KnowledgeBase.objects + (id -> obj)
          Behaviors.same
        }
        case m: Request => {
          println(s"Information actor received request: $m")
          val key = getKey(m)
          val value = KnowledgeBase.objects get key
          value match {
            case None => {
              val p = Proposition(
                identifier = m.proposition.identifier,
                instance = m.proposition.instance,
                state = Unknown
              )
              m.replyTo ! Inform(p)
            }
            case Some(value) => {
              println(s"Information actor found value for request: $value")
              val p: Proposition = getProp(m.proposition, value)
              m.replyTo ! Inform(p)
            }
          }
          Behaviors.same
        }
      }
    }

  def getKey(message: Request): String

  def getProp(prop: Proposition, value: Any): Proposition
}
