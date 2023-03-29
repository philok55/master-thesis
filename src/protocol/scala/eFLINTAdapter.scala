package protocol

object EflintAdapter {
  def apply(message: Message): String = {
    message match {
      case Inform(predicate)  => predicateToEflint(predicate)
      case InformAct(act)     => actToEflint(act)
      case InformEvent(event) => eventToEflint(event)
      case Request(predicate, replyTo) =>
        predicateToEflint(predicate, request = true)
      case RequestAct(act, replyTo) => actToEflint(act, request = true)
      case _                        => "invalid"
    }
  }

  def predicateToEflint(
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
              predicateToEflint(Predicate(name, instance, state), nested = true)
          }
          .mkString(", ")

        if (nested)
          s"$name($instanceString)"
        else if (request)
          s"?$name($instanceString)."
        else
          s"$modifier$name($instanceString)."
      }
    }
  }

  def actToEflint(act: Act, request: Boolean = false): String = {
    act match {
      case Act(name, actor, recipient, related_to, _) => {
        val actorString = predicateToEflint(actor, nested = true)
        val relatedToString = related_to
          .map(predicateToEflint(_, nested = true))
          .mkString(", ")
        val result =
          if (recipient == null) {
            if (relatedToString == "") {
              s"$name($actorString)"
            } else {
              s"$name($actorString, $relatedToString)"
            }
          } else {
            val recipientString = predicateToEflint(recipient, nested = true)
            if (relatedToString == "") {
              s"$name($actorString, $recipientString)"
            } else {
              s"$name($actorString, $recipientString, $relatedToString)"
            }
          }
        if (request) s"?Enabled($result)." else s"$result."
      }
    }
  }

  def eventToEflint(event: Event): String = {
    s"${event.name}()."
  }
}
