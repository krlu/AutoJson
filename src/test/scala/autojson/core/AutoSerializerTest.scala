package autojson.core

import autojson.core.example1.{TestClass, TestInterface}
import autojson.core.example2._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._


class AutoSerializerTest extends AnyFlatSpec with Matchers{
  "AutoSerializer" should "Serialize a construction site object" in {
    val bl = new BrickLayer("Brandie", "asdf", 100)
    val in = new Inspector("Ivan", "qwer", 100)
    val en = new Engineer("Eugenia", "uiop", 100)
    val b1r1 = new Room();
    val b1r2 = new Room();
    val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
    val b2r1 = new Room();
    val b2r2 = new Room();
    val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
    val buildings = Set(build1, build2).asJava
    val workers = Set(bl, in, en).asJava
    val cs = new ConstructionSite(workers, buildings)
    val csJsonString = AutoSerializer.toJson(cs)
    val gtString = "{\"workers\":[{\"name\":\"Brandie\",\"id\":\"asdf\",\"age\":100,\"className\":\"BrickLayer\"},{\"name\":\"Ivan\",\"id\":\"qwer\",\"age\":100,\"className\":\"Inspector\"},{\"name\":\"Eugenia\",\"id\":\"uiop\",\"age\":100,\"className\":\"Engineer\"}],\"buildings\":[{\"id\":\"b1\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"},{\"id\":\"b2\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"}],\"className\":\"ConstructionSite\"}"
    csJsonString shouldEqual gtString
  }

  "AutoSerializer" should "Serialize a test class object" in {
    val testInt: TestInterface = new TestClass(1, 2)
    val jsonString = AutoSerializer.toJson(testInt)
    val gtString = "{\"e\":{\"a\":0,\"b\":\"b\",\"className\":\"TestClass2\"},\"a\":1,\"b\":2,\"className\":\"TestClass\",\"c\":[{\"key\":1,\"value\":\"1\"},{\"key\":2,\"value\":\"2\"}],\"d\":[]}"
    jsonString shouldEqual gtString
  }

}
