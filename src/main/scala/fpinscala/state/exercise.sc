import fpinscala.state.RNG
import fpinscala.state.RNG.SimpleRNG

val rng = SimpleRNG(42) // set 42 as seed number

val (n1, rng2) = rng.nextInt

val (n2, rng3) = rng2.nextInt

Int.MaxValue / (Int.MaxValue.toDouble + 1.0)
Int.MinValue.toDouble


Int.MaxValue + 1
Int.MaxValue.toDouble
Int.MaxValue.toDouble + 1

RNG.nonNegativeInt(rng)

