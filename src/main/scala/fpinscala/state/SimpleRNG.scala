package fpinscala.state

trait RNG {
  def nextInt: (Int, RNG)
}

object RNG {

  case class SimpleRNG(seed: Long) extends RNG {
    def nextInt: (Int, RNG) = {
      val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFL
      val nextRNG = SimpleRNG(newSeed)
      val n = (newSeed >>> 16).toInt
      (n, nextRNG)
    }
  }

  // exercise 6.1 - RNG.nextInt를 이용하여 0 이상, Int.MaxValue 이하의 난수 정수를 생성하는 함수를 작성
  //                nextInt가 Int.MinValue를 돌려주는 구석진 경우(음이 아닌 대응수가 없음)도 확실하게 처리할 것
  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, rng2) = rng.nextInt

    (if (i < 0) (i + 1) * -1 else i, rng2)
  }

  // exercise 6.2 - 0 이상 1 미만의 Double 난수를 발생하는 함수를 작성하라
  //                참고: 최대의 양의 정수를 얻으려면 Int.MaxValue를, x: Int를 Double로 변환하려면 x.toDouble을 사용하면 된다
  def double(rng: RNG): (Double, RNG) = {
    val (i, rng2) = nonNegativeInt(rng)
    (i / (Int.MaxValue + 1.0), rng2)
  }

  // exercise 6.3 - 각각 난수쌍(Int, Double) 하나 (Double, Int) 하나 3튜플(Double, Double, Double)하나를 발생하는 함수 작성
  //                앞에서 작성한 함수들을 재사용하라
  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (i, rng1) = rng.nextInt
    val (d, rng2) = double(rng1)

    (i, d) -> rng2
  }

  def doubleInt(rng: RNG): ((Double, Int), RNG) = {
    val (d, rng1) = double(rng)
    val (i, rng2) = rng1.nextInt

    (d, i) -> rng2
  }

  def double3(rng: RNG): ((Double, Double, Double), RNG) = {
    val (d1, rng1) = double(rng)
    val (d2, rng2) = double(rng1)
    val (d3, rng3) = double(rng2)

    (d1, d2, d3) -> rng3
  }

  // exercise 6.4 - 정수 난수들의 목록을 생성하는 함수를 작성하라
  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
    def go(cnt: Int, rslt: List[Int], r: RNG): (List[Int], RNG) = cnt match {
      case 0 => (rslt, r)
      case _ =>
        val (i, r2) = r.nextInt
        go(cnt - 1, rslt :+ i, r2)
    }

    go(count, Nil, rng)
  }

  type Rand[+A] = RNG => (A, RNG)

  val int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] = rng => (a, rng)

  def map[A, B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def nonNegativeEven: Rand[Int] =
    map(nonNegativeInt)(i => i - 1 % 2)

  // exercise 6.5 - exercise 6.2의 double을 map을 이용해서 좀 더 우아한 방식으로 구현하라
  def doubleViaMap: Rand[Double] =
    map(nonNegativeInt)(i => i / Int.MaxValue + 1.0)

  // exercise 6.6 - 다음 서명에 따라 map2 구현. 이 함수는 두 상태 동작 ra, rb와 이들의 결과를 조합하는 함수 f를 받고,
  //                두 동작을 조합한 새 동작을 반환
  def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
  rng => {
    val (a, rngA) = ra(rng)
    val (b, rngB) = rb(rngA)
    (f(a, b), rngB)
  }

  def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2(ra, rb)((_, _))

  val randIntDouble: Rand[(Int, Double)] = both(int, double)
  val randDoubleInt: Rand[(Double, Int)] = both(double, int)

  // exercise 6.7 - 두 RNG 상태 전이를 조합할 수 있다면 그런 상태 전이들의 목록 전체를 조합하는 것도 가능해야 마땅하다
  //                상태 전이들의 List를 하나의 상태 전이로 조합하는 함수 sequence를 구현하라. 그리고 이 함수를 이용해,
  //                이전에 작성한 ints 함수를 다시 구현하라. ints 함수의 구현에서 x가 n번 되풀이 되는 목록을 만들 일이 있으면
  //                표준 라이브러리 함수 List.fill(n)(x)를 사용해도 좋다
  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] = {
    def go(orig: List[Rand[A]], rslt: Rand[List[A]]): Rand[List[A]] = orig match {
      case Nil => rslt
      case h :: t => go(t, map2(h, rslt)((a, b) => a +: b))
    }

    go(fs, RNG => (Nil, RNG))
  }

  def ints2(count: Int)(rng: RNG): Rand[List[Int]] = sequence(List.fill(count)(int))

  // exercise 6.8 - flatMap을 구현하고 그것을 이용해서 nonNegativeLessThan을 구현하라
  def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r1) = f(rng)
      g(a)(r1)
    }

  def nonNegativeLessThanViaFlatMap(n: Int): Rand[Int] =
    flatMap(nonNegativeInt) {
      i =>
        val mod = i % n
        if (i + (n - 1) - mod >= 0) unit(mod) else nonNegativeLessThanViaFlatMap(n)
    }

  // exercise 6.9 - map과 map2를 flatMap을 이용해서 다시 구현하라.
  //                이것이 가능하다는 사실은 앞에서 flatMap이 map과 map2보다 더 강력하다는 근거가 된다
  def mapViaFlatMap[A, B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(a => unit(f(a)))

  def map2VialFlatMap[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => map(rb)(b => f(a, b)))
}


