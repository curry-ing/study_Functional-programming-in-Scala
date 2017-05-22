/**
  * 2.1 n번째 피보나치 수를 돌려주는 재귀함수를 작성하라.
  * - 0, 1 로 시작.
  * - 반드시, 꼬리재귀 사용
  */

def fib(n: Int): Int = {
  def loop(a: Int, b: Int, c: Int): Int = {
    if (n == a) b
    else loop(a+1, c, b+c)
  }
  loop(0, 0, 1)
}

// 0, 1, 1, 2, 3, 5, 8, 13

val fibRslt = Array(fib(0), fib(1), fib(2), fib(3), fib(4), fib(5), fib(6), fib(7))


/**
  * 2.2 Array[A]가 주어진 비교 함수에 의거해 정렬되어 있는지 정렬하는 `isSorted`구현.
  */
def isSorted[A](as: Array[A], ordered: (A, A) => Boolean): Boolean = {
  def loop(list: Array[A]): Boolean = {
    if (list.length < 2) true
    else {
      if (!ordered(list(0), list(1))) false
      else loop(list.tail)
    }
  }
  loop(as)
}

val Array(x, y) = fibRslt.take(2)

def orderedImpl(a: Int, b: Int):Boolean = a <= b

isSorted(fibRslt, orderedImpl)
isSorted(Array(), orderedImpl)
isSorted(Array(1,0,2,4,3,6), orderedImpl)


/**
  * 2.3 인수가 두 개인 함수 `f`를 인수 하나를 받고 그것으로 `f`를 부분 적용하는 함수로 변환하는 **커링**(currying)을 구현
  */
def curry[A, B, C](f: (A, B) => C): A => (B => C) =
  (a: A) => (b: B) => f(a, b)

/**
  * 2.4 curry의 변환을 역으로 수행하는 고차 함수 uncurry를 구현
  * `=>`는 오른쪽으로 묶이므로 `A => (B => C)`를 `A => B => C`라고 표기할 수 있음을 주의
  */
def uncurry[A, B, C](f: A => B => C): (A, B) => C = ???

/**
  * 2.5 두 함수를 합성하는 고차 함수를 구현
  */
def compose[A, B, C](f: B => C, g: A => B): A => C = ???

val test = Array(1, 2, 3, 4)
test.groupBy(_.toString)