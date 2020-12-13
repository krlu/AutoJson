package autojson.core

import java.lang.reflect.Modifier
import java.util

import autojson.core.Utils._
import org.json4s.DefaultFormats
import org.json4s.native.Json

import scala.jdk.CollectionConverters._

object AutoSerializer {

  // Note, when converting a map structure to a JSON string, the order in which the key/value pairs appear may vary
  // The variation depends on the implicit Map ordering
  def toJson(inputObject: Object, prettyPrint: Boolean = false): String = {
    val map = toMap(inputObject)
    if(prettyPrint)
      Json(DefaultFormats).writePretty(map)
    else
      Json(DefaultFormats).write(map)
  }

  private def toMap(inputObject: Object): Map[String, Object] = {
    val cls = inputObject.getClass
    val isScala = cls.getAnnotations.exists(annotation => annotation.toString.contains("ScalaSignature"))
    val fieldsMap: Map[String, Object] = cls.getFields.toList.map{ field =>
      val fieldValue: Object = field.get(inputObject)
      parseMember(field.getName, fieldValue)
    }.toMap
    val methodsMap: Map[String, Object] =
      if(!isScala) Map.empty[String, Object]
      else
        cls.getDeclaredMethods.toList
        .filter(method => Modifier.isPublic(method.getModifiers) && method.getParameterCount == 0)
        .map(method => parseMember(method.getName, method.invoke(inputObject, List(): _*))).toMap
    fieldsMap ++ methodsMap ++ Map("className" -> cls.getSimpleName)
  }

  private def parseMember(memberName: String, memberValue: Object): (String, Object) = {
    val value =
      if(isPrimitive(memberValue)) memberValue
      else if(recognize[Iterable[Object]](memberValue)){
        val collection = memberValue.asInstanceOf[Iterable[Object]]
        collection.toList.map(element => toMap(element))
      }
      else if(recognize[util.Collection[Object]](memberValue)){
        val collection = memberValue.asInstanceOf[util.Collection[Object]]
        collection.asScala.toList.map(element => toMap(element))
      }
      else if(recognize[Map[Object, Object]](memberValue)){
        val map = memberValue.asInstanceOf[Map[Object, Object]]
        createMapOfMap(map)
      }
      else if(recognize[util.Map[Object, Object]](memberValue)){
        val map = memberValue.asInstanceOf[util.Map[Object, Object]].asScala.toMap
        createMapOfMap(map)
      }
      else toMap(memberValue)
    (memberName, value)
  }

  // converts Map(k -> v) to Map("key" -> k, "value" -> v)
  private def createMapOfMap(map: Map[Object, Object]): Seq[Map[String, Object]] = {
    map.toList.map{ case (key, value) =>
      val keyValue = if(isPrimitive(key)) key else toMap(key)
      val valValue = if(isPrimitive(value)) value else toMap(value)
      Map("key" -> keyValue, "value" -> valValue)
    }
  }
}
