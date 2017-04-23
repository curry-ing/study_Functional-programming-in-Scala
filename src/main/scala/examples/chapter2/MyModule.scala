package examples.chapter2

class MyModule {
  def abs(n: Int): Int =
    if (n < 0) -n
    else n

  def factorial(n: Int): Int = {
    def go(n: Int, acc: Int): Int =
      if (n <= 0) acc
      else go(n-1, n * acc)
    go(n, 1)
  }

  private def formatAbs(x: Int) =
    s"The absolute value of $x is ${abs(x)}"

  private def formatFactorial(n: Int) =
    s"The factorial of $n is ${factorial(n)}"

  def main(args: Array[String]): Unit = {
    println(formatAbs(-42))
    println(formatFactorial(7))
  }
}

