package autojson.core

import java.io.{File, FileWriter}
import java.util

import autojson.core.Utils._
import spoon.reflect.CtModel
import spoon.reflect.declaration.{CtClass, CtField, CtInterface}
import spoon.reflect.reference.CtTypeReference

import scala.jdk.CollectionConverters._

object JsonCodeGenerator {


  /**
   * Input: ObjToConvert, pathToObject, pathToSerializers
   * Pseudocode:
   * - Base case: All fields are int, string, double, or float
   * - Recursive case 1: Object Type
   *    - if object type is an interface, how do we get the actual data?
   *      - find all implementing classes by search src directory tree under src/main/java
   *      - generate sub-serializer for each class
   *          - Add serializer to serializers directory (
   *          - NOTE: we will use multiple dispatch on a main method that utilizes on of the sub-serializer
   * -    If check if serializer for said object already exists
   *          If serializer does not exist create serializer for said object once
   *          References said serializer with method call
   *      Else, should be a primitive type, serialization should be trivial
   * - Recursive case 2: Collection or Array type
   *      - Generate code for iterating through collection as scala collection
   *      - Within iteration, determine
   * - Recursive case 3: Map Type
   *      - Map[A,B] => {"key": A, "value: B}
   *Output: JsonString
   */

  def saveSerializerCode(serializersPath: String, codeString: String, typeName: String, packageStr: String): Unit = {
    val fw = new FileWriter(serializersPath + s"/${typeName}Serializer.scala", true)
    fw.append(s"package $packageStr\n")
    fw.append(codeString)
    fw.close()
  }

  def generateSerializationCode(obj: Any, path: String, serializersPath: String): (String, String) = {
    val objTypeName = obj.getClass.getSimpleName
    generateSerializationCodeWithObjName(objTypeName, path, serializersPath)
  }

  private def generateSerializationCodeWithObjName(objTypeName: String, path: String, serializersPath: String): (String, String) = {
    if(!path.contains(objTypeName))
      throw new IllegalArgumentException(s"path $path does not contain $objTypeName.java")
    val model = getAST(path).orNull
    val fieldNames: Seq[(String, CtTypeReference[Any])] = getFields(model)
    val toMapName = "toMap"
    val existingSerializers = getExistingSerializerNames(serializersPath)
    // if is abstract class or interface
    if(isAbstractClass(objTypeName, model) || isInterface(objTypeName, model)){
      val file = new File(path)
      val implementingTypes = OntologyGenerator.buildOntology(file.getParent).filter(_._1 == objTypeName).map(_._2)
      if(implementingTypes.isEmpty) {
        println(s"$objTypeName has no implementing types!")
        return ("", objTypeName)
      }
      // TODO: each implement class needs to contain fields from parent interface/abstract-class
      // generate serializer for each
      implementingTypes.foreach{ implType =>
        if(!serializerExists(implType, existingSerializers))
          generateSerializer(implType, serializersPath)
      }
      val caseStatementsString = implementingTypes.map{ impltype =>
        val varName = impltype.toLowerCase
        s"      case $varName: $impltype => ${impltype}Serializer.toMap($varName)\n"
      }.mkString("").dropRight(1)
      val codeString =
        s"""import org.json4s.DefaultFormats
         |import org.json4s.native.Json
         |import ${javaPathToPackage(file.getParent)}.{${implementingTypes.mkString(",")}}
         |import ${model.getAllPackages.asScala.last}.$objTypeName
         |
         |object ${objTypeName}Serializer{
         |  def $toMapName(obj: Any): Map[String, Any] = {
         |    obj.asInstanceOf[$objTypeName] match{
         |       $caseStatementsString
         |    }
         |  }
         |  def toJson(obj : Any): String = {
         |    val map = $toMapName(obj)
         |    Json(DefaultFormats).write(map)
         |  }
         |}""".stripMargin
      return (codeString, objTypeName)
    }
    val innerMapData = fieldNames.map { case (fieldName, fieldType) =>
      val fieldTypeName = fieldType.getSimpleName
      val isArray = fieldTypeName.contains("[]")
      val superInterfaces = fieldType.getSuperInterfaces.asScala.toList
      val isCollection = superInterfaces.exists(_.getSimpleName == "Collection")
      val isMap = fieldTypeName == "Map"
        val fieldValue =
        // if the field is an array or collection, we generate code that iterates through each element
        // serializations of key/values that are complex objects require calls to their respective serializers
        // if their serializers don't exist yet, we create a serializer for said object on the spot
        if(isArray || isCollection){
          val paramTypeName = fieldType.getDirectChildren.asScala.filter(recognize[CtTypeReference[Any]](_))
            .map(_.asInstanceOf[CtTypeReference[Any]].getSimpleName).headOption match {
            case None => ""
            case Some(name) => name
          }
          if(!serializerExists(paramTypeName, existingSerializers) && !isPrimitive(paramTypeName)){
            generateSerializer(paramTypeName, serializersPath)
          }
          val collElementSerializerCode = if(isPrimitive(paramTypeName)) "data" else s"${paramTypeName}Serializer.$toMapName(data)"
          s"""
             |      castedObj.$fieldName.asScala.toList.map{ data =>
             |        $collElementSerializerCode
             |      }""".stripMargin
        }
        // if the field is a map, we iterate through each key/value pair and serialize in the form: Map[A,B] => {"key": A, "value: B}
        // serializations of key/values that are complex objects require calls to their respective serializers
        // if their serializers don't exist yet, we create a serializer for said object on the spot
        else if(isMap) {
          val typeParamNames = fieldType.getDirectChildren.asScala.toArray
            .filter(recognize[CtTypeReference[Any]](_))
            .map(_.asInstanceOf[CtTypeReference[Any]].getSimpleName)
          val keyType = typeParamNames(0)
          val valType = typeParamNames(1)
          if(!serializerExists(keyType, existingSerializers) && !isPrimitive(keyType)){
            generateSerializer(keyType, serializersPath)
          }
          if(!serializerExists(valType, existingSerializers) && !isPrimitive(keyType)){
            generateSerializer(valType, serializersPath)
          }
          val keySerializationCode = if(isPrimitive(keyType)) "key" else s"${keyType}Serializer.$toMapName(key)"
          val valSerializationCode = if(isPrimitive(valType)) "value" else s"${valType}Serializer.$toMapName(value)"
          s"""
             |      castedObj.$fieldName.asScala.toList.map{ case (key, value) =>
             |        Map("key" -> $keySerializationCode, "value" -> $valSerializationCode)
             |      }""".stripMargin
        }
        // if the field is some complex type, we call to the serializer for said object
        // if their serializers don't exist yet, we create a serializer for said object on the spot
        // if type is abstract class or interface, we generate all serializers and use multiple dispatch
        else if(!isPrimitive(fieldTypeName)) {
          if(!serializerExists(fieldTypeName, existingSerializers)){
            generateSerializer(fieldTypeName, serializersPath)
          }
          s"${fieldTypeName}Serializer.$toMapName(castedObj.$fieldName)"
        } // all other cases, field is primitive, in which case serialization is trivial
        else
          s"castedObj.$fieldName"
      s"""\"$fieldName\"->$fieldValue"""
    }
    val innerMapStr = if(innerMapData.nonEmpty) innerMapData.mkString(",") + "," else ""
    val formattedMap =
      if(innerMapStr.length > 80) {
        s"""Map($innerMapStr"className" -> "$objTypeName"
           |    )""".stripMargin
      } else
        s"""Map($innerMapStr"className" -> "$objTypeName")""".stripMargin
    val serializerCode: String =
      s"""import org.json4s.DefaultFormats
         |import org.json4s.native.Json
         |import scala.jdk.CollectionConverters._
         |import ${model.getAllPackages.asScala.last}.$objTypeName
         |
         |object ${objTypeName}Serializer{
         |  def $toMapName(obj: Any): Map[String, Any] = {
         |    val castedObj = obj.asInstanceOf[$objTypeName]
         |    $formattedMap
         |  }
         |  def toJson(obj : Any): String = {
         |    val map = $toMapName(obj)
         |    Json(DefaultFormats).write(map)
         |  }
         |}""".stripMargin
    (serializerCode, objTypeName)
  }

  private def isInterface(objTypeName: String, model: CtModel): Boolean =
    model.getElements(filter(classOf[CtInterface[Any]])).asScala.exists{e => e.getSimpleName == objTypeName}

  private def isAbstractClass(objTypeName: String, model: CtModel): Boolean =
    model.getElements(filter(classOf[CtClass[Any]])).asScala.exists{e => e.getSimpleName == objTypeName && e.isAbstract}

  private def scalaPathToPackage(scalaPath: String): String = pathToPackage(scalaPath, "scala")
  private def javaPathToPackage(javaPath: String): String = pathToPackage(javaPath, "java")
  private def pathToPackage(path: String, pathType: String): String = {
    val normalizedPath = path.replace("\\", "/")
    if(normalizedPath.contains(s"src/main/${pathType}"))
      normalizedPath.split(s"src/main/$pathType").toList.last.split("/").toList.mkString(".").drop(1)
    else throw new IllegalArgumentException(s"serializers path $path did not contain src/main/$pathType")
  }

  def generateSerializer(fieldTypeName: String, serializersPath: String, javaPath: String = "src/main/java"): Unit = {
    // assuming that the root of all java programs is src/main/java
    listJavaFiles(javaPath).find{ file =>
      file.getName == s"$fieldTypeName.java"
    } match {
      case Some(file) =>
        val (code, _) = generateSerializationCodeWithObjName(fieldTypeName, file.getAbsolutePath, serializersPath)
        saveSerializerCode(serializersPath, code, fieldTypeName, scalaPathToPackage(serializersPath))
      case None =>
        throw new IllegalStateException(s"Source code file for $fieldTypeName not found in $serializersPath")
    }
  }

  private def serializerExists(objTypeName: String, existingSerializers: List[String]): Boolean =
    existingSerializers.contains(s"${objTypeName}Serializer")

  private def getExistingSerializerNames(serializersPath: String): List[String] = {
    val dir = new File(serializersPath)
    if (dir.isDirectory) {
      dir.listFiles.toList.filter(file => isScalaFile(file.getName)).map(_.getName.split("\\.").head)
    }
    else List.empty[String]
  }


  /**
   * @param model - reference to source code AST
   * @return List of pairs (filedName, typeName)
   */
  private def getFields(model: CtModel): Seq[(String, CtTypeReference[Any])] = {
    val fields: util.List[CtField[Any]] = model.getElements(filter(classOf[CtField[Any]]))
    fields.asScala.map{ field =>
      field.getType.getSuperInterfaces.asScala.foreach{ x =>
        x.getSimpleName
      }
      (field.getReference.toString, field.getType)
    }.toList
  }

  private def isPrimitive(typeString: String): Boolean = {
    List("int", "string", "float", "double", "integer").contains(typeString.toLowerCase)
  }
}
