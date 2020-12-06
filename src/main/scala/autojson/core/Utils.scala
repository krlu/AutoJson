package autojson.core

import java.io.File

import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtElement
import spoon.reflect.visitor.filter.TypeFilter

import scala.reflect.ClassTag

object Utils {
  def recognize[T](x: Any)(implicit tag: ClassTag[T]): Boolean =
    x match {
      case _: T => true
      case _ => false
    }
  def recognize[T](seq: Seq[Any])(implicit tag: ClassTag[T]): Boolean = {
    seq.forall(x => recognize(x))
  }

  def isJavaFile(path: String): Boolean = path.split("\\.").last == "java"
  def isScalaFile(path: String): Boolean = path.split("\\.").last == "scala"

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

  def listJavaFiles(path: String): Seq[File] = {
    val file = new File(path)
    if (file.isDirectory) {
      file.listFiles.toList.flatMap(child => listJavaFiles(child.getAbsolutePath))
    }
    else if(file.exists && isJavaFile(file.getName)) {
      List[File](file)
    }
    else List.empty[File]
  }

  def filter[T <: CtElement](c: Class[T]): TypeFilter[T] = new TypeFilter[T](c)
}
