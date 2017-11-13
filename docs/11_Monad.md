# 11. 모나드 
## 11.1 Functor: `map`함수의 일반화 
- map: 인수 하나를 받는 함수를 다른 자료형식의 *문맥으로 끌어올림* (lift a function)

```scala
def map[A, B](ga: Gen[A])(f: A => B): Gen[B]
def map[A, B](pa: Parser[A])(f: A => B): Parser[B]
def map[A, B](oa: Option[A])(f: A => B): Option[B]
```
```scala
trait Functor[F[_]] {
  def mappA, B](fa: F[A])(f: A => B): F[B]
}
```
- `map`을 형식 생성자 `F[_]`로 매개변수화함 

```scala
val listFunctor = new Functor[List] {
  def map[A, B](as: List[A])(f: A => B): List[B] = as map f
}
```
- `List`같은 형식 생성자 => **Functor**
- `Functor[F]`인스턴스는 `F`가 실제 하나의 함수자임을 증명

#### 무엇을 할 수 있을까? 
- 인터페이스의 연산들을 대수적인 방식으로 **다루는** 것만으로 유용한 함수를 발견 가능 

##### `def distribute`
```scala
trait Functor[F[_]] {
  ...
  def distribute[A, B](fab: F[(A, B)]): (F[A], F[B]) = 
    (map(fab)(_._1), map(fab)(_._2))
}
```
- `F`는 펑터 => `F[(A, B)]`가 주어졌을 때, `F`를 쌍(pair)에 분배하여 `(F[A], F[B])` 획득 가능 
- `List`, `Gen`, `Option`등 구체적인 자료 형식에서 어떤 의미인가? 
	- `distribute`적용시 같은 길이의 목록 두 개 산출 (Unzip)
	- **List** 뿐 아니라 모든 함수자에 대해 작동

##### `def codistribute`
```scala
def codistribute[A, B](e: Either[F[A], F[B]]): F[Either[A, B]] = e match {
  case Left(fa) => map(fa)(Left(_))
  case Right(fb) => map(fb)(Right(_))
}
```
- `distribute`가 곱 연산(product)으로 인해 가능했다면, 합이나 쌍대곱(coproduct)에 대해 이와 반대되는 연산인 `codistribute` 작성 가능 
- 의미: `A`에 대한 생성기나 `B`에 대한 생성기가 있을 때, 둘 중 어떤것이 주어지느냐에 따라 `A`, `B` 중 하나를 생성하는 생성기 작성 가능

### 11.1.1 Functor 의 법칙들
#### Importance of Laws
- 인터페이스의 의미론(semantic)을, 해당 대수의 인스턴스들과는 **독립적으로** 추론할 수 있을 정도의 새로운 수준으로 끌어올리는 데 도움이 된다 
	- `Monoid[A]`와 `Monoid[B]`의 곱으로 `Monoid[(A, B)]`가 생성되었을 때 _융합된_ 모노이드 역시 결합법칙을 만족한다고 추론 가능(`A`, `B`에 대해 아무것도 모르더라도!)
- `Functor`같은 추상 인터페이스의 함수들로부터 조합기 파생시 **법칙**에 의존하는 경우가 많음 

#### Functor `Par`'s Identity laws
```scala
map(x)(a => a) == x
```
- `map(x)`가 `x`의 **구조를 보존해야 한다**는 요구사항 반영 
	- 예외 발생, `List`의 요소를 제거, `Some`을 `None`으로 변경 등의 행동을 하면 안 됨 
	- `map`은 오직 자료구조의 요소들만 수정
- `List`, `Option`, `Par`, `Gen`을 비롯한 `map`을 정의하는 대부분에 대해 성립

##### `distribute` & `codistribute`
```scala
def distribute[A, B](fab: F[(A, B)]): (F[A], F[B])
def codistribute[A, B](e: Either[F[A], F[B]]): F[Either[A, B]]
```
- `F`가 Functor 라는 사실 외에는 아는바가 없음
- 반환된 값들이 그 인수들과 같은 형태임을 보장 
- **대수적 추론**: 속성마다 개별적 검사 불필요

## 11.2 Monad: `flatMap`과 `unit`의 일반화 
#### Limitation of Functor
- `map`만으로 조합 가능한 연산이 별로 많지 않음 
#### Advantage of Monad
- 코드의 중복을 줄일 수 있는 유용한 연산들을 다수 생성 가능 
- 라이브러리가 기대한 대로 작동하는지 추론하는데 사용할 수 있는 법칙도 보유 

##### `map2`s in `Gen`, `Parser` and `Option`
```scala
def map2[A, B, C](fa: Gen[A], fb: Gen[B])(f: (A, B) => C): Gen[C] = 
  fa flatMap (a => fb map(b => f(a, b)))
def map2[A, B, C](fa: Parser[A], fb: Parser[B])(f: (A, B) => C): Parser[C] = 
  fa flatMap (a => fb map(b => f(a, b)))
def map2[A, B, C](fa: Option[A], fb: Option[B])(f: (A, B) => C): Option[C] = 
  fa flatMap (a => fb map(b => f(a, b)))
```
- 공통점이 없어 보이는 서로 다른 자료형식에 작용하나 구현이 모두 동일함 
- 좀 더 일반적인 패턴으로 확장 가능 

### 11.2.1 Monad 특질 
```scala
trait Mon[F[_]] {
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] = 
    fa flatMap(a => fb map( b=> f(a, b)))
}
```
- 구체적 자료형 대신 `F`로 대체해 사용 
- `F`가 무엇인지 모르므로, `F[A]`에 대해 `flatMap`이나 `map`의 적용방법을 모름: _컴파일 불가_

#### `map`과 `flatMap`을 `Mon`에 추상적인 형태로 추가 
```scala 
trait Mon[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] = 
    fa flatMap(a => fb map( b=> f(a, b)))
}
```
- `map2`가 호출하는 모든 함수(`map` & `flatMap`)를 인터페이스의 추상 메서드로 추가
-  다소 기계적 변경, refactoring 여지 있음 

##### Refactoring: `map` & `flatMap`에 대한 **기본수단** 찾아보기
```scala
def map[A, B](f: A => B): Gen[B] = flatMap(a => unit(f(a))
```
- `map`: `flatMap`과 **`unit`**을 이용해 구현 가능
- 최소한의 기본수단 집합: `flatMap` & `unit`

#### Monad 특질 정의
```scala
trait Monad[F[_]] extends Functor[F] {
  def unit[A](a: => A): F[A]
  def flatMap[A, B](ma: F[A])(f: A => F[B]): F[B]
  def map[A, B](ma: F[A])(f: A => B): F[B] = 
    flatMap(ma)(a => unit(f(a)))
  def map2[A, B, C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] = 
    flatMap(ma)(a => map(mb)(b => f(a, b)))
```
- 위 `Mon`의 함수들이 정의한 모든 자료형식을 하나의 개념으로 종합 
- 이름을 `Monad`라 명명 
- `flatMap`, `unit`을 기본 수단으로 `map`, `map2`역시 구현하여 제공 

##### `Gen`을 위한 Monad 구현 
```scala
object Monad {
  val genMonad = new Monad[Gen] {
    def unit[A](a: => A): Gen[A] = Gen.unit(a)
    def flatMap[A, B](ma: Gen[A])(f: A => Gen[B]): Gen[B] = ma flatMap f  
  }
}
```
- `unit`과 `flatMap`만 구현하면 
- `map`, `map2`는 trait에서 구체적으로 구현하고 있기 때문에 알아서 생성 
  
>  **Monad 라는 용어**  
> 수학의 한 분야인 **범주론**에서 비롯됨  
> **Monad**는 의도적으로 **Monoid**와 비슷하게 만들어진 용어이며, 두 개념 사이에는 깊은 연관관계 존재 
