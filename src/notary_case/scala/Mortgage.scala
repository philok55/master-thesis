package notary_case

import akka.actor.typed.ActorRef

class Mortgage(val property: Property, val citizen: ActorRef[norms.Message], val value: Int)
