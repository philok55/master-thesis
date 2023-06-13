package caseStudies

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._
import scala.collection.Map

import protocol._

object TaxAuthority extends InformationActor {
  def getKey(message: Request): String = {
    val prop = message.proposition
    prop match {
      case p: PIncome => s"income-${p.tenant.name}"
      case _ => ""
    }
  }

  def getProp(prop: Proposition, value: Any): Proposition = {
    prop match {
      case p: PIncome => new PIncome(p.tenant, value.asInstanceOf[Int])
      case _ => prop
    }
  }
}