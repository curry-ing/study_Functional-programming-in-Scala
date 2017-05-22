def findFirst(ss: Array[String], key: String): Int = {
  def loop(n: Int): Int =
    if (n >= ss.length) -1
    else if (ss(n) == key) n
    else loop(n + 1)
  loop(0)
}

def findFirst[A](as: Array[A], p: A => Boolean): Int = {
  def loop(n: Int): Int =
    if (n >= as.length) -1
    else if (p(as(n))) n
    else loop(n + 1)
  loop(0)
}

findFirst(Array(7, 9, 13), (x: Int) => x == 9)

(x: Int, y: Int) => x == y

