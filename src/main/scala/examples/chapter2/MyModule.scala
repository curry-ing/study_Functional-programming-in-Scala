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

  def formatResult(name: String, n: Int, f: Int => Int) =
    s"The $name of $n is ${f(n)}"

  def main(args: Array[String]): Unit = {
    formatResult("absolute value", -42, abs)
    formatResult("factorial", 7, factorial)
  }
}

