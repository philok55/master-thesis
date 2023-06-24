# Master thesis project: a communication protocol for normative ator systems
This is the implementation repository for my Master Thesis, the graduation project for the MSc. Software Engineering at the University of Amsterdam.

It is a Proof-of-Concept implementation of the protocol design I created for usage within normative actor systems. The generic module for the protocol is in `src/protocol`, and `src/caseStudies` contains a collection of case studies using the protocol in the context of a hypothetical real estate management application.


## Setup
This project has 3 external dependencies: [eFLINT](https://gitlab.com/eflint/haskell-implementation) (tested with version 3.1.0.1), the [eFLINT java-server project](https://gitlab.com/eflint/eflint-actors/java-implementation) (tested with version 0.1.11), and the [eFLINT Akka implementation in Scala](https://gitlab.com/eflint/eflint-actors/scala-implementation) (tested with version 0.1.10).

Install `Java`, `sbt` and `Cabal`, follow the install instructions for each project, and then run `sbt run`.
