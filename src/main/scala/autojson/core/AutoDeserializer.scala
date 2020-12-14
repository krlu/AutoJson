package autojson.core

import java.lang.reflect.{Modifier, ParameterizedType, Type}

import autojson.core.Utils._
import autojson.core.Utils.recognize
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.jdk.CollectionConverters._

object AutoDeserializer {

  def jsonToObject[T](jsonString: String, classOf: Class[T]): T = {
    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    val map = parse(jsonString).extract[Map[String, Any]]
    mapToObject(convert(map), classOf)
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

  private def mapToObject[T](map: Map[String, Any], classOf: Class[T]): T = {
    val packageName = classOf.getPackage.toString.split(" ").last
    val fields = classOf.getFields.toList
    val methods = classOf.getDeclaredMethods.toList.filter(method => Modifier.isPublic(method.getModifiers) && method.getParameterCount == 0)
    val isAbstract = Modifier.isAbstract(classOf.asInstanceOf[Class[_]].getModifiers)
    val isInterface = classOf.isInterface
    if(isAbstract || isInterface){
      val subType = Class.forName(s"$packageName.${map("className").toString}").asInstanceOf[Class[T]]
      return mapToObject(map, subType)
    }
    val methodArgs = methods.filter(m => map.contains(m.getName)).map{ method =>
      val name = method.getName
      val value = map(method.getName).asInstanceOf[Object]
      val returnType = method.getReturnType
      val typeParam = method.getGenericReturnType
      name -> parseArgs(value, returnType, typeParam, name)
    }
    val arguments = fields.map{field =>
      val name = field.getName
      val value = map(field.getName).asInstanceOf[Object]
      val fieldType = field.getType
      name -> parseArgs(value, fieldType, field.getGenericType, name)
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

  private def parseArgs(value: Object, valueType: Class[_], genericType: Type, valueName: String): Object = {
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
          mapToObject(element.asInstanceOf[Map[String, Any]], typeParamClass)
        }
      }
      if(valueType.getSimpleName.contains("Set")) {
        if(interfaces.contains("Iterable")) collection.toSet
        else collection.toSet.asJava
      } else if(valueType.getSimpleName.contains("List"))
        if(interfaces.contains("Iterable")) collection
        else collection.asJava
      else if(valueType.getSimpleName.contains("Map")) {
        value.asInstanceOf[List[_]].map{ element =>
          val mapOfMap = element.asInstanceOf[Map[Object, Object]]
          val mKey = mapOfMap("key")
          val mValue = mapOfMap("value")
          val mk =
            if(isPrimitive(mKey)) mKey
            else {
              val map = mKey.asInstanceOf[Map[String,Any]]
              val packageName = valueType.getPackage.toString.split(" ").last
              val subType = Class.forName(s"$packageName.${map("className").toString}")
              mapToObject(mKey.asInstanceOf[Map[String,Any]], subType)
            }
          val mv =
            if(isPrimitive(mKey)) mValue
            else {
              val map = mValue.asInstanceOf[Map[String,Any]]
              val packageName = valueType.getPackage.toString.split(" ").last
              val subType = Class.forName(s"$packageName.${map("className").toString}")
              mapToObject(mValue.asInstanceOf[Map[String,Any]], subType)
            }
          mk -> mv
        }.toMap
      } else
        throw new IllegalArgumentException(s"Could not deserialize iterable/collection structure of ${valueType.getName}")
    }
    else if(recognize[Map[String, Any]](value))
      mapToObject(value.asInstanceOf[Map[String, Any]], valueType).asInstanceOf[Object]
    else{
      throw new IllegalStateException(s"Unable to parse field $valueName of type ${valueType.getSimpleName}!")
    }
  }
}
