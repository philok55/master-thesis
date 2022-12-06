package protocol

trait Message {}
trait QueryMessage extends Message {}

final case class Inform(predicate: Predicate) extends Message
final case class InformAct(act: Act) extends Message
final case class InformEvent(event: Event) extends Message
final case class InformDuty(duty: Duty) extends Message

final case class InformDutyTerminated(duty: Duty) extends Message
final case class InformViolatedDuty(duty: Duty) extends Message
final case class InformViolatedAct(act: Act) extends Message
final case class InformViolatedInvariant(predicate: Predicate) extends Message

final case class Request(predicate: Predicate) extends QueryMessage
final case class RequestAct(act: Act) extends QueryMessage

final case class Permitted(act: Act) extends Message
final case class Forbidden(act: Act) extends Message
