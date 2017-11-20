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

## 11.3 Monadic Combinators
#### `listOfN` => `replicateM`
- 파서 또는 생성기를 n번 되풀이해서 그 길이의 입력을 인식하는 파서 또는 그 개수만큼의 목록들을 생성하는 생성기를 얻는 데 사용 
- Monad trait에 추가하여 모든 모나드 `F`에 대해 구현 가능 
- 좀 더 일반적인 이름 부여 => `replicateM` (replicate in a monad)
```scala
def replicateM[A](n: Int, ma: F[A]): F[List[A]]
```

#### `product`
- `Gen`, `Par`에서 구현한 조합기
	- `map2`를 이용하여 구현 
- 주어진 생성기 두 개로 쌍들을 생성하는 하나의 생성기 리턴
```scala
def product[A, B](ma: F[A], mb: F[B]): F[(A, B)] = 
  map2(ma, mb)((_, _))
```

## 11.4 Monad Laws
- Functor의 법칙들은 당연히 포함 : `Monad[F]`가 `Functor[F]`에 포함관계 
- `flatMap`과 `unit`을 규정하는 법칙

### 11.4.1 결합법칙
#### `genOrder` 예제 
```scala
case class Order(item: Item, quantity: Int)
case class Item(name: String, price: Double)

val genOrder: Gen[Order] = for {
  name <- Gen.stringN(3)  // 길이가 3이 무작위 문자열
  price <- Gen.uniform.map(_ * 10) // 0~10 사이 균등분포 난수 생성
  quantity <- Gen.choose(1, 100) // 1~100사이 Int난수
} yield Order(Item(name, price), quantity)
```
- 즉석에서 `Item`을 생성
- 개별적으로 `Item`을 생성할 필요가 있을 땐?

##### `Item`을 위한 생성기 
```scala 
val genItem: Gen[Item] = for {
  name <- Gen.stringN(3)
  price <- Gen.uniform.map(_ * 10)
} yield Item(name, price)
```

##### `Item`을 이용한 `genOrder`
```scala
val genOrder: Gen[Order] = for {
  item <- genItem
  quantity <- Gen.choose(1, 100)
} yield Order(item, quantity)
```

##### `genOrder`의 두 구현을 `map`과 `flatMap`으로 전개해 결과 비교
**구현 1**
```scala
Gen.nextString.flatMap(name => 
  Gen.nextDouble.flatMap(price => 
    Gen.nextInt.map(quantity => 
      Order(Item(name, price), quantity))))
```

**구현 2**
```scala 
Gen.nextString.flatMap(name => 
  Gen.nextInt.map(price =>
    Item(name, price))).flatMap(item => 
      Gen.nextInt.map(quantity >
        Order(item, quantity)))
```

- 두 구현이 정확히 동일하지 않음 
- for-comprehension 을 살펴보면 두 구현이 정확히 동일한 일을 하리라고 가정하는것이 합당해 보임 
- `flatMap`은 결합법칙을 만족 
```scala
x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
```
- 이 법칙은 해당 형식의 모든 `x`, `f`, `g`에 대해 성립해야 함.

### 11.4.2 특정 모나드의 결합법칙 성립 증명 
##### `x`를 `None`으로 치환 
```scala
None.flatMap(f).flatMap(g) == None.flatMap(a => f(a).flatMap(g))
```
- `None.flatMap(f)` => 모든 `f`에 대해 `None`이므로 등식 성립 

##### ` x`를 `Some(v)`로 치환 
```scala
x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
Some(v).flatMap(f).flatMap(g) == Some(v).flatMap(a => f(a).flatMap(g))
f(v).flatMap(g) == (a => f(a).flatMap(g))(v)
f(v).flatMap(g) == f(v).flatMap(g)
```

- `x`가 `None`이거나 `Some(v)`이거나 둘 다 성립함이 증명됨 
- `Option`의 가능성은 위 두가지(`Some` or `None`)이므로, 이 법칙은 `Option`에 대해 항상 성립함이 증명됨 


#### 크라이슬리 합성: 결합법칙에 관한 좀 더 명확한 관점 
##### 모노이드의 결합법칙
```scala
op(op(x, y), z)) == op(x, op(y, z))
```
- 직관적이며 명확해 이해가 쉬움 

##### 크라이슬리 화살표
- `F[A]`와 같은 형식의 Monadic value 가 아닌, `A => F[B]`같은 모나드적 **함수** 로 설명
- 위 종류의 함수를 **크라이슬리 화살표**(Kleisli arrow)라고 부름 
```scala
def compose[A, B, C](f: A => F[B]. g: B => F[C]): A => F[C]
```

##### 모나드 결합법칙을 크라이슬리 화살표로 확인 
```scala
compose(compose(f, g), h)) == compose(f, compose(g, h))
```

### 11.4.3 항등법칙
#### `unit`
-  Monoid 의 `append`에 대한 항등원 `zero`가 존재하듯, **`compose`**에 대한 항등원 존재 

```scala
def unit[A](a: => A): F[A]
```
- 적절한 형식을 `compose`의 인수로 전달 
- 임의의 Monad와 `unit`의 합성은 원래의 모나드와 동일 
-  **왼쪽 항등법칙**, **오른쪽 항등법칙**으로 표현
```scala
compose(f, unit) == f
compose(unit, f) == 
```

##### 위 항등법칙을 `flatMap`을 통해 표현 가능 
```scala 
flatMap(x)(unit) == x
flatMap(unit(y))(f) == f(y)
```

## 11.5 도대체 모나드란 무엇인가? 
### 인터페이스
- 어떤 추상 자료 형식에 대한 비교적 완비된 API를 제공하는 무엇.
- Linked List와 Array는 내부적인 구현은 다르지만 Application 입장에서는 공통의 **인터페이스** 공유 

- Monoid와 같이 Monad는 좀 더 추상적이고 대수적인 **인터페이스**
- Monad 조합기들은 주어진, 그리고 모나드가 될 수 없는 자료형식에 대한 전체 API중 일부만 차지 
- 한 두 형식을 일반화하는 것이 아니라, Monad 인터페이스와 법칙을 만족하는 **아주 다양하고 많은 형식을 일반화**

### 모나드란
> **Monad**는 **Monad적 조합기들의 최소 집합 중 하나**를 **결합법칙과 항등법칙을 만족**하도록 구현한 것 

#### Monad 조합기들의 최소 집합 
- `unit` & `flatMap`
- `unit` & `compose`
- `unit`, `map`, `join`
- 위 세 집합 중 하나 구현 필요 

#### 만족해야 할 Monad 법칙
- 결합법칙
- 항등법칙 

### 11.5.1 항등 모나드 (identity monad)
```scala
case class Id[A](value: A)
```
- 간단한 wrapper
- `Id`를 `A`에 적용하는 일 외에 딱히 하는 일 없음 

##### `Id(A)` 는 항등연산
- 감싸인 형식(`A`)이 감싸이지 않은 형식과(`A`) 완전 **동형**(isomorphic)
- 한 형식에서 다른 형식으로 갔다가 다시 원래의 형식으로 돌아와도 정보가 전혀 소실되지 않는다 

#### 항등 모나드의 의미
##### flatMap
```scala
Id("Hello, ") flatMap (a => 
  Id("monad!") flatMap ( b =>
    Id(a + b)

// res0: Id[Java.lang.String] = Id(Hello, monad!)
```

##### for-comprehension
```scala
for {
  a <- Id("Hello, ")
  b <- Id("monad!")
  } yield a + b

// res1: Id[java.lang.String] = Id(Hello, Monad!)
```
- 항등 모나드에 대한 **`flatMap` 동작**은 그저 변수 치환 
- 변수 `a`, `b`가 각각 "Hello, " 와 "monad!" 에 묶인 후 `a + b`에 대입 

##### scala variable
```scala 
val a = "Hello, "
val b = "monad!" 
a + b 

// res2: java.lang.String = Hello, monad!
```
- `Id` 래퍼 말고는 차이가 없음 
- **모나드는 변수의 도입과 결속(binding), 그리고 변수 치환 수행을 위한 문맥 제공**

### 11.5.2 State 모나드와 부분 형식 적용 
##### `State` 
```scala
case class State[S, A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] = 
    State(s => {
      val (a, s1) = run(s)
      (f(a), s1)
    })
  def flatMap[B](f: A => State[S, B]): State[S, B] = 
    State(s => {
      val (a, s1) = run(s)
      f(a).run(s1)
    })
}
```
- 형태로 볼 때 `State`는 모나드가 되기 충분해 보임 
- 단 `State`의 형식 지정자가 형식 인수 두 개를 받지만(`State[S, A]`) Monad는 인수가 하나인 형식 생성자를 요구 
- 하지만, 어떤 구체적인 `S`에 대해 `State[S, _]`는 Monad가 받아들일 수 있는 형태 
	- `State`에 단 하나의 모나드 인스턴스가 있는 것이 아니라, 여러 인스턴스들(구체적으로, `S`마다 하나씩)의 부류가 존재 
	- 형식 인수 `S`가 어떤 구체적인 형식으로 고정되도록 **`State`를 부분적으로 적용** 가능할까? 

#### 부분 형식 적용 
```scala
type IntState[A] = State[Int, A]
```

```scala
object IntStateMonad extends Monad[IntState] {
  def unit[A](a : => A): IntState[A] = State(s => (a, s))
  def flatMap[A, B](st: IntState[A])(f: A => IntState[B]): IntState[B] = st flatMap f 
}
- 구체적인 상태 형식마다 이런 개별적 Monad 인스턴스를 작성해야 한다면? -> **비효율적**
- 익명 함수에서 underscore의 사용과 비슷하게 **형식 수준에서 람다 비슷한 어떤 것** 존재

#### 형식 람다(type lambda)
```scala
object intStateMonad extends
  Monad[({type IntState[A] = State[Int, A]})#IntState] {
    ...
}
```
- 괄호 내에서 익명 형식을 선언 
- `#`를 이용해서 `IntState` 멤버에 접근 가능 (객체의 멤버에 접근시 `.`를 사용하는 것처럼...)
- `State` 형식 생성자를 부분 적용 후 하나의 `StateMonad` 특질을 선언 가능 
	- `StateMonad[S]`의 인스턴스는 주어진 상태 형식 S에 대한 **모나드 인스턴스**

```scala
def stateMonad[S] = new Monad[({type f[x] = State[S, x]})#f] { 
  def unit[A](a: => A): State[S, A] = State(s => (a, s))
  def flatMap[A, B](st: State[S, A])(f: A => State[S, B]): State[S, B] = st flatMap f
}
```
- `unit`, `flatMap`의 구현과 마찬가지로 이 구현은 다른 모나드적 조합기에도 유용

#### `Id`모나드와 `State`모나드의 차이점 
##### `State`에 대한 기본수단 연산 
- getState: `def getState[S]: State[S, S]` - 현재 상태를 읽음
- setState: `def setState[S](s: => S): State[S, Unit]` - 새 상태를 설정 
- 위 조합기들이 `State`에 대한 기본수단들의 최소 집합을 형성 
- 이들과 모나드적 기본수단(`unit`, `flatMap`)은 `State` 자료 형식으로 할 수 있는 모든 것을 **완전히 명시**
- **모든 모나드는 `unit`과 `flatMap`이 있으며, 모나드마다 자신이 하는 일에 특화된 추가적인 기본수단 연산이 존재** 

##### `State`모나드에 대한 위 기본연산들의 의미
```scala
val F = stateMonad[Int]

def zipWithIndex[A](as: List[A]): List[(Int, A)] = 
  as.foldLeft(F.unit(List[(Int, A)]()))((acc, a) => for {
    xs <- acc 
    n <- getState
    _ <- setState(n + 1)
  } yield (n, a) :: xs).run(0)._1.reverse
```
- `State` 동작을 이용하여 목록 안의 모든 요소를 카운트 
	- 0에서 시작해 매번 증가하는 `Int`형식의 상태를 유지 
-  애초에 역순으로 구축되었기 떄문에 결과가 나오면 그것을 reverse
- for-comprehension 안의 `getState`와 `setState`
	- `Id`모나드에서처럼 변수를 묶음
	- 일련의 상태 동작들의 값 바인딩 (`getState` -> `acc` -> `setState`) 
	- 행간: **`flatMap`의 구현은 현재 상태가 `getState`에 주어지며, 새 상태가 `setState`다음의 모든 동작으로 전파됨을 보장**

##### `flatMap`호출 연쇄의 의미
- 변수에 값을 배정하는 명령문들로 이루어진 명령식 프로그램과 유사
- **모나드는 각 명령문의 경계에서 어떤 일이 일어나는지 명시**
	- `Id`: `Id`생성자 안의 래핑 풀기와 다시 래핑하기 이외에 아무 일도 일어나지 않음 
	- `State`: 가장 최근의 상태가 한 명령문에서 다음 명령문으로 전달 
	- `Option` 모나드: 명령문이 `None`을 반환해 프로그램 종료 가능 
	- `List` 모나드: 명령문이 여러 결과를 돌려줄 수 있으며 그 다음 명령문들이 여러 번(결과당 한 번씩) 실행 가능

> Monad 계약이 행간에서 **무엇이** 일어나는지 명시하지는 않지만,  
어떤 일이 **일어나든지** 그것이 **결합법칙**과 **항등법칙**을 만족한다 

## 11.6 요약 
- 이 책에서 여러번 등장한 패턴 하나를 살펴보고 이를 **모나드**라는 단일한 개념으로 통합
- 모나드를 이용하여 언뜻 공통점이 전혀 없는 서로 다른 자료형식들에 대한 여러 조합기 작성 가능 
- 이런 조합기들이 모두 만족하는 법칙들을 다양한 관점에서 고찰
- 모나드의 완전한 이해를 위하여 주제를 서로 다른 관점에서 다시 고찰하는 반복적인 접근방식이 필요 
- **새로운 모나드**나 **모나드의 새로운 용법을 발견**한다면, 혹은 **새로운 문맥에서 모나드가 등장**하는 것을 보게 된다면, 모나드에 대한 새로운 통찰을 얻을 것  
