package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Set
import norms.NormActor

/**
 * Abstract information actor implementation.
 * 
 * Implement getKey and getProp to define the information actor's behavior.
 */
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
              println(
                s"Information actor found value for request: $value. Sending to reasoner."
              )
              val p: Proposition = getProp(m.proposition, value)
              m.replyTo ! Inform(p)
            }
          }
          Behaviors.same
        }
      }
    }

  /**
   * Returns the key in the knowledge base for the given request.
   *
   * @param message
   */
  def getKey(message: Request): String

  /**
   * Returns a new proposition by inserting the found value in 
   * the correct field of the original proposition.
   *
   * @param prop the original proposition
   * @param value the value found in the knowledge base
   */
  def getProp(prop: Proposition, value: Any): Proposition
}
