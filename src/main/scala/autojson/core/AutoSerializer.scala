package autojson.core

import java.lang.reflect.ParameterizedType
import java.util

import autojson.core.Utils._
import autojson.core.example2.{BrickLayer, Building, ConstructionSite, Engineer, Inspector, Room}
import org.json4s.DefaultFormats
import org.json4s.native.Json

import scala.jdk.CollectionConverters._

object AutoSerializer {
  def main(args: Array[String]): Unit = {
    val bl = new BrickLayer("Brandie", "asdf", 100)
    val in = new Inspector("Ivan", "qwer", 100)
    val en = new Engineer("Eugenia", "uiop", 100)
    val b1r1 = new Room()
    val b1r2 = new Room()
    val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
    val b2r1 = new Room()
    val b2r2 = new Room()
    val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
    val buildings = Set(build1, build2).asJava
    val workers = Set(bl, in, en).asJava
    val cs = new ConstructionSite(workers, buildings)
    val map = toMap(cs)
    val x = mapToObject(map, classOf[ConstructionSite])
  }

  def mapToObject[T](map: Map[String, Any], classOf: Class[T]): T = {
    val fields = classOf.getFields.toList
    val params = fields.map(field => field.getType)
    val arguments = fields.map{field =>
      val value = map(field.getName)
      val fieldType = field.getType
      if(isPrimitive(fieldType.getSimpleName)) value
      else if(fieldType.getInterfaces.map(_.getSimpleName).contains("Collection")) {
        val typeParam = field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments.head
        val typeParamName = typeParam.getTypeName.split("\\.").last
        val collection = value.asInstanceOf[List[Any]].map { element =>
          if(isPrimitive(typeParamName)){
            element
          } else {
            mapToObject(element.asInstanceOf[Map[String, Any]], typeParam.asInstanceOf[Class[_]])
          }
        }
        if(fieldType.getSimpleName.contains("Set"))
          collection.toSet.asJava
        else if(fieldType.getSimpleName.contains("List"))
          collection.asJava
        else collection
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
