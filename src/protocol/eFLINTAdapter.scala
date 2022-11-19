package protocol

object EFLINTAdapter {
  def apply(message: Message): String = {
    message match {
      case Inform(predicate) => predicateToEFLINT(predicate)
      case _                 => "invalid"
    }
  }

  def predicateToEFLINT(predicate: Predicate): String = {
    predicate match {
      case Predicate(value, typeName, instance) => {
        val modifier = value match {
          case True    => "+"
          case False   => "-"
          case Unknown => "~"
        }
        s"$modifier$typeName($instance)"
      }
      case _ => "invalid"
    }
  }
}
