import fpinscala.errorhandling._

// 4.2
def variance(xs: Seq[Double]): Option[Double] = { //: Option[Double] = {
  def getMean(ls: Seq[Double]) = ls match {
    case Nil => None
    case _ => Some(ls.sum / ls.size)
  }
  getMean(xs) flatMap(m => getMean(xs.map(x => math.pow(x - m, 2))))
}

val a = Seq[Double](1, 3, 4, 5)
variance(a)

// 4.3
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = (a, b) match {
  case (None, _) => None
  case (_, None) => None
  case _ => Some(f(a, b))
}

def map3[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = {
  a flatMap(x => b map(y => f(x, y)))
}

// 4.4
def sequence[A](a: List[Option[A]]):Option[List[A]] = a match {
  case Nil => Some(Nil)
  case h :: t => h.flatMap(x => sequence(t).map(x :: _))
}

// 4.5
def traverse[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = a match {
  case Nil => Some(Nil)
//  case h :: t => map2(f(h), traverse(t)(f))(_ :: _)
  case h :: t => f(h).flatMap(a => traverse(t)(f) map(a :: _))
}

// 4.7
def sequence2[E, A](es: List[Either[E, A]]): Either[E, List[A]] = es match {
  case h :: t => h.flatMap((a: A) => Right(a :: sequence2(t).r))
}

//def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???

