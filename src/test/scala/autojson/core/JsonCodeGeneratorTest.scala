package autojson.core

import java.io.File

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonCodeGeneratorTest extends AnyFlatSpec with Matchers{
  "JsonCodeGenerator" should "generator valid scala serializer upon analyzing java data structure" in {
    val x = new TestClass(1, 2)
    val path = "src/main/java/autojson/core/TestClass.java"
    val serializerPath = "src/main/scala/autojson/core"
    // path to a file containing names of all existing serializers
    val (codeString, methodName)= JsonCodeGenerator.generateSerializationCode(x, path)
    JsonCodeGenerator.saveSerializerCode(serializerPath, codeString, methodName, "autojson.core")

    val gtString =
    """import org.json4s.DefaultFormats
      |import org.json4s.native.Json
      |import scala.jdk.CollectionConverters._
      |
      |object TestClassSerializer{
      |  def toJson(obj: Any): String = {
      |    val castedObj = obj.asInstanceOf[TestClass]
      |    val map = Map("a"->castedObj.a,"b"->castedObj.b,"c"->
      |      castedObj.c.asScala.toList.map{ case (key, value) =>
      |        Map("key" -> key, "value" -> value)
      |      })
      |    Json(DefaultFormats).write(map)
      |  }
      |}""".stripMargin
    codeString shouldEqual gtString
    methodName shouldEqual "TestClass"
    val file = new File(serializerPath + s"/${methodName}Serializer.scala")
    assert(file.exists())
    file.delete()
    assert(!file.exists())
  }
}
