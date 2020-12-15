package autojson.core

import java.io.File

import scala.reflect.ClassTag

object Utils {

  private[autojson] def recognize[T](x: Any)(implicit tag: ClassTag[T]): Boolean =
    x match {
      case _: T => true
      case _ => false
    }

  private[autojson] def isPrimitiveStr(inputString: String): Boolean =
    Set("int", "double", "integer", "float", "string", "long", "bigint").contains(inputString.toLowerCase())


  private[autojson] def isPrimitive(obj: Object): Boolean ={
    obj match {
      case _: java.lang.Integer => true
      case _: java.lang.String => true
      case _: java.lang.Double => true
      case _: java.lang.Float => true
      case _: java.lang.Boolean => true
      case _ => false
    }
  }

  private[autojson] def isJavaFile(path: String): Boolean = path.split("\\.").last == "java"
  private[autojson] def isScalaFile(path: String): Boolean = path.split("\\.").last == "scala"

  private[autojson] def listAllFiles(path: String): Seq[File] = {
    val file = new File(path)
    if (file.isDirectory) {
      file.listFiles.toList.flatMap(child => listAllFiles(child.getAbsolutePath))
    }
    else if(file.exists) {
      List[File](file)
    }
    else List.empty[File]
  }
}
