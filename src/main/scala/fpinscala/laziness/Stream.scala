package fpinscala.laziness

import Stream._

sealed trait Stream[+A] {
  def headOption: Option[A] = this match {
    case Empty => None
    case Cons(h, t) => Some(h())
  }

  // 5.1
  def toList: List[A] = {
    def go(s: Stream[A], acc: List[A]): List[A] = s match {
      case Empty => acc
      case Cons(h, t) => go(t(), h() :: acc)
    }

    go(this, List[A]()).reverse
  }

  // 5.2
  def take(n: Int): Stream[A] = this match {
    case Cons(h, t) if n > 1 => cons(h(), t().take(n - 1))
    case Cons(h, t) if n == 1 => cons(h(), empty)
    case _ => empty
  }

  def drop(n: Int): Stream[A] = this match {
    case Cons(_, t) if t == empty => empty
    case Cons(_, t) if n > 1 => t().drop(n - 1)
    case Cons(_, t) if n == 1 => t()
  }

  // 5.3
  def takeWhile(p: A => Boolean): Stream[A] = this match {
    case Cons(h, t) if p(h()) => cons(h(), t().takeWhile(p))
    case _ => empty
  }

  def exists(p: A => Boolean): Boolean = this match {
    case Cons(h, t) => p(h()) || t().exists(p)
    case _ => false
  }

  def foldRight[B](z: => B)(f: (A, => B) => B): B = this match {
    case Cons(h, t) => f(h(), t().foldRight(z)(f))
    case _ => z
  }

  def exists2(p: A => Boolean): Boolean =
    foldRight(false)((a, b) => p(a) || b)

  // 5.4 Stream의 모든 요소가 주어진 술어를 만족하는지 점검하는 forAll 함수를 구현. 만족하지 않을 시 즉시 순회를 마쳐야 함.
  def forAll(p: A => Boolean): Boolean =
    foldRight(true)((a, b) => p(a) && b)

  // 5.5 foldRight를 이용하여 takeWhile을 구현
  def takeWhileViaFoldRight(p: A => Boolean): Stream[A] =
    foldRight(empty[A])((a, b) => if (p(a)) cons(a, b) else empty)

  // 5.6 foldRight를 이용하여 headOption을 구현하라
  //  def headOptionViaFoldRight: Option[A] = ???

  // 5.7 foldRight를 이용하여 map, filter, append, flatMap을 구현. append 메서드는 자신의 인수에 대해 엄격하지 않아야 함
  def map[B](f: A => B): Stream[B] =
    foldRight(empty[B])((a, b) => cons(f(a), b))

  def filter(f: A => Boolean): Stream[A] =
    foldRight(empty[A])((a, b) => if (f(a)) cons(a, b) else empty)

  def append[B >: A](x: Stream[B]): Stream[B] =
    foldRight(x)((a, b) => cons(a, x))

  def flatMap[B](f: A => Stream[B]): Stream[B] =
    foldRight(empty[B])((a, b) => f(a) append b)


  // 5.13 unfold를 이용하여 map, take, takeWhile, zipWith, zipAll을 구현하라
  // zipAll 함수는 스트림에 요소가 더 있는 한 순회를 계속해야 한다
  // 각 스트림이 소진되었는지는 Option을 이용해 지정한다

  def map2[A, B](f: A => B): Stream[B] = unfold(this) {
    case Cons(h, t) => Some((f(h()), t()))
    case _ => None
  }

  def take2(n: Int): Stream[A] = unfold(this, n) {
    case (Cons(h, t), n) => if (n == 1) Some(h(), (empty, 0)) else Some(h(), (t(), n - 1))
    case _ => None
  }

  def takeWhile2(f: A => Boolean): Stream[A] = unfold(this) {
    case Cons(h, t) if f(h) => Some(h(), t())
    case _ => None
  }

  def zipWith[B, C](a: Stream[B])(f: (A, B) => C): Stream[C] = unfold(this, a) {
    case (Cons(h1, t1), Cons(h2, t2)) => Some(f(h1(), h2()), (t1(), t2()))
    case _ => None
  }

  def zipAll[B](s2: Stream[B]): Stream[(Option[A], Option[B])] = ???

  // 5.14 앞에서 작성한 함수들을 이용하여 startsWith를 구현하라. 이 함수는 한 Stream이 다른 한 Stream의 선행 순차열 (prefix: 접두사)인지 점검해야 한다.
  // 예를 들어 Stream(1, 2, 3) startsWith Stream(1, 2) 는 true가 되어야 한다
  def startsWith[A](s: Stream[A]): Boolean = ???

  // 5.15 unfold를 이용해서 tails를 구현하라. tails는 주어진 입력 Stream과 그 후행 순차열(suffix, 접미사)들로 이루어진 스트림을 돌려준다.
  // 예를 들어 Stream(1, 2, 3)에 대해 이 함수는 원래의 Stream(Stream(1, 2, 3), Stream(2, 3), Stream(3), Stream())을 돌려주어야 한다
  def tails: Stream[Stream[A]]  = ???

  // 5.16 tails를 일반화한 scanRight 함수를 작성하라. 이 함수는 중간 결과들의 스트림을 돌려주는 foldRight와 비슷하다
  // scala> Stream(1, 2, 3).scanRight(0)(_ + _).toList
  // res0: List[Int] = List(6, 5, 3, 0)
  // 이 예는 표현식 List(1+2+3+0, 2+3+0, 3+0, 0)과 동등해야 한다. 독자의 구현은 중간 결과들을 재사용해야 한다.
  // 즉, 요소가 n개인 Stream을 훑는 데 걸리는 시간이 항상 n에 선형 비례해야 한다. 이 함수를 unfold를 이용해서 구현할 수 있을까?
  // 있다면 어떻게? 없다면 왜 그럴까? 이 함수를 앞에서 작성한 어떤 함수로 구현할 수는 없을까?
  def scanRight = ???
}

case object Empty extends Stream[Nothing]

case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) empty
    else cons(as.head, apply(as.tail: _*))

  // 5.11 좀 더 일반화된 스트림 구축 함수 unfold를 작성하라
  // 이 함수는 초기 상태 하나와 다음 상태 및 (생성된 스트림 안의)다음 값을 산출하는 함수 하나를 받아야 한다
  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = f(z) match {
    case Some((h, s)) => Stream.cons(h, unfold(s)(f))
    case None => Stream.empty
  }
}
