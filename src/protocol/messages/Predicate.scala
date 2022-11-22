package protocol

trait PredicateValue {}

final case class Predicate(state: TruthValue, name: String, instance: List[PredicateValue]) extends PredicateValue

final case class PredicateString(value: String) extends PredicateValue
final case class PredicateInt(value: Int) extends PredicateValue

