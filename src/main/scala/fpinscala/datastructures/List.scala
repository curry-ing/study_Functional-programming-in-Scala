package fpinscala.datastructures

sealed trait List[+A]

case object Nil extends List[Nothing]

case class Cons[+A](head: A, tail: List[A]) extends List[A]

object List {
  def sum(ints: List[Int]): Int = ints match {
    case Nil => 0
    case Cons(x, xs) => x + sum(xs)
  }

  def product(ds: List[Double]): Double = ds match {
    case Nil => 1.0
    case Cons(0.0, _) => 0.0
    case Cons(x, xs) => x * product(xs)
  }

  def tail[A](ls: List[A]): List[A] = ls match {
    case Nil => Nil
    case Cons(x, xs) => xs
  }

  def setHead[A](r: A, ls: List[A]): List[A] = ls match {
    case Nil => Nil
    case Cons(_, xs) => Cons(r, xs)
  }

  def drop[A](l: List[A], n: Int): List[A] = l match {
    case Nil => Nil
    case Cons(x, xs) => if (n == 0) l else drop(xs, n - 1)
  }

  def dropWhileOld[A](l: List[A], f: A => Boolean): List[A] = l match {
    case Cons(x, xs) if f(x) => dropWhileOld(xs, f)
    case _ => l
  }

  def dropWhile[A](l: List[A])(f: A => Boolean): List[A] = l match {
    case Cons(x, xs) if f(x) => dropWhile(xs)(f)
    case _ => l
  }

  def append[A](a1: List[A], a2: List[A]): List[A] = a1 match {
    case Nil => a2
    case Cons(h, t) => Cons(h, append(t, a2))
  }

  def init[A](l: List[A]): List[A] = l match {
    //    def loop(l1: List[A], l2: List[A]): List[A] = l2 match {
    case Nil => Nil
    case Cons(x, Nil) => Nil
    case Cons(h, t) => Cons(h, init(t))
  }

  // 3.8
  def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B = as match {
    case Nil => z
    case Cons(x, xs) => f(x, foldRight(xs, z)(f))
  }

  def sum2(ns: List[Int]): Int = foldRight(ns, 0)((x, y) => x + y)

  def product2(ns: List[Double]): Double = foldRight(ns, 1.0)(_ * _)

  // 3.9
  def length[A](as: List[A]): Int = foldRight(as, 0)((x, y) => y + 1)

  // 3.10
  def foldLeft[A, B](as: List[A], z: B)(f: (B, A) => B): B = as match {
    case Nil => z
    case Cons(x, xs) => foldLeft(xs, f(z, x))(f)
  }

  // 3.11
  def sum3(ns: List[Int]): Int = foldLeft(ns, 0)(_ + _)
  def product3(ns: List[Double]): Double = foldLeft(ns, 1.0)(_ * _)
  def length2[A](as: List[A]): Int = foldLeft(as, 0)((x, _) => x + 1)

  // 3.12
  def reverse[A](l: List[A]): List[A] = {
    foldLeft(l, Nil: List[A])((x, y) => Cons(y, x))
  }

  // 3.13
  def foldLeftByFoldRight() = ???
  def foldRightByFoldLeft() = ???

  // 3.14
  def append2[A](l1: List[A], l2: List[A]): List[A] = {
    foldRight(l1, l2)(Cons(_, _))
  }

//  def append2[A](l1: List[A], l2: List[A]): List[A] = {
//  }

  // 3.15 D 목록들의 목록을 하나의 목록으로 연결하는 함수를 작성하라 (실행 시간은 반드시 모든 목록의 전체 길이에 선형으로 비례해야 함)
  def concatenate[A](l: List[A]): List[A] = {
//    foldRight(l, Nil: List[A])((a, b) => append(a, b))
    ???
  }


  // 3.16 정수 목록의 각 요소에 1을 더해서 목록을 반환하는 함수를 작성하라
  def addOne(l: List[Int]): List[Int] = {
    foldRight(l, Nil: List[Int])((x, y) => Cons(x + 1, y))
  }

  def addOne2(l: List[Int]): List[Int] = l match {
    case Cons(x, xs) => Cons(x + 1, addOne2(xs))
    case Nil => Nil
  }

  // 3.17 List[Double]의 각 값을 String으로 변환하는 함수를 작성.
  def elemToString(l: List[Double]): List[String] = l match {
    case Nil => Nil
    case Cons(x, xs) => Cons(x.toString, elemToString(xs))
  }

  // 3.18 목록의 구조를 유지하면서 목록의 각 요소를 수정하는 작업을 일반화한 함수 map을 작성
  def map[A, B](as: List[A])(f: A => B): List[B] = as match {
    case Nil => Nil
    case Cons(x, xs) => Cons(f(x), map(xs)(f))
  }

  // 3.19 목록에서 주어진 술어를 만족하지 않는 요소들을 제거하는 함수 filter를 작성 후 이를 이용하여 List[Int]의 모든 홀수를 제거할 것
  def filter[A](as: List[A])(f: A => Boolean): List[A] = as match {
    case Nil => Nil
    case Cons(x, xs) => if (f(x)) Cons(x, filter(xs)(f)) else filter(xs)(f)
  }

  // 3.20 map과 비슷하되 하나의 요소가 아니라 목록을 최종 결과 목록에 삽입하는 함수 flatMap 작성
  def flatMap[A, B](as: List[A])(f: A => List[B]): List[B] = as match {
    case Nil => Nil
    case Cons(x, xs) => append(f(x), flatMap(xs)(f))
  }

  // 3.21 flatMap을 이용하여 filter 구현
  def filterWithFlatMap[A, B](l: List[A])(f: A => Boolean): List[A] =
  flatMap(l)(a => if (f(a)) List(a) else Nil)

  // 3.22 목록 두 개를 받아 대응되는 요소들을 더한 값들로 이루어진 새 목록을 구축하는 함수를 작성
  // ex> List(1, 2, 3) & List(4, 5, 6) => List(5, 7 ,9)
  def addPairs(l1: List[Int], l2: List[Int]): List[Int] = (l1, l2) match {
    case (a, Nil) => Nil
    case (Nil, b) => Nil
    case (Cons(x, xs), Cons(y, ys)) => Cons(x + y, addPairs(xs, ys))
  }

  // 3.23 위 3.22를 정수나 덧셈에 국한되지 않도록 일반화 할 것 (zipWith)
  def zipWith[A](l: List[A], r: List[A])(f: (A, A) => A): List[A] = (l, r) match {
    case (_, Nil) => Nil
    case (Nil, _) => Nil
    case (Cons(x, xs), Cons(y, ys)) => Cons(f(x, y), zipWith(xs, ys)(f))
  }

  // 3.24 * List가 또 다른 List를 부분 순차열로서 담고있는지 점검하는 hasSubsequence 함수를 작성
  def hasSubsequence[A](sup: List[A], sub: List[A]): Boolean = ???





  def apply[A](as: A*): List[A] =
    if (as.isEmpty) Nil
    else Cons(as.head, apply(as.tail: _*))
}


