package caseStudies

import akka.actor.typed.ActorRefResolver
import util._ // from eflint.java-server
import protocol._

object Reasoner extends ReasonerActor {
  override def getInformationProp(
      msg: Message,
      resp: norms.DecisionInputRequired
  )(implicit
      resolver: ActorRefResolver
  ): Option[Proposition] = {
    val input = resp.inputs(0)
    input match {
      case i: RequiredInstances => {
        i.`type` match {
          case "yearly-income" => {
            msg match {
              case InformAct(a: IndexAgreement) => {
                val tenant = a.current.agreement.tenantAddress
                Some(new PIncome(new PTenant(tenant), 0))
              }
              case _ => None
            }
          }
          case _ => None
        }
      }
      case _ => None
    }
  }
}
