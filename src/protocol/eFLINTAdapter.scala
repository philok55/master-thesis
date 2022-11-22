package protocol

object EFLINTAdapter {
  def apply(message: Message): String = {
    message match {
      case Inform(predicate)  => predicateToEFLINT(predicate)
      case InformAct(act)     => actToEFLINT(act)
      case InformEvent(event) => eventToEFLINT(event)
      case _                  => "invalid"
    }
  }

  def predicateToEFLINT(predicate: Predicate): String = {
    predicate match {
      case Predicate(state, name, instance) => {
        val modifier = state match {
          case True    => "+"
          case False   => "-"
          case Unknown => "~"
        }
        val instanceString = instance.map {
          case PredicateString(value) => value
          case PredicateInt(value)    => value.toString
          case Predicate(state, name, instance) =>
            predicateToEFLINT(Predicate(state, name, instance))
        }.mkString(", ")
        s"$modifier$name($instanceString)."
      }
      case _ => "invalid"
    }
  }

  def actToEFLINT(act: Act): String = {
    act match {
      case Act(name, actor, recipient, _) => s"$name($actor, $recipient)."
      case _                           => "invalid"
    }
  }

  def eventToEFLINT(event: Event): String = {
    event match {
      case Event(name, _) => s"$name()."
      case _           => "invalid"
    }
  }
}
