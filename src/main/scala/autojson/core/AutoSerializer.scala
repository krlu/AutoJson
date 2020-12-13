package autojson.core

import java.lang.reflect.{Modifier, ParameterizedType, Type}
import java.util

import autojson.core.Utils._
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json

import scala.jdk.CollectionConverters._

object AutoSerializer {

  def jsonToObject[T](jsonString: String, classOf: Class[T], packageName: String): T = {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    val map = parse(jsonString).extract[Map[String, Any]]
    mapToObject(convert(map), classOf, packageName)
  }

  private def convert(map: Map[String, Any]): Map[String, Any] = {
    map.map{ case (k,v) =>
      val value = if(recognize[BigInt](v)){
        v.asInstanceOf[BigInt].toInt
      }
      else if(recognize[Seq[_]](v)){
        v.asInstanceOf[Seq[_]].map{ element =>
          if(recognize[BigInt](element))
            element.asInstanceOf[BigInt].toInt
          else if(recognize[Map[String, Any]](element))
            convert(element.asInstanceOf[Map[String, Any]])
          else element
        }
      }
      else if(recognize[Map[String, Any]](v)){
        convert(v.asInstanceOf[Map[String, Any]])
      }
      else v
      k -> value
    }
  }

  def mapToObject[T](map: Map[String, Any], classOf: Class[T], packageName: String): T = {
    val fields = classOf.getFields.toList
    val methods = classOf.getDeclaredMethods.toList.filter(method => Modifier.isPublic(method.getModifiers) && method.getParameterCount == 0)
    val isAbstract = Modifier.isAbstract(classOf.asInstanceOf[Class[_]].getModifiers)
    val isInterface = classOf.isInterface
    if(isAbstract || isInterface){
      val subType = Class.forName(s"$packageName.${map("className").toString}").asInstanceOf[Class[_ <: T]]
      return mapToObject(map, subType, packageName)
    }
    val methodArgs = methods.map{ method =>
      val name = method.getName
      val value = map(method.getName).asInstanceOf[Object]
      val returnType = method.getReturnType
      val typeParam = method.getGenericReturnType
      name -> parseArgs(value, returnType, typeParam, name, packageName)
    }
    val arguments = fields.map{field =>
      val name = field.getName
      val value = map(field.getName).asInstanceOf[Object]
      val fieldType = field.getType
      name -> parseArgs(value, fieldType, field.getGenericType, name, packageName)
    } ++ methodArgs
    val argMap = arguments.toMap
    val constructor = classOf.getConstructors.toList.head
    val constructorParams = constructor.getParameters
    if(constructorParams.forall(cp => argMap.contains(cp.getName))) {
      val orderedArgs = classOf.getConstructors.toList.head.getParameters.map { param =>
        argMap(param.getName)
      }
      constructor.newInstance(orderedArgs: _*).asInstanceOf[T]
    }else constructor.newInstance(arguments.map(_._2):_*).asInstanceOf[T]
  }

  def parseArgs(value: Object, valueType: Class[_], genericType: Type, valueName: String, packageName: String): Object = {
    val interfaces = valueType.getInterfaces.map(_.getSimpleName)
    if(isPrimitive(valueType.getSimpleName)) value
    else if(interfaces.contains("Collection") || interfaces.contains("Iterable")) {
      val typeParam = genericType.asInstanceOf[ParameterizedType].getActualTypeArguments.head
      val typeParamClass = typeParam.asInstanceOf[Class[_]]
      val typeParamName = typeParam.getTypeName.split("\\.").last
      val collection = value.asInstanceOf[List[Any]].map { element =>
        if(isPrimitive(typeParamName)){
          element
        } else {
          mapToObject(element.asInstanceOf[Map[String, Any]], typeParamClass, packageName)
        }
      }
      if(valueType.getSimpleName.contains("Set")) {
        if(interfaces.contains("Iterable")) collection.toSet
        else collection.toSet.asJava
      } else if(valueType.getSimpleName.contains("List"))
        if(interfaces.contains("Iterable")) collection.toSet
        else collection.toSet.asJava
      else if(valueType.getSimpleName.contains("Map"))
        throw new IllegalArgumentException(s"AutoJson does not yet support serialization for Map types like: ${valueType.getName}.")
      else
        throw new IllegalArgumentException(s"Could not deserialize iterable/collection structure of ${valueType.getName}")
    }
    else{
      throw new IllegalStateException(s"Unable to parse field $valueName of type ${valueType.getSimpleName}!")
    }
  }


  def isPrimitive(inputString: String): Boolean =
    List("int", "double", "integer", "float", "string", "long", "bigint").contains(inputString.toLowerCase())

  def toJson(inputObject: Object, prettyPrint: Boolean = false): String = {
    val map = toMap(inputObject)
    if(prettyPrint)
      Json(DefaultFormats).writePretty(map)
    else
      Json(DefaultFormats).write(map)
  }

  def toMap(inputObject: Object): Map[String, Object] = {
    val cls = inputObject.getClass
    val fieldsMap: Map[String, Object] = cls.getFields.toList.map{ field =>
      val fieldValue: Object = field.get(inputObject)
      parseMember(field.getName, fieldValue)
    }.toMap
    val methodsMap: Map[String, Object] = cls.getDeclaredMethods.toList
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
      else if(recognize[util.Map[Object, Object]](memberValue)){
        val map = memberValue.asInstanceOf[util.Map[Object, Object]]
        map.asScala.toList.map{ case (key, value) =>
          val keyValue = if(isPrimitive(key)) key else toMap(key)
          val valValue = if(isPrimitive(value)) value else toMap(value)
          Map("key" -> keyValue, "value" -> valValue)
        }
      }
      else toMap(memberValue)
    (memberName, value)
  }

  private def isPrimitive(obj: Object): Boolean ={
    obj match {
      case _: java.lang.Integer => true
      case _: java.lang.String => true
      case _: java.lang.Double => true
      case _: java.lang.Float => true
      case _: java.lang.Boolean => true
      case _ => false
    }
  }
}