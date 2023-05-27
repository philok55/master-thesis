package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import protocol._

object Tenant extends ApplicationActor {
  final case class AccessDocument(documentId: String) extends Message

  def getPredicate(name: String): Predicate =
    Predicate("tenant", List(PString(name)))

  def apply(
      enforcer: ActorRef[Message],
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case m: AccessDocument => {
          val act = Act(
            name = "access-document",
            actor = getPredicate(context.self.path.name),
            relatedTo = List(
              Predicate("document", List(PString(m.documentId)))
            )
          )
          context.spawn(
            actRequestHandler(act, enforcer),
            s"app-act-request-${java.util.UUID.randomUUID.toString()}"
          )
        }
      }
      Behaviors.same
    }

  override def actPermitted(act: Act): Unit = println("Act permitted")

  override def actForbidden(act: Act): Unit = println("Act forbidden")

  override def actRejected(act: Act): Unit = println("Act rejected")
}
