package autojson.core

import java.io.File

import autojson.core.example2.{BrickLayer, Building, ConstructionSite, Engineer, Inspector, Room}
import autojson.core.example1.{TestClass, TestInterface}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class JsonCodeGeneratorTest extends AnyFlatSpec with Matchers {

  "JsonCodeGenerator" should "generate valid scala serializer upon analyzing TestClass.java" in {
    val x: TestInterface = new TestClass(1, 2)
    val path = "src/main/java/autojson/core/example1/TestClass.java"
    val serializerPath = "src/main/scala/autojson/core/serializers"
    // path to a file containing names of all existing serializers
    val (codeString, methodName) = JsonCodeGenerator.generateSerializationCode(x, path, serializerPath)
    JsonCodeGenerator.saveSerializerCode(serializerPath, codeString, methodName, "autojson.core.serializers")

    val gtString =
      """import org.json4s.DefaultFormats
        |import org.json4s.native.Json
        |import scala.jdk.CollectionConverters._
        |import autojson.core.example1.TestClass
        |
        |object TestClassSerializer{
        |  def toMap(obj: Any): Map[String, Any] = {
        |    val castedObj = obj.asInstanceOf[TestClass]
        |    Map("a"->castedObj.a,"b"->castedObj.b,"c"->
        |      castedObj.c.asScala.toList.map{ case (key, value) =>
        |        Map("key" -> key, "value" -> value)
        |      },"d"->
        |      castedObj.d.asScala.toList.map{ data =>
        |        data
        |      },"e"->TestClass2Serializer.toMap(castedObj.e),"className" -> "TestClass"
        |    )
        |  }
        |  def toJson(obj : Any): String = {
        |    val map = toMap(obj)
        |    Json(DefaultFormats).write(map)
        |  }
        |}""".stripMargin
    codeString shouldEqual gtString
    methodName shouldEqual "TestClass"
    for (name <- List("TestClass", "TestClass2")) {
      val file = new File(serializerPath + s"/${name}Serializer.scala")
      assert(file.exists())
      file.delete()
      assert(!file.exists())
    }
  }

  "JsonCodeGenerator" should "generate valid scala serializer upon analyzing ConstructionSite.java" in {
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
    val path = "src/main/java/autojson/core/example2/ConstructionSite.java"
    val serializerPath = "src/main/scala/autojson/core/serializers"
    val (codeString, methodName) = JsonCodeGenerator.generateSerializationCode(cs, path, serializerPath)
    JsonCodeGenerator.saveSerializerCode(serializerPath, codeString, methodName, "autojson.core.serializers")
    for (name <- List("BrickLayer", "Building", "ConstructionSite", "Engineer", "Inspector", "Room", "Worker")) {
      val file = new File(serializerPath + s"/${name}Serializer.scala")
      assert(file.exists())
      file.delete()
      assert(!file.exists())
    }
  }

}
