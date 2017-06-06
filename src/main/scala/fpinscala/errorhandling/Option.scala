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
//    flatMap(a => if(f(a)) Some(a) else None)
}

case class Some[+A](get: A) extends Option[A]
case object None extends Option[Nothing]


