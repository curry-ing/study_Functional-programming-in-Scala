package fpinscala.datastructures

sealed trait Tree[+A]
case class Leaf[A](value: A) extends Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

object Tree {
  // 3.25 트리의 노드, 즉 잎(leaf)과 가지(branch)의 개수를 새는 size 함수 작성
  def size[A](t: Tree[A]): Int = t match {
    case Branch(l, r) => size(l) + size(r) + 1
    case Leaf(a) => 1
  }

  // 3.26 Tree[Int]에서 가장 큰 요소를 돌려주는 함수 maximum을 작성 (x.max(y) 혹은 x max y 로 최댓값 산출)
  def maximum(t: Tree[Int]): Int = t match {
    case Branch(l, r) => maximum(l) max maximum(r)
    case Leaf(a) => a
  }

  // 3.27 root에서 임의의 잎으로의 가장 긴 경로의 길이를 돌려주는 함수 depth를 작성
  def depth[A](t: Tree[A]): Int = t match {
    case Branch(l, r) => (depth(l) max depth(r)) + 1
    case Leaf(a) => 0
  }

  // 3.28 List에 대한 함수 map과 비슷하게 트리의 각 요소를 주어진 함수로 수정하는 함수 map 작성
  def map[A, B](t: Tree[A])(f: A => B): Tree[B] = t match {
    case Branch(l, r) => Branch(map(l)(f), map(r)(f))
    case Leaf(a) => Leaf(f(a))
  }

  // 3.29 size와 maximum, depth, map 의 유사성을 요약하여 일반화한 새 함수 fold 작성 후 size, maximum, depth, map을 새 함수로 재작성
  // 이 fold 함수와 List에 대한 왼쪽 오른쪽 fold 사이의 유사성 파악
  def fold[A, B](t: Tree[A])(f: A => B)(g: (B, B) => B): B = t match {
    case Branch(l, r) => g(fold(l)(f), fold(r)(f))
    case Leaf(a) => f(a)
  }

  def size2[A](t: Tree[A]): Int = fold(t)(a => 1)(_ + _ + 1)

  def maximum2(t: Tree[Int]): Int = fold(t)(identity)(_ max _)

  def depth2[A](t: Tree[A]): Int = fold(t)(a => 0)((a, b) => (a max b) + 1)

  def map2[A, B](t: Tree[A])(f: A => B): Tree[B] =
    fold(t)(a => Leaf(f(a)): Tree[B])((a, b) => Branch(a, b))
}
