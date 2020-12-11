package autojson.core

import java.lang.reflect.{Modifier, ParameterizedType}
import java.util

import autojson.core.Utils._
import org.json4s.DefaultFormats
import org.json4s.native.Json

import scala.jdk.CollectionConverters._

object AutoSerializer {
  def mapToObject[T](map: Map[String, Any], classOf: Class[T], packageName: String): T = {
    val fields = classOf.getFields.toList
    val params = fields.map(field => field.getType)
    val isAbstract = Modifier.isAbstract(classOf.asInstanceOf[Class[_]].getModifiers)
    val isInterface = classOf.isInterface
    if(isAbstract || isInterface){
      val subType = Class.forName(s"${packageName}.${map("className").toString}").asInstanceOf[Class[_ <: T]]
      return mapToObject(map, subType, packageName)
    }
    val arguments = fields.map{field =>
      val value = map(field.getName)
      val fieldType = field.getType
      if(isPrimitive(fieldType.getSimpleName)) value
      else if(fieldType.getInterfaces.map(_.getSimpleName).contains("Collection")) {
        val typeParam = field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments.head
        val typeParamClass = typeParam.asInstanceOf[Class[_]]

        val typeParamName = typeParam.getTypeName.split("\\.").last
        val collection = value.asInstanceOf[List[Any]].map { element =>
          if(isPrimitive(typeParamName)){
            element
          } else {
            mapToObject(element.asInstanceOf[Map[String, Any]], typeParamClass, packageName)
          }
        }
        if(fieldType.getSimpleName.contains("Set"))
          collection.toSet.asJava
        else if(fieldType.getSimpleName.contains("List"))
          collection.asJava
        else collection
      }
      else{
        throw new IllegalStateException(s"Unable to parse field ${field.getName} of type ${fieldType.getSimpleName}!")
      }
    }
    classOf.getDeclaredConstructor(params: _*).newInstance(arguments: _*)
  }

  def isPrimitive(inputString: String): Boolean =
    List("int", "double", "integer", "float", "string", "long").contains(inputString.toLowerCase())

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