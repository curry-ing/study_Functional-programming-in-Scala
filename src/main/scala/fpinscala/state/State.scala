package fpinscala.state

import State._

case class State[S, +A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] = {
//    val (a, s1) = this.run
//    State((f(a), _))
    flatMap(a => unit(f(a)))
  }

  def map2[B, C](sb: State[S, B])(f: (A, B) => C): State[S, C] = {
//    val (a, s1) = this.run
//    val (b, s2) = sb.run
//    State((f(a, b), _))
    flatMap(a => sb.map(b => f(a, b)))
  }

  def flatMap[B](f: A => State[S, B]): State[S, B] = {
    State(s => {
      val (a, s1) = run(s)
      f(a).run(s1)
    })
  }

}

object State {
  type Rand[A] = State[RNG, A]

  def unit[S, A](a: A): State[S, A] = State(s => (a, s))


  def sequence[S, A](fs: List[State[S, A]]): State[S, List[A]] = {
    def go(s: S, orig: List[State[S, A]], rslt: List[A]): (List[A], S) = orig match {
      case Nil => (rslt.reverse, s)
      case h :: t => h.run(s) match { case(a, s1) => go(s1, t, a :: rslt) }
    }
    State(s => go(s, fs, Nil))
  }

  def sequenceViaFoldRight[S, A](fs: List[State[S, A]]): State[S, List[A]] = {
    fs.foldRight(unit[S, List[A]](Nil))((f, acc) => f.map2(acc)(_ :: _))
  }

  def sequenceViaFoldLeft[S, A](fs: List[State[S, A]]): State[S, List[A]] = {
    fs.reverse.foldLeft(unit[S, List[A]](Nil))((acc, f) => f.map2(acc)(_ :: _))
  }

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))

  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get
    _ <- set(f(s))
  } yield ()
}

sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)

object Candy {
  def update = (i: Input) => (s: Machine) => (i, s) match {
    case (_, Machine(_, 0, _)) => s         // 사탕이 없는 판매기는 모든 입력을 무시
    case (Coin, Machine(false, _, _)) => s  // 풀린 판매기에 동전을 넣으면 아무일도 생기지 않음
    case (Turn, Machine(true, _, _)) => s   // 잠긴 판매기의 손잡이를 돌리면 아무일도 일어나지 않음
    case (Coin, Machine(true, candy, coin)) => Machine(locked = true, candy, coin + 1)  // 잠긴 판매기에 동전을 넣으면 사탕이 남아있는 경우 잠김이 풀림
    case (Turn, Machine(false, candy, coin)) => Machine(locked = true, candy - 1, coin) // 풀린 판매기의 손잡이를 돌리면 사탕이 나오고 판매기가 잠김
  }

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = for {
    _ <- sequence(inputs.map(modify[Machine] compose update))
    s <- get
  } yield (s.coins, )
}