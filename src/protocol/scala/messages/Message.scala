package protocol

import akka.actor.typed.ActorRef

trait Message {}
trait QueryMessage extends Message {}

final case class Inform(proposition: Proposition) extends Message
final case class InformAct(act: Act) extends Message
final case class InformEvent(event: Event) extends Message
final case class InformDuty(duty: Duty) extends Message

final case class InformDutyTerminated(duty: Duty) extends Message
final case class InformViolatedDuty(duty: Duty) extends Message
final case class InformViolatedAct(act: Act) extends Message
final case class InformViolatedInvariant(proposition: Proposition) extends Message

final case class Request(proposition: Proposition, replyTo: ActorRef[Message]) extends QueryMessage
final case class RequestAct(act: Act, replyTo: ActorRef[Message]) extends QueryMessage
final case class RequestDuty(duty: Duty, replyTo: ActorRef[Message]) extends QueryMessage

final case class Permit(act: Act) extends Message
final case class Permitted(act: Act) extends Message
final case class Forbid(act: Act) extends Message
final case class Forbidden(act: Act) extends Message

final case class Reject(obj: Either[Act, Duty]) extends Message
