package autojson.core

import autojson.core.Utils._
import spoon.reflect.declaration.CtType

import scala.jdk.CollectionConverters._


object OntologyGenerator {
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
}
