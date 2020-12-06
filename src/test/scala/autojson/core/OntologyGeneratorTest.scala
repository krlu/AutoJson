package autojson.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OntologyGeneratorTest extends AnyFlatSpec with Matchers{
  "Ontology Generator" should "create ontology from java files" in {
    val x = new TestClass(1, 2)
    val path = "src/main/java/autojson/core/TestClass.java"
    val ontology = OntologyGenerator.buildOntology(path)
    ontology shouldEqual Set(("TestInterface","TestClass"), ("TestSuperClass", "TestClass"))
  }
}
