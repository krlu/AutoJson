package autojson.core

import java.io.File

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonCodeGeneratorTest extends AnyFlatSpec with Matchers{
  "JsonCodeGenerator" should "generator valid scala serializer upon analyzing java data structure" in {
    val x: TestInterface = new TestClass(1, 2)
    val path = "src/main/java/autojson/core/TestClass.java"
    val serializerPath = "src/main/scala/autojson/core/serializers"
    // path to a file containing names of all existing serializers
    val (codeString, methodName)= JsonCodeGenerator.generateSerializationCode(x, path, serializerPath)
    JsonCodeGenerator.saveSerializerCode(serializerPath, codeString, methodName, "autojson.core.serializers")

    val gtString =
    """import org.json4s.DefaultFormats
      |import org.json4s.native.Json
      |import scala.jdk.CollectionConverters._
      |import autojson.core.TestClass
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
      |      },"e"->TestClass2Serializer.toMap(castedObj.e), "className" -> "TestClass"
      |    )
      |  }
      |  def toJson(obj : Any): String = {
      |    val map = toMap(obj)
      |    Json(DefaultFormats).write(map)
      |  }
      |}""".stripMargin
    codeString shouldEqual gtString
    methodName shouldEqual "TestClass"
    for(name <- List("TestClass", "TestClass2")){
      val file = new File(serializerPath + s"/${name}Serializer.scala")
      assert(file.exists())
//      file.delete()
//      assert(!file.exists())
    }
  }
}
