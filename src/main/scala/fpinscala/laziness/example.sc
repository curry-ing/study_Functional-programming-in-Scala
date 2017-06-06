import fpinscala.laziness._

def if2[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A =
  if(cond) onTrue() else onFalse()

val a = 3
if2(a < 22, () => println("a"), () => println("b"))

def if3[A](cond: Boolean, onTrue: => A, onFalse: => A):A =
  if(cond) onTrue else onFalse

if3(a > 22, println("a"), println("b"))
if3(cond = false, sys.error("fail"), 3)

def maybeTwice(b: Boolean, i: => Int) = if (b) i + i else 0

val x = maybeTwice(true, { println("hi"); 1 + 41})

def maybeTwice2(b: Boolean, i: => Int) = {
  lazy val j = i
  if (b) j + j else 0
}

val y = maybeTwice2(true, { println("hi"); 1 + 41})


1 +: List(2, 3)
