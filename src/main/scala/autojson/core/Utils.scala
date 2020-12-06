package autojson.core

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

  def filter[T <: CtElement](c: Class[T]): TypeFilter[T] = new TypeFilter[T](c)
}
