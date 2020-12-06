package autojson.core

import autojson.core.JsonCodeGenerator.generateSerializationCode
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonCodeGeneratorTest extends AnyFlatSpec with Matchers{
  "JsonCodeGenerator" should "generator valid serializer upon analyzing java data structure" in {
    val x = new TestClass(1, 2)
    val path = "src/main/java/autojson/core/TestClass.java"
    val serializerIndices = "src/main/java"
    // path to a file containing names of all existing serializers
    val codeString: String = generateSerializationCode(x, path, serializerIndices)

    val gtString =
    """def mapFieldsForTestClass(obj: Any): String = {
      |  val castedObj = obj.asInstanceOf[TestClass]
      |  val map = Map("a"->castedObj.a,"b"->castedObj.b,"c"->
      |    castedObj.c.asScala.toList.map{ case (key, value) =>
      |      Map("key" -> key, "value" -> value)
      |    }
      |  )
      |  Json(DefaultFormats).write(map)
      |}""".stripMargin
    codeString shouldEqual gtString
  }
}
