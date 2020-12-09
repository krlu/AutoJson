package autojson.core

import java.util

import org.json4s.DefaultFormats
import org.json4s.native.Json
import autojson.core.Utils._

import scala.jdk.CollectionConverters._

object AutoSerializer {

  def toJson(inputObject: Object): String = {
    val map = toMap(inputObject)
    Json(DefaultFormats).write(map)
  }
  def toMap(inputObject: Object): Map[String, Object] = {
    val cls = inputObject.getClass
    val fieldsMap = cls.getFields.toList.map{ field =>
      val fieldValue: Object = field.get(inputObject)
      val value =
        if(isPrimitive(fieldValue)) fieldValue
        else if(recognize[util.Collection[Object]](fieldValue)){
          val collection = fieldValue.asInstanceOf[util.Collection[Object]]
          collection.asScala.toList.map(element => toMap(element))
        }
        else if(recognize[util.Map[Object, Object]](fieldValue)){
          val map = fieldValue.asInstanceOf[util.Map[Object, Object]]
          map.asScala.toList.map{ case (key, value) =>
            val keyValue = if(isPrimitive(key)) key else toMap(key)
            val valValue = if(isPrimitive(value)) value else toMap(value)
            Map("key" -> keyValue, "value" -> valValue)
          }
        }
        else toMap(fieldValue)
      (field.getName, value)
    }.toMap
    fieldsMap ++ Map("className" -> cls.getSimpleName)
  }

  private def isPrimitive(obj: Object): Boolean ={
    obj match {
      case _: java.lang.Integer => true
      case _: java.lang.String => true
      case _: java.lang.Double => true
      case _ => false
    }
  }
}
