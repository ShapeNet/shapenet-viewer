package edu.stanford.graphics.shapenet.util

/**
 * Some generic conversion utils that are helpful
 * @author Angel Chang
 */
object ConversionUtils {
  type or[L,R] = Either[L,R]

  implicit def l2Or[L,R](l: L): L or R = Left(l)
  implicit def r2Or[L,R](r: R): L or R = Right(r)

  def seqToJavaList[T](seq: Seq[T]): java.util.List[T] = {
    val list = new java.util.ArrayList[T](seq.size)
    for (elem <- seq) {
      list.add(elem)
    }
    list
  }

  import java.util.function.{ Function => JFunction, Predicate => JPredicate, BiPredicate }

  //usage example: `i: Int => 42`
  implicit def toJavaFunction[A, B](f: Function1[A, B]) = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  //usage example: `i: Int => true`
  implicit def toJavaFunction[A, B, C](f: Function1[A, C], res: B) = new JFunction[A, B] {
    override def apply(a: A): B = {
      f(a)
      res
    }
  }

  implicit def toJavaPredicate[A](f: Function1[A, Boolean]) = new JPredicate[A] {
    override def test(a: A): Boolean = f(a)
  }

  //usage example: `(i: Int, s: String) => true`
  implicit def toJavaBiPredicate[A, B](predicate: (A, B) => Boolean) =
    new BiPredicate[A, B] {
      def test(a: A, b: B) = predicate(a, b)
    }

}
