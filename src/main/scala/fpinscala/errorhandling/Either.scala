package fpinscala.errorhandling

// 4.6 Right 값에 대해 작용하는 버전의 map, flatMap, map2, Either를 구현할 것
sealed trait Either[+E, +A] {
  def map[B](f: A => B): Either[E, B] = this match {
    case Right(a) => Right(f(a))
    case Left(e) => Left(e)
  }

  // 우변에 대한 사상에서 +E 공변 주해를 만족하기 위해 반드시 왼쪽 형식 매개변수를 적절한 상위 형식으로 승격시킬 필요가 있음
  def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B] = this match {
    case Right(a) => f(a)
    case Left(e) => Left(e)
  }

  def orElse[EE >: E, B >: A](b: => Either[EE, B]): Either[EE, B] = this match {
    case Left(a) => b
    case Right(a) => Right(a)
  }

  def map2[EE >: E, B, C](b: Either[EE, B])(f: (A, B) => C): Either[EE, C] =
    for {
      x <- this
      y <- b
    } yield f(x, y)

  //  def map2[EE >: E, B, C](b: Either[EE, B])(f: (A, B) => C): Either[EE, C] =
  //    this flatMap(x => b map(y => f(x, y)))

  //  def map2[EE >: E, B, C](b: Either[EE, B])(f: (A, B) => C): Either[EE, C] = (this, b) match {
  //    case (Left(a), _) => Left(a)
  //    case (_, Left(a)) => Left(a)
  //    case (Right(a), Right(b)) => Right(f(a, b))
  //  }
}

case class Left[+E](value: E) extends Either[E, Nothing]

case class Right[+A](value: A) extends Either[Nothing, A]

object Either {
  def mean(xs: IndexedSeq[Double]): Either[String, Double] =
    if (xs.isEmpty)
      Left("mean of empty list!")
    else
      Right(xs.sum / xs.length)

  def safeDiv(x: Int, y: Int): Either[Exception, Int] =
    try Right(x / y)
    catch {
      case e: Exception => Left(e)
    }

  def Try[A](a: => A): Either[Exception, A] =
    try Right(a)
    catch {
      case e: Exception => Left(e)
    }

  // 4.7 Either에 대한 sequence와 traverse를 작성할 것
  // 이 두 함수는 발생한 첫 오류를 돌려주어야 한다
  def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] = es match {
    case Nil => Right(Nil)
    case h :: t => h flatMap(x => sequence(t).map(y => x :: y))
  }

  def traverse[E, A, B](es: List[A])(f: A => Either[E, B]): Either[E, List[B]] = es match {
    case Nil => Right(Nil)
//    case h :: t => f(h) flatMap(x => traverse(t)(f) map (y => x :: y))
    case h :: t => (f(h) map2 traverse(t)(f))(_ :: _)
  }

  def sequence2[E, A](es: List[Either[E, A]]): Either[E, List[A]] = traverse(es)(identity)
}
