package protocol

object EflintAdapter {
  def apply(message: Message): String = {
    message match {
      case Inform(proposition) => propositionToEflint(proposition)
      case InformAct(act)      => actToEflint(act)
      case InformEvent(event)  => eventToEflint(event)
      case Request(proposition, replyTo) =>
        propositionToEflint(proposition, request = true)
      case RequestAct(act, replyTo) => actToEflint(act, request = true)
      case _ =>
        println("ERROR: eFLINTAdapter called with invalid message")
        ""
    }
  }

  def propositionToEflint(
      proposition: Proposition,
      nested: Boolean = false,
      request: Boolean = false
  ): String = {
    proposition match {
      case Proposition(name, instance, state) => {
        val modifier = state match {
          case True    => "+"
          case False   => "-"
          case Unknown => "~"
        }
        val instanceString = instance
          .map {
            case PString(value) => s""""$value""""
            case PInt(value)    => value.toString
            case Proposition(name, instance, state) =>
              propositionToEflint(
                Proposition(name, instance, state),
                nested = true
              )
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
      case Act(name, _, pActor, _, pRecipient, related_to, _) => {
        val actorString = propositionToEflint(pActor, nested = true)
        val relatedToString = related_to
          .map(propositionToEflint(_, nested = true))
          .mkString(", ")
        val result =
          if (pRecipient == null) {
            if (relatedToString == "") {
              s"$name($actorString)"
            } else {
              s"$name($actorString, $relatedToString)"
            }
          } else {
            val recipientString = propositionToEflint(pRecipient, nested = true)
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
