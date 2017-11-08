package fpinscala.monoid

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}

object Monoid {
  val stringMonoid = new Monoid[String] {
    def op(a1: String, a2: String) = a1 + a2
    val zero = ""
  }

  def listMonoid[A] = new Monoid[List[A]] {
    def op(a1: List[A], a2: List[A]) = a1 ::: a2
    val zero = Nil
  }

  // 10.1 정수 덧셈과 곱셈에 대한 Monoid 인스턴스들과 해당 부울 연산자들을 제시하라
  val intAddition: Monoid[Int] = new Monoid[Int] {
    def op(a1: Int, a2: Int) = a1 + a2
    val zero = 0
  }

  val intMultiplication: Monoid[Int] = new Monoid[Int] {
    def op(a1: Int, a2: Int) = a1 * a2
    val zero = 1
  }

  val booleanOr: Monoid[Boolean] = new Monoid[Boolean] {
    def op(a1: Boolean, a2: Boolean) = a1 || a2
    val zero = false
  }

  val booleanAnd: Monoid[Boolean] = new Monoid[Boolean] {
    def op(a1: Boolean, a2: Boolean) = a1 && a2
    val zero = true
  }

  // 10.2 Option 값들의 조합을 위한 Monoid 인스턴스를 제시하라
  def optionMonoid[A]: Monoid[Option[A]] = new Monoid[Option[A]] {
    def op(a1: Option[A], a2: Option[A]) = a1 orElse a2
    val zero = None
  }

  def dual[A](m: Monoid[A]): Monoid[A] = new Monoid[A] {
    def op(x: A, y: A):A = m.op(y, x)
    def zero = m.zero
  }

  def firstOptionMonoid[A]: Monoid[Option[A]] = optionMonoid[A]
  def lastOptionMonoid[A]: Monoid[Option[A]] = dual(firstOptionMonoid)

  // 10.3 인수의 형식과 반환값의 형식이 같은 함수를 자기함수(endofunction)라고 부른다.
  // 자기함수들을 위한 모노이드를 작성하라
  def endoMonoid[A]: Monoid[A => A] = new Monoid[A => A] {
    def op(f: A => A, g: A => A) = f compose g   // `g andThen f` also completes
    val zero = (a: A) => a
  }

  // 10.4 2장에서 개발한 속성 기반 검사 프레임워크를 이용해서 모노이드 법칙에 대한 속성을 구현하라
  // 그리고 그 속성을 이용해서 앞에서 작성한 모노이드들을 검사하라
//  def monoidLaws[A](m: Monoid[A], gen: Gen[A]): Prop = ???

  def concatenate[A](as: List[A], m: Monoid[A]): A = as.foldLeft(m.zero)(m.op)

  // 10.5 foldMap을 구현하라
  def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B =
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))


  // 10.6 foldMap을 foldLeft나 foldRight를 이용하여 구현할 수 있다.
  // 그런데, foldLeft와 foldRight를 foldMap을 이용하여 구현할 수도 있다. 시도해 보라
  def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B =
    foldMap(as, endoMonoid[B])(f.curried)(z)


  // 10.7 IndexedSeq에 대한 foldMap을 구현하라.
  // 구현은 반드시 순차열을 둘로 분할하여 재귀적으로 각 절반을 처리하고
  // 그 결과들을 모노이드를 이용해서 결합해야 한다
  def foldMapV[A, B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): B = {
    if (v.isEmpty) m.zero
    else if (v.length == 1) f(v.head)
    else {
      val (l, r) = v.splitAt(v.length / 2)
      m.op(foldMapV(l, m)(f), foldMapV(r, m)(f))
    }
  }

  // 10.8 7장에서 개발한 라이브러리를 이용하여 foldMap의         병렬 버전도 구현하라.
  // 힌트: Monoid[A]를 Monoid[Par[A]]로 승격하는 조합기 par를 구현하고, 그것을 이용해서 parFoldMap을 구현할 것

//  import fpinscala.pararellism.Nonblocking._

//  def par[A](m: Monoid[A]): Monoid[Par[A]]
//  def parFoldMap[A, B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): Par[B]

  // 10.9 foldMap을 이용하여 주어진 IndexedSeq[Int]가 정렬되어 있는지 검사하라.
  // 독창적인 Monoid를 고안해야 할 것
  def ordered(ints: IndexedSeq[Int]): Boolean = {
    val mon = new Monoid[Option[(Int, Int, Boolean)]] {
      val zero = None
      def op(x: Option[(Int, Int, Boolean)], y: Option[(Int, Int, Boolean)]) = (x, y) match {
        case (Some((a1, b1, p)), Some((a2, b2, q))) =>
          Some((a1 min a2, b1 max b2, p && q && b1 <= a2))
        case (a, None) => a
        case (None, a) => a
      }
    }
    foldMapV(ints, mon)(i => Some((i, i, true))).forall(_._3)
  }

  sealed trait WC
  case class Stub(chars: String) extends WC
  case class Part(lStub: String, words: Int, rStub: String) extends WC

  // 10.10 WC를 위한 모노이드 인스턴스를 작성하고, 그것이 모노이드 법칙을 지키는지 확인하라
  val wcMonoid: Monoid[WC] = new Monoid[WC] {
    val zero = Stub("")
    def op(a: WC, b: WC): WC = (a, b) match {
      case (Stub(c), Stub(d)) => Stub(c + d)
      case (Stub(c), Part(l, w, r)) => Part(c + l, w, r)
      case (Part(l, w, r), Stub(d)) => Part(l, w, r + d)
      case (Part(l1, w1, r1), Part(l2, w2, r2)) =>
        Part(l1, w1 + (if ((r1 + l2).isEmpty) 0 else 1) + w2, r2)
    }
  }

  // 10.11 WC 모노이드를 이용해서 String의 단어 개수를 세는 함수를 구현하라
  // 구현은 주어진 문자열을 부분 문자열들로 분할하고 각 부분 문자열의 단어 개수를 세는 과정을 재귀적으로 반복해서 전체 단어 개수를 구해야 한다
  def count(s: String): Int = {
    def wc(c: Char): WC = {
      if (c.isWhitespace)
        Part("", 0, "")
      else
        Stub(c.toString)
    }

    def unstub(s: String) = s.length min 1
    foldMapV(s.toIndexedSeq, wcMonoid)(wc) match {
      case Stub(`s`) => unstub(s)
      case Part(l, w, r) => unstub(l) + w + unstub(r)
    }
  }

  // 10.12 Foldable[List]와 Foldable[IndexedSeq], Foldable[Stream]을 구현하라.
  // foldRight와 foldLeft, foldMap 모두 서로를 이용해서 구현할 수 있지만, 그것이 가장 효율적인 구현은 아닐 수 있음을 기억하라
  trait Foldable[F[_]] {
    def foldRight[A, B](as: F[A])(z: B)(f: (A, B) => B): B =
      foldMap(as)(f.curried)(endoMonoid[B])(z)

    def foldLeft[A, B](as: F[A])(z: B)(f: (B, A) => B): B =
      foldMap(as)(a => (b: B) => f(b, a))(dual(endoMonoid[B]))(z)

    def foldMap[A, B](as: F[A])(f: A => B)(mb: Monoid[B]): B =
      foldRight(as)(mb.zero)((a, b) => mb.op(f(a), b))

    def concatenate[A](as: F[A])(m: Monoid[A]): A =
      foldLeft(as)(m.zero)(m.op)
  }

  object ListFoldable extends Foldable[List] {
    override def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B) =
      as.foldRight(z)(f)

    override def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B): B =
      as.foldLeft(z)(f)

    override def foldMap[A, B](as: List[A])(f: A => B)(mb: Monoid[B]): B =
      foldLeft(as)(mb.zero)((b, a) => mb.op(b, f(a)))
  }

  object IndexedSeqFoldable extends Foldable[IndexedSeq] {
    override def foldRight[A, B](as: IndexedSeq[A])(z: B)(f: (A, B) => B): B =
      as.foldRight(z)(f)

    override def foldLeft[A, B](as: IndexedSeq[A])(z: B)(f: (B, A) => B): B =
      as.foldLeft(z)(f)

    override def foldMap[A, B](as: IndexedSeq[A])(f: A => B)(mb: Monoid[B]): B =
      foldMapV(as, mb)(f)
  }

  object StreamFoldable extends Foldable[Stream] {
    override def foldRight[A, B](as: Stream[A])(z: B)(f: (A, B) => B): B =
      as.foldRight(z)(f)

    override def foldLeft[A, B](as: Stream[A])(z: B)(f: (B, A) => B): B =
      as.foldLeft(z)(f)
  }

  // 10.13 제 3장의 이진 Tree 자료 형식을 기억할 것이다
  // 그 자료구조를 위한 Foldable 인스턴스를 구현하라
  sealed trait Tree[+A]
  case class Leaf[A](value: A) extends Tree[A]
  case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

  object TreeFoldable extends Foldable[Tree] {
    override def foldMap[A, B](as: Tree[A])(f: A => B)(mb: Monoid[B]): B = as match {
      case Leaf(a) => f(a)
      case Branch(l, r) => mb.op(foldMap(l)(f)(mb), foldMap(r)(f)(mb))
    }
    override def foldLeft[A, B](as: Tree[A])(z: B)(f: (B, A) => B): B = as match {
      case Leaf(a) => f(z, a)
      case Branch(l, r) => foldLeft(r)(foldLeft(l)(z)(f))(f)
    }

    override def foldRight[A, B](as: Tree[A])(z: B)(f: (A, B) => B): B = as match {
      case Leaf(a) => f(a, z)
      case Branch(l, r) => foldRight(l)(foldRight(r)(z)(f))(f)
    }
  }

  // 10.14 Foldable[Option] 인스턴스를 작성하라
  object OptionFoldable extends Foldable[Option] {
    override def foldMap[A, B](as: Option[A])(f: A => B)(mb: Monoid[B]): B = as match {
      case None => mb.zero
      case Some(a) => f(a)
    }

    override def foldLeft[A, B](as: Option[A])(z: B)(f: (B, A) => B): B = as match {
      case None => z
      case Some(a) => f(z, a)
    }

    override def foldRight[A, B](as: Option[A])(z: B)(f: (A, B) => B): B = as match {
      case None => z
      case Some(a) => f(a, z)
    }
  }

  // 10.15 임의의 Foldable 구조를 List로 변환할 수 있다. 이러한 변환을 일반적인 방식으로 수행하는 함수를 작성하라
  // def toList[A](as: F[A]): List[A] = foldRight(as)(List[A]())(_ :: _)

  // 10.16 다음을 증명하라. A.op와 B.op가 둘 다 결합적이면 다음 함수에 대한 op의 구현은 자명하게 결합적이다
  def productMonoid[A, B](A: Monoid[A], B: Monoid[B]): Monoid[(A, B)] =
    new Monoid[(A, B)] {
      def op(x: (A, B), y: (A, B)) = (A.op(x._1, y._1), B.op(x._2, y._2))
      def zero = (A.zero, B.zero)
    }


  def mapMergeMonoid[K, V](V: Monoid[V]): Monoid[Map[K, V]] =
    new Monoid[Map[K, V]] {
      def zero = Map[K, V]()
      def op(a: Map[K, V], b: Map[K, V]) =
        (a.keySet ++ b.keySet).foldLeft(zero) { (acc, k) =>
          acc.updated(k, V.op(a.getOrElse(k, V.zero), b.getOrElse(k, V.zero)))
        }
    }

  // 10.17 결과가 모노이드이인 함수들에 대한 모노이드 인스턴스를 작성하라
  def functionMonoid[A, B](B: Monoid[B]): Monoid[A => B] =
    new Monoid[A => B] {
      def zero: A => B = a => B.zero
      def op(f: A => B, g: A => B) = a => B.op(f(a), g(a))
    }

  // 10.18 '자루(bag)'라는 자료구조는 집합처럼 각 요소를 하나씩만 담되, 그 요소의 출현 횟수도 기억한다.
  // 구체적으로 자루는 각 요소가 키이고 그 요소의 출현 횟수가 값인 Map으로 표현된다. 다음은 자루의 예이다
  //
  // scala> bag(Vector("a", "rose", "is", "a", "rose"))
  // res0: Map[String, Int] = Map(a -> 2, rose -> 2, is -> 1)
  //
  // 모노이드들을 이용해서 IndexedSeq로부터 '자루'를 계산하는 함수를 작성하라
  def bag[A](as: IndexedSeq[A]): Map[A, Int] =
    foldMapV(as, mapMergeMonoid[A, Int](intAddition))((a: A) => Map(a -> 1))
}
