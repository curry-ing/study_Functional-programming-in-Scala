import fpinscala.laziness._
import fpinscala.laziness.Stream._

def if2[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A =
  if (cond) onTrue() else onFalse()

val a = 3
if2(a < 22, () => println("a"), () => println("b"))

def if3[A](cond: Boolean, onTrue: => A, onFalse: => A): A =
  if (cond) onTrue else onFalse

if3(a > 22, println("a"), println("b"))
if3(cond = false, sys.error("fail"), 3)

def maybeTwice(b: Boolean, i: => Int) = if (b) i + i else 0

val x = maybeTwice(true, {
  println("hi");
  1 + 41
})

def maybeTwice2(b: Boolean, i: => Int) = {
  lazy val j = i
  if (b) j + j else 0
}

val y = maybeTwice2(true, {
  println("hi");
  1 + 41
})


//
val ones: Stream[Int] = Stream.cons(1, ones)

ones.take(5).toList
ones.exists(_ % 2 != 0)
ones.map(_ + 1).exists(_ % 2 == 0)
ones.takeWhile(_ == 1)
ones.forAll(_ != 1)


// 5.8 ones를 조금 일반화해서 주어진 값의 무한 Stream을 돌려주는 함수 constant를 구현
def constant[A](a: A): Stream[A] = Stream.cons(a, constant(a))

constant("MA").take(5).toList


// 5.9 n에서 시작해서 n+1, n+2 등으로 이어지는 무한 정수 스트림을 생성하는 함수를 작성하라
def from(n: Int): Stream[Int] = Stream.cons(n, from(n + 1))

from(5).take(5).toList

// 5.10 무한 피보나치 수 0, 1, 1, 2, 3, 5, 8, ... 으로 이루어진 무한 스트림을 생성하는 함수 fibs를 작성하라
val fibs = {
  def go(a: Int, b: Int): Stream[Int] = {
    Stream.cons(a, go(b, a + b))
  }

  go(0, 1)
}

fibs.take(10).toList


unfold(1)((x: Int) => Some((x, x + 3))) take 5 toList

//unfold((0, 1))((x: Int, y: Int) => Some((x, (y, x + y)))) take 5


// 5.12 unfold를 이용하여 fibs. from, constant, ones를 작성
val fibs2 = unfold((0, 1)) { case (i, b) => Some(i, (b, i + b)) }

fibs2 take 10 toList

def from2(n: Int) = unfold(n)(n => Some(n, n+1))

from2(10) take 5 toList

def constant2(n: Int) = unfold(n)(x => Some(x, x))

constant2(5) take 10 toList

val ones2 = unfold(1)(x => Some(x, x))

ones2 take 5 toList
