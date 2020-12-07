package autojson.core

import autojson.core.example1.TestClass
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OntologyGeneratorTest extends AnyFlatSpec with Matchers{
  "Ontology Generator" should "create ontology from java files" in {
    val path1 = "src/main/java/autojson/core/example1/"
    val path2 = "src/main/java/autojson/core/example2/"
    val onto1 = OntologyGenerator.buildOntology(path1)
    val onto2 = OntologyGenerator.buildOntology(path2)
    onto1 shouldEqual Set(("TestInterface","TestClass"), ("TestSuperClass", "TestClass"))
    onto2 shouldEqual Set(("Worker","BrickLayer"), ("Worker","Engineer"), ("Worker","Inspector"))
  }
}
