package protocol

object EFLINTAdapter {
  def apply(message: Message): String = {
    message match {
      case Inform(predicate)  => predicateToEFLINT(predicate)
      case InformAct(act)     => actToEFLINT(act)
      case InformEvent(event) => eventToEFLINT(event)
      case Request(predicate) => predicateToEFLINT(predicate, request = true)
      case RequestAct(act)    => actToEFLINT(act, request = true)
      case _                  => "invalid"
    }
  }

  def predicateToEFLINT(
      predicate: Predicate,
      nested: Boolean = false,
      request: Boolean = false
  ): String = {
    predicate match {
      case Predicate(name, instance, state) => {
        val modifier = state match {
          case True    => "+"
          case False   => "-"
          case Unknown => "~"
        }
        val instanceString = instance
          .map {
            case PString(value) => s""""$value""""
            case PInt(value)    => value.toString
            case Predicate(name, instance, state) =>
              predicateToEFLINT(Predicate(name, instance, state), nested = true)
          }
          .mkString(", ")

        if (nested)
          s"$name($instanceString)"
        else if (request)
          s"?$name($instanceString)."
        else
          s"$modifier$name($instanceString)."
      }
      case _ => "invalid"
    }
  }

  def actToEFLINT(act: Act, request: Boolean = false): String = {
    act match {
      case Act(name, actor, recipient, related_to, _) => {
        val relatedToString = related_to
          .map(predicateToEFLINT(_, nested = true))
          .mkString(", ")
        val result = if (relatedToString == "") {
          s"$name($actor, $recipient)"
        } else {
          s"$name($actor, $recipient, $relatedToString)"
        }
        if (request) s"?Enabled($result)." else result
      }
      case _ => "invalid"
    }
  }

  def eventToEFLINT(event: Event): String = {
    event match {
      case Event(name, _) => s"$name()."
      case _              => "invalid"
    }
  }
}
