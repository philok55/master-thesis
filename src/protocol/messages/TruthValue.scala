package protocol

trait TruthValue {}
final case object True extends TruthValue
final case object False extends TruthValue
final case object Unknown extends TruthValue

