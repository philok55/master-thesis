package protocol

trait Message {}
final case class Inform(predicate: Predicate) extends Message

