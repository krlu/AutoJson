package autojson.core

import java.io.File
import java.util

import org.json4s._
import org.json4s.native.{Json, Serialization}
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.{CtElement, CtField, CtType}
import spoon.reflect.reference.CtTypeReference
import spoon.reflect.visitor.filter.TypeFilter

import scala.jdk.CollectionConverters._

object JsonCodeGenerator {


  /**
   * Pseudocode:
   * - Base case: All fields are int, string, double, or float
   * - Recursive case 1: Object Type
   *    - if object type is an interface, how do we get the actual data?
   *      - find all implementing classes by search src directory tree under src/main/java
   *      - generate sub-serializer for each class
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
   */

  def main(args: Array[String]): Unit = {
  }

  private def filter[T <: CtElement](c: Class[T]): TypeFilter[T] = new TypeFilter[T](c)

  /**
   * @param path - path to source code
   * @return List of pairs (filedName, typeName)
   */
  private def getFields(path: String): Seq[(String, CtTypeReference[Any])] = {
    val model = getAST(path).orNull
    val fields: util.List[CtField[Any]] = model.getElements(filter(classOf[CtField[Any]]))
    fields.asScala.map{ field =>
      field.getType.getSuperInterfaces.asScala.foreach{ x =>
        x.getSimpleName
      }
      (field.getReference.toString, field.getType)
    }.toList
  }

  private def isJavaFile(path: String): Boolean = path.split("\\.").last == "java"

  def getAST(inputPath: String): Option[CtModel] = {
    if(isJavaFile(inputPath)) {
      val launcher = new Launcher
      launcher.addInputResource(inputPath)
      launcher.getEnvironment.setAutoImports(true)
      launcher.getEnvironment.setNoClasspath(true)
      launcher.buildModel
      val model: CtModel = launcher.getModel
      Some(model)
    } else None
  }

  def getMethodForClass(obj: TestClass, serializersIndices: String): Option[String] = {
    val file = io.Source.fromFile(serializersIndices)
    val methodOpt = file.getLines().toList.find(methodName =>
      methodName.contains(obj.getClass.getName))
    file.close()
    methodOpt
  }

  def generateSerializationCode(obj: Any, path: String, serializerIndices: String): String = {
    val objTypeName = obj.getClass.getSimpleName
    val fieldNames: Seq[(String, CtTypeReference[Any])] = getFields(path)
    val innerMap = fieldNames.map { case (fieldName, fieldType) =>
      val isArray = fieldType.getSimpleName.contains("[]")

      val superInterfaces = fieldType.getSuperInterfaces.asScala.toList
      val isCollection = superInterfaces.exists(_.getSimpleName == "Collection")
      val isMap = fieldType.getSimpleName == "Map"

      val fieldValue =
        if(isArray || isCollection){
          s"""    $fieldName.asScala.toList.map{ data =>
             |      mapFieldsForData(data)
             |    }
             |  """.stripMargin
        }
        else if(isMap) {
          val typeParams = fieldType.getDirectChildren.asScala.toArray
            .filter(Recognizer.recognize[CtTypeReference[Any]](_))
            .map(_.asInstanceOf[CtTypeReference[Any]].getSimpleName)
          val keyType = typeParams(0)
          val valType = typeParams(1)
          val keySerializationCode = if(isPrimitive(keyType)) "key" else s"mapFieldsFor$keyType(key)"
          val valSerializationCode = if(isPrimitive(valType)) "value" else s"mapFieldsFor$valType(value)"

          s"""
             |    castedObj.$fieldName.asScala.toList.map{ case (key, value) =>
             |      Map("key" -> $keySerializationCode, "value" -> $valSerializationCode)
             |    }
             |  """.stripMargin
        }
        else s"castedObj.$fieldName"
      s"""\"$fieldName\"->$fieldValue"""
    }.mkString(",")
    val programToGenerate: String =
      s"""def mapFieldsFor$objTypeName(obj: Any): String = {
         |  val castedObj = obj.asInstanceOf[$objTypeName]
         |  val map = Map($innerMap)
         |  Json(DefaultFormats).write(map)
         |}""".stripMargin
    programToGenerate
  }

  def isPrimitive(typeString: String): Boolean = {
    List("int", "string", "float", "double", "integer").contains(typeString.toLowerCase)
  }

  /**
   * @param path - to src of java files
   * @return List[(String, String)] pair of interface/class names.
   *         - left pair is parent
   *         - right pair is child
   */
  def buildOntology(path: String): Set[(String, String)] = {
    val files = listJavaFiles(path)
    files.flatMap{ file =>
      val model = getAST(file.getPath).orNull
      val classModel = model.getElements(filter(classOf[CtType[Any]])).asScala.head
      val superclass =classModel.getSuperclass
      val inheritees =  classModel.getSuperInterfaces.asScala ++
        (if(superclass != null) List(superclass) else List())
      inheritees.map{ inheritor =>
        (inheritor.getSimpleName, classModel.getSimpleName)
      }
    }.toSet
  }

  private def listJavaFiles(path: String): Seq[File] = {
    val file = new File(path)
    if (file.isDirectory) {
      file.listFiles.toList.flatMap(child => listJavaFiles(child.getAbsolutePath))
    }
    else if(file.exists && isJavaFile(file.getName)) {
      List[File](file)
    }
    else List.empty[File]
  }

}