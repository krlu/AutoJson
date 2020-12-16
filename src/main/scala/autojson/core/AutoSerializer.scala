package autojson.core

import java.lang.reflect.Modifier
import java.util

import autojson.core.Utils._
import org.json4s.DefaultFormats
import org.json4s.native.{Json, JsonMethods}

import scala.jdk.CollectionConverters._

object AutoSerializer {

  // Note, when converting a map structure to a JSON string, the order in which the key/value pairs appear may vary
  // The variation depends on the implicit Map ordering
  def toJson(inputObject: Object, prettyPrint: Boolean = false) : String = {
    val rawStrOpt = inputObject match {
      case arr: Array[_] =>
        val arrStr = arr.toList.map(x => toJsonHelper(x.asInstanceOf[Object])).mkString(",")
        Some(s"""{"elements": [$arrStr], "className":"${inputObject.getClass.getCanonicalName}"}""")
      case map: Map[_,_] =>
        Some(parseMap(map.asInstanceOf[Map[Object, Object]], map.getClass))
      case map: java.util.Map[_, _] =>
        Some(parseMap(map.asScala.toMap.asInstanceOf[Map[Object, Object]], map.getClass))
      case iterable: Iterable[_] =>
        val iterableStr = iterable.toList.map(x => toJsonHelper(x.asInstanceOf[Object])).mkString(",")
        val iterableTypeName = iterable.getClass.getCanonicalName
        val finalTypeName =
          if(iterableTypeName.contains("$colon$colon")) s"${iterable.getClass.getPackage.getName}.List"
          else iterableTypeName
        Some(s"""{"elements": [$iterableStr], "className":"$finalTypeName"}""")
      case coll: util.Collection[_] =>
        val collectionStr = coll.asScala.toList.map(x => toJsonHelper(x.asInstanceOf[Object])).mkString(",")
        Some(s"""{"elements": [$collectionStr], "className":"${coll.getClass.getCanonicalName}"}""")
      case _ => None
    }
    val parsedStr = rawStrOpt match {
      case Some(rawStr) => JsonMethods.parse(rawStr)
      case None => toMap(inputObject)
    }
    if(prettyPrint)
      Json(DefaultFormats).writePretty(parsedStr)
    else
      Json(DefaultFormats).write(parsedStr)
  }

  private def parseMap(map: Map[Object, Object], cls: Class[_]): String = {
    val listOfMaps = map.map{ case (k ,v) =>
      s"""{"key":${toJsonHelper(k)}, "value":${toJsonHelper(v)}}"""
    }
    s"""{"elements":[${listOfMaps.toList.mkString(",")}],"className":"${cls.getPackage.getName}.${cls.getSimpleName}"}"""
  }

  private def toJsonHelper(inputObject: Object, prettyPrint: Boolean = false): String = {
    if(inputObject.isInstanceOf[String])
      return s""""$inputObject""""
    if(isPrimitive(inputObject))
      return inputObject.toString
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
      else {
        cls.getDeclaredMethods.toList
        .filter(method => Modifier.isPublic(method.getModifiers) && method.getParameterCount == 0)
        .map(method => parseMember(method.getName, method.invoke(inputObject, List(): _*))).toMap
      }
    fieldsMap ++ methodsMap ++ Map("className" -> s"${cls.getCanonicalName}")
  }

  private def parseMember(memberName: String, memberValue: Object): (String, Object) = {
    val value =
      if(isPrimitive(memberValue))
        memberValue
      else if(memberValue.isInstanceOf[Map[_,_]]){
        val map = memberValue.asInstanceOf[Map[Object, Object]]
        createMapOfMap(map)
      }
      else if(recognize[Iterable[Object]](memberValue)){
        val collection = memberValue.asInstanceOf[Iterable[Object]]
        collection.toList.map(element => toMap(element))
      }
      else if(recognize[util.Collection[Object]](memberValue)){
        val collection = memberValue.asInstanceOf[util.Collection[Object]]
        collection.asScala.toList.map(element => toMap(element))
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
