import fpinscala.datastructures._
import fpinscala.datastructures.List._

// 3.1
val x: List[Int] = List(1, 2, 3, 4, 5)
val xd: List[Double] = List(1.0, 2.0, 3.0, 4.0, 5.0)

x match {
  case Cons(i, Cons(2, Cons(4, _))) => i
  case Nil => 42
  case Cons(a, Cons(b, Cons(3, Cons(4, _)))) => a + b
  case Cons(h: Int, t: List[Int]) => h + sum(t)
  case _ => 101
}

// 3.2
tail(x)

// 3.3
setHead(0, x)

// 3.4
drop(x, 2)

// 3.5
dropWhileOld(x, (i: Int) => i < 3)
dropWhile(x)(i => i < 3)

// 3.6
init(x)

// 3.7

// 3.8
foldRight(List(1, 2, 3), Nil: List[Int])(Cons(_, _))
// 1 - Cons(1, foldRight(List(2, 3), Nil)(Cons(_, _)))
// 2 - Cons(1, Cons(2, foldRight(List(3), Nil)(Cons(_, _))))
// 3 - Cons(1, Cons(2, Cons(3, foldRight(Nil, Nil)(Cons(_, _)))))
// 4 - Cons(1, Cons(2, Cons(3, Nil)))

// 3.9
length(x)

// 3.10
foldLeft(List(1, 2, 3), Nil: List[Int])((x, y) => Cons(y, x))

// 3.11
sum3(x)
product3(xd)
length2(x)

// 3.12
reverse(x)

// 3.13

// 3.14
append2(x, List(4, 5, 6))


// 3.16
addOne(x)
addOne2(x)

// 3.17
elemToString(xd)

// 3.18
map(x)(a => a * 2)
map(x)(a => s"-$a")

// 3.19
filter(x)(a => a % 2 != 0)

flatMap(x)(a => List(a, a*2))
filterWithFlatMap(x)(a => a % 2 == 0)

addPairs(List(1, 2, 3), List(4, 5, 6))



