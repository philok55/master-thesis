package caseStudies

import protocol._
import scala.collection.Set


object Reasoner extends ReasonerActor {
    override def blockedActions = Set("access-document")
}
