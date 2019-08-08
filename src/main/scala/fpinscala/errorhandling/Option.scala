package fpinscala.errorhandling

//import scala.{Option => _, Either => _, _}

/* 연습문제 4.1
Option에 대한 함수들을 모두 구현할 것. 각 함수를 구현할 때 그 함수가 어떤 일을 하고 어떤 상황에서 쓰일 것인지 생각해 볼 것.

- 패턴 매칭을 사용 가능하지만, map과 getOrElse를 제외한 모든 함수는 패턴 매칭 없이도 구현 할 수 있어야 함
- map과 flatMap의 형식 서명은 구현을 결정하기에 충분해야 한다
- getOrElse는 Option의 Some안의 결과를 돌려준다. 단, Option이 None이면 주어진 기본값을 돌려준다
- orElse는 첫 Option이 정의되어 있으면 그것을 돌려주고, 그렇지 않으면 둘째 Option을 돌려준다
 */

sealed trait Option[+A] {
  def map[B](f: A => B): Option[B] = this match {
    case None => None
    case Some(a) => Some(f(a))
  }

  def getOrElse[B >: A](default: => B): B = this match {
    case None => default
    case Some(a) => a
  }

  // 이건 무슨 의미가 있는 함수일까? map이랑 뭐가 다름??
  def flatMap[B](f: A => Option[B]): Option[B] =
    map(f).getOrElse(None)

  def orElse[B >: A](ob: => Option[B]): Option[B] =
    this.map(Some(_)).getOrElse(ob)

  def filter(f: A => Boolean): Option[A] =
    this match {
      case None => None
      case Some(a) => if (f(a)) Some(a) else None
    }
}

case class Some[+A](get: A) extends Option[A]

case object None extends Option[Nothing]

object Option {
  // 4.3 두 Option값을 이항 함수(binary function)를 이용해서 결합하는 일반적인 함수 map2를 작성하라
  // 두 Option중 하나라도 None이면 map2의 결과 역시 None이어야 함
  def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
    a flatMap(x => b.map(y => f(x, y)))

  // 4.4 Option들의 목록을 받고 그 목록에 있는 모든 Some 값으로 구성된 목록을 담은 `Option`을 돌려주는 함수 `sequence`를 작성
  // 원래의 목록에 None이 하나라도 있으면 함수의 결과도 None이어야 한다.
  // 그렇지 않으면 원래의 목록에 있는 모든 값의 목록을 담은 Some을 반환할 것
  def sequence[A](a: List[Option[A]]): Option[List[A]] = a match {
    case Nil => Some(Nil)
    case h :: t => h.flatMap(x => sequence(t).map(x :: _))
  }

  // 4.5 목록을 순회하며 동시에 각 요소에 함수를 매핑하는 traverse를 구현할 것
  // 또한 sequence를 traverse를 이용해 구현 해 볼 것
  def traverse[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = a match {
    case Nil => Some(Nil)
    case h :: t => map2(f(h), traverse(t)(f))(_ :: _)
//    case h :: t => f(h).flatMap(a => traverse(t)(f) map (a :: _))
  }

  def sequence2[A](a: List[Option[A]]): Option[List[A]] = traverse(a)(identity)
}
