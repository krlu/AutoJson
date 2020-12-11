package autojson.core

import scala.reflect.ClassTag

object Utils {
  def recognize[T](x: Any)(implicit tag: ClassTag[T]): Boolean =
    x match {
      case _: T => true
      case _ => false
    }
}
