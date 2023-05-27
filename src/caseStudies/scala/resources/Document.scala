package caseStudies

import protocol._

class Document(val id: String, val fileName: String, val content: String) {
  def getPredicate(): Predicate = Predicate("document", List(PString(id)))
}
