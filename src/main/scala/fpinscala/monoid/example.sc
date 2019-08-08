import fpinscala.monoid.Monoid._

val words = List("Hic", "Est", "Index")


val s = words.foldRight(stringMonoid.zero)(stringMonoid.op)
val t = words.foldLeft(stringMonoid.zero)(stringMonoid.op)

