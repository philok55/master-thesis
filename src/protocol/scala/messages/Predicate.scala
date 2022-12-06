package protocol

trait PVal {}

final case class Predicate(name: String, instance: List[PVal], state: TruthValue = True) extends PVal

final case class PString(value: String) extends PVal
final case class PInt(value: Int) extends PVal

