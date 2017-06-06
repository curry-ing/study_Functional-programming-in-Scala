package fpinscala.errorhandling

case class Person(name: Name, age: Age)

sealed class Name(val value: String)
sealed class Age(val value: Int)


object Person {
  def mkName(name: String): Either[String, Name] =
    if (name == "" || name == null) Left("Name is Empty")
    else Right(new Name(name))

  def mkAge(age: Int): Either[String, Age] =
    if (age < 0) Left("Age is out of range")
    else Right(new Age(age))

  def mkPerson(name: String, age: Int): Either[String, Person] =
    mkName(name).map2(mkAge(age))(Person(_, _))

  /* 4.8
  이 구현에서 map2는 이름과 나이가 모두 유효하지 않을 때에도 오류를 하나만 보고할 수 있다.
  두 오류를 모두 보고하게 하려면 어떻게 고쳐야 할까? map2를 바꾸는 것이 좋을까? 아니면 mkPerson의 서명을 바꾸는 것이 좋을까?
  아니면 이러한 요구사항을 Either 보다 더 잘 만족하는 새로운 자료 형식을 만들 수도 있을 것이다.
  그러한 자료 형식에 대해 orElse, traverse, sequence는 다르게 행동할까?
   */
}
