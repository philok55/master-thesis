package protocol

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import norms.NormActor

trait ReasonerActor {
  final case class RegisterEnforcer(enforcer: ActorRef[Message]) extends Message
  final case class RegisterInfoActor(actor: ActorRef[Message]) extends Message

  var pendingMessage: Option[Message] = None
  var informationActor: Option[ActorRef[Message]] = None

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
      eflintServer: ActorRef[NormActor.Message],
      enforcer: Option[ActorRef[Message]] = None
  )(implicit resolver: ActorRefResolver): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case RegisterEnforcer(enf) => listen(eflintServer, Some(enf))
        case RegisterInfoActor(actor) => {
          informationActor = Some(actor)
          Behaviors.same
        }
        case m: RequestDuty =>
          context.spawn(
            dutyResponseHandler(m, eflintServer),
            s"dresponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          Behaviors.same
        case m: QueryMessage =>
          context.spawn(
            queryResponseHandler(m, eflintServer),
            s"qresponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          Behaviors.same
        case m: Inform => {
          context.spawn(
            phraseResponseHandler(m, eflintServer, context.self, enforcer),
            s"presponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          pendingMessage match {
            case Some(pending) => {
              context.spawn(
                phraseResponseHandler(
                  pending,
                  eflintServer,
                  context.self,
                  enforcer
                ),
                s"presponse-handler-${java.util.UUID.randomUUID.toString()}"
              )
              pendingMessage = None
            }
            case None => {}
          }
          Behaviors.same
        }
        case m: Message =>
          context.spawn(
            phraseResponseHandler(m, eflintServer, context.self, enforcer),
            s"presponse-handler-${java.util.UUID.randomUUID.toString()}"
          )
          Behaviors.same
        case _ =>
          println("Error: reasoner received unknown message")
          Behaviors.same
      }
    }

  def queryResponseHandler(
      msg: QueryMessage,
      eflintServer: ActorRef[NormActor.Message]
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[NormActor.QueryResponse] = Behaviors.setup { context =>
    val phrase = EflintAdapter(msg)
    eflintServer ! NormActor.Query(
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
            if (response.success) m.replyTo ! Permitted(m.act)
            else m.replyTo ! Forbidden(m.act)
          }
          case m: Request => {
            if (response.success) m.replyTo ! Inform(m.proposition)
            else {
              val np = Proposition(
                identifier = m.proposition.identifier,
                instance = m.proposition.instance,
                state = False
              )
              m.replyTo ! Inform(np)
            }
          }
          case _ => println("NotImplementedError")
        }
        Behaviors.stopped
      }
      case response: norms.NormActor.QueryInputRequired => {
        println(
          s"Reasoner received input required. Query: $phrase; Response: $response"
        )
        Behaviors.same
      }
      case response: norms.Message => {
        println(
          s"Reasoner received unknown response. Query: $phrase; Response: $response"
        )
        Behaviors.same
      }
    }
  }

  def phraseResponseHandler(
      msg: Message,
      eflintServer: ActorRef[NormActor.Message],
      parent: ActorRef[Message],
      enforcer: Option[ActorRef[Message]] = None
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[norms.Message] = Behaviors.setup { context =>
    val phrase = EflintAdapter(msg)
    println(s"Reasoner evaluating phrase: $phrase")
    eflintServer ! NormActor.Phrase(
      phrase = phrase,
      handler = context.self
    )

    enforcer match {
      case None =>
        context.log.error(
          "Reasoner received query before enforcer was registered."
        )
        Behaviors.stopped
      case Some(enf) =>
        Behaviors.receiveMessage {
          case norms.ViolatedAction(action) => {
            println(s"Reasoner received violated action: $action")
            msg match {
              case m: InformAct => enf ! InformViolatedAct(m.act)
              case _ =>
                context.log.error(
                  s"Unexpected response to ${phrase}: ViolatedAction"
                )
            }
            Behaviors.same
          }
          case norms.ViolatedDuty(duty) => {
            println(s"Reasoner received violated duty: $duty")
            val d = parseDuty(duty)
            d match {
              case Some(d) => enf ! InformViolatedDuty(d)
              case None    => {}
            }
            Behaviors.same
          }
          case norms.ActiveDuty(duty) => {
            println(s"Reasoner received active duty: $duty")
            val d = parseDuty(duty)
            d match {
              case Some(d) => enf ! InformDuty(d)
              case None    => {}
            }
            Behaviors.same
          }
          case norms.TerminatedDuty(duty) => {
            println(s"Reasoner received terminated duty: $duty")
            val d = parseDuty(duty)
            d match {
              case Some(d) => enf ! InformDutyTerminated(d)
              case None    => {}
            }
            Behaviors.same
          }
          case norms.ExecutedAction(action) => {
            // No use case yet
            Behaviors.same
          }
          case response: norms.DecisionInputRequired => {
            println(
              s"Reasoner received input required. Phrase: $phrase; Response: $response"
            )
            val prop: Option[Proposition] = getInformationProp(msg, response)
            prop match {
              case Some(r) => {
                informationActor match {
                  case Some(infoActor) => {
                    pendingMessage = Some(msg)
                    val message = Request(r, parent)
                    infoActor ! message
                  }
                  case None =>
                    context.log.error(
                      "Reasoner received input required, but no information actor is registered."
                    )
                }
              }
              case None => {}
            }
            Behaviors.same
          }
          case response: norms.Message => {
            context.log.error(
              s"Reasoner received unknown message from eFLINT. Phrase: $phrase; Response: $response"
            )
            Behaviors.stopped
          }
        }
    }
  }

  def dutyResponseHandler(
      msg: RequestDuty,
      eflintServer: ActorRef[NormActor.Message]
  )(implicit
      resolver: ActorRefResolver
  ): Behavior[norms.Message] = Behaviors.setup { context =>
    val holder = msg.duty.pHolder match {
      case Some(h) => EflintAdapter.propToEflintSimple(h)
      case None    => ""
    }
    val claimant = msg.duty.pClaimant match {
      case Some(c) => EflintAdapter.propToEflintSimple(c)
      case None    => ""
    }
    eflintServer ! NormActor.FindDuties(
      holder,
      claimant,
      context.self
    )
    Behaviors.receiveMessage {
      case norms.ActiveDuty(duty) => {
        println(s"Reasoner received duty: ${duty}")
        val d = parseDuty(duty)
        d match {
          case Some(d) => msg.replyTo ! InformDuty(d)
          case None    => {}
        }
        Behaviors.same
      }
    }
  }

  def parseDuty(duty: norms.Duty)(implicit
      resolver: ActorRefResolver
  ): Option[Duty] = {
    duty match {
      case norms.DutyValue(holder, claimant, value) => {
        (holder, claimant) match {
          case (Left(holder), Left(claimant)) => {
            val d = Duty(
              name = value.fact_type,
              holder = Some(resolver.resolveActorRef[Message](holder)),
              None,
              claimant = Some(resolver.resolveActorRef[Message](claimant)),
              None,
              relatedTo = List() // TODO: parse relatedTo
            )
            Some(d)
          }
          case _ => {
            println("Reasoner received duty with actor refs")
            None
          }
        }
      }
      case _ => {
        println("Reasoner received duty with unexpected value")
        None
      }
    }
  }

  def getInformationProp(
      msg: Message,
      resp: norms.DecisionInputRequired
  )(implicit
      resolver: ActorRefResolver
  ): Option[Proposition]
}
