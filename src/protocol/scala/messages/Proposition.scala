package protocol

trait PVal {
  def getValue(): Any = this match {
    case PString(value)                     => value
    case PInt(value)                        => value
    case Proposition(name, instance, state) => null
  }
}

final case class PString(value: String) extends PVal
final case class PInt(value: Int) extends PVal

case class Proposition(
    identifier: String,
    instance: List[PVal],
    state: TruthValue = True
) extends PVal
