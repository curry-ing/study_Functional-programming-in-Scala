# 8. 속성 기반 검사 
- 프로그램 **행동 방식의 서술**과 그를 검사하는 **test case 의 생성**을 분리 
	- 개발자: 검례들에 대한 고수준 제약만 명시
	- 검사도구: 제약을 만족하는 test case 를 자동 생성 & 테스트 

## 8.1 속성 기반 검사의 간략한 소개 
```scala
// ScalaCheck example
val intList = Gen.listof(Gen.choose(0, 100))
val prop = 
  forAll(intList)(ns => ns.reverse.reverse == ns) &&
  forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
val failingProp = forAll(intList)(ns => ns.reverse == ns)
```

```
scala> prop.check
+ OK, passed 100 tests.

scala> failingProp.check
! Falsification after 6 passed tests.
> ARG_0: List(0, 1)
```

####  `Gen[List[Int]]`
- `List[Int]` 형식의 검사 자료를 생성하는 방법을 아는 무엇. 
- 0과 100사이의 난수들로 채워진 여러가지 길이의 목록(**표본**)을 산출 

#### `forAll`
- `Gen[A]` 형식의 생성기와 `A => Boolean`형식의 술어(predicate)를 조합하여 하나의 **속성** 산출 
	- Generator가 생성한 모든 값이 이 술어를 만족해야 함 
	- **반증**(falsification)이 전혀 없어야 true
	- 위 예제에서는 `reverse` 메서드의 정확한 행동에 대한 명세의 _일부_를 형성 

### ScalaCheck의 유용한 추가 기능
#### 검례 최소회(test case minimization)
- 테스트 실패시, 검사에 실패하는 **가장 작은** 케이스에 도달 할 때 까지 더 작은 테스트를 실행
- 찾아낸 최소의 목록을 보고 
#### 전수 검례 생성(exhaustive test case generation)
- `Gen[A]`가 생성 가능한 모든 값의 집합: **정의역**(domain)
- 정의역이 충분히 작은 경우 sampling 대신 전수 테스트 가능 
- 모든 경우에 테스트를 통과 한 경우: **증명** 된 것

## 8.2 자료 형식과 함수의 선택
- 라이브러리에 적합한 자료 형식과 함수들을 반복적으로 찾아 구현

### 8.2.1 API의 초기 버전 
- 메서드의 리턴 타입이 반드시 어떤 형식을 매개 변수로 사용해야 함 

#### `listOf`
```scala
def listOf[A](a: Gen[A]): Gen[List[A]]
```
- 생성할 목록의 크기를 명시하지 않음 
	- 생성기가 특정 크기를 가정하거나 (모든 문맥에 적합한 크기 X, 유연하지 않음)
	- **생성기에게 크기를 알려 줄 수단이 필요** 

#### `listOfN`
```scala
def listOfN[A](n: Int, a: Gen[A]): Gen[List[A]]
```
- 꽤 유용하지만, 크기를 명시적으로 지정할 필요가 **없는** 버전이 더 강력한 조합기일 것 
- **검사를 수행하는 함수가 검례의 크기를 임의 지정 가능** 하다면 -> **검례 최소화** 의 조건 

#### `forAll`
```scala
def forAll[A](a: Gen[A])(f: A => Boolean): Prop
```
- **생성기와 술어의 형식만 맞다면** 그 종류를 구체적으로 알아야 할 필요는 없음

#### `Prop`
```scala
trait Prop {
  def &&(p: Prop): Prop
}
```
- `Gen`과 술어를 묶은 결과를 표현 
- `&&`: 두 `Prop`을 조합 

### 8.2.2 속성의 의미와 API
#### `Prop`
- `forAll`: 속성 생성
- `&&`: 속성 합성 
- `check`: 속성의 점검

##### `check`
- 문제점
	- side-effect 존재 (console printing)
	- 합성의 기저로 사용 불가능 
	- `&&`과 같은 조합이 가능하기 위해선 어떤 의미있는 값을 돌려주는 **순수 함수** 여야 함
- 개선 방향 
	- `check`의 반환값은 속성 검사의 성공 / 실패 여부 파악 가능 필요 

##### 개선 1
```scala
trait Prop { def check: Boolean } 
```
- 테스트의 성공/실패 여부를 `Boolean`으로 반환 

##### 개선 2
```scala
object Prop {
  type SuccessCount = Int
  ...
}
trait Prop { def check: Either[???, SuccessCount] }
```
- `Either`를 사용하여 테스트 성공시, 지금까지 성공한 테스트의 개수를 반환 

##### 개선 3
```scala
object Prop {
  type FailedCase = String
  type SuccessCount = Int
}
trait Prop {
  def check: Either[(FailedCase, SuccessCount), SuccessCount]
}
```
- 테스트 실패의 경우 테스트를 종료하고 현재까지 성공 개수와 실패 결과를 화면에 출력 

### 8.2.3 생성기의 의미와 API
#### `Gen[A]`
```scala
case class Gen[A](sample: State[RNG, A]) {
  def choose(start: Int, stopExclusive: Int): Gen[Int]
  def unit[A](a: => A): Gen[A]
  def boolean: Gen[Boolean]
  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]]
```
- `Gen[A]`가 `A`형식의 값을 생성하는 방법 중 하나: **무작위 생성**
	- `sample: State[RNG, A]`

- **기본수단** 이 되는 연산과 **파생된** 연산을 구분하고,  작지만 강력한 **기본수단**들의 집합의 구현에 노력 

> **주어진 기본수단으로 표현할 수 있는 대상을 찾는 좋은 방법**
> - 표현하고자 하는 구체적인 예제를 선택 & 그에 필요한 가능성을 조합할 수 있는 지 실험 
> - 패턴 인식 -> 패턴을 추출하여 조합기 생성 -> 기본수단 정련

### 8.2.4 생성된 값들에 의존하는 생성기 
##### 예제	
- 둘째 문자열이 첫 문자열의 문자들로만 이루어 진 문자열 튜플 생성기 `Gen[(String, String)]`
- 0에서 11사이의 정수를 선택하여 그 값을 목록의 길이로 사용한 리스트 생성기 `Gen[List[Double]]`

#### `flatMap`
- 먼저 하나의 값을 생성 후, 이를 다음 생성할 값의 결정에 사용 => **`flatMap`**

### 8.2.5 Prop 자료 형식의 정련
```scala
trait Prop {
  def check: Either[(FailedCaase, SuccessCount), SuccessCount]
}
```
- 속성이 테스트를 **통과** 하는 임계점을 지정하지 않음 
	- 하드코딩 보다 의존성을 적절히 추상화 하는것이 중요 

```scala
type TestCases = Int
type Result = Either[(FailedCase, SuccessCount), SuccessCount]
case class Prop(run: TestCases => Result)
```
- 성공한 테스트 수를 `Either`의 양 변에 모두 기록
	- 한 속성이 검사를 통과했다면, run의 개수와 동일한 것으로 간주 가능 
	- 이제는 `Either`의 `Right`가 필요 없으므로 `Option으로 변경가능 

```scala
type Result = Option[(FailedCase, SuccessCount)]
case class Prop(run: TestCases => Result)
```
- 일반적인 `Option`의 쓰임과 반대
	- `None`: 테스트의 통과를 의미
	- `Some`: 실패가 있었음을 의미 

```scala 
sealed trait Result {
  def isFalsified: Boolean
}

case object Passes extends Result {
  def isFalsified = false
}

case class Falsified(failure: FailedCase, successes: SuccessCount) extends Result {
  def isFalsified = true
}
```
- `forAll` 구현 불가능 
	- 시도 할 테스트 개수 외에도 테스트 생성 시 필요한 모든 정보 필요 
	- 무작위 테스트 생성시 `RNG`필요 

```scala 
case class Prop(run: (TestCases, RNG) => Result)
```
- 기타 다른 테스트의 의존성이 필요시 그에 해당하는 매개변수를 `Prop.run`에 추가 

#### `forAll`
```scala 
def forAll[A](as: Gen[A])(f: A => Boolean): Prop = Prop {
  (n, rng) => randomStream(as)(rng).zip(Stream.from(0)).take(n).map {
    case (a, i) => try {
      if (f(a)) Passed else Falsified(a.toString, i)
    } catch { case e: Exception => Falsified(buildMsg(a, e), i) }
  }.find(_.isFalsified).getOrElse(Passed)

def randomStream[A](g: Gen[A])(rng: RNG): Stream[A] = 
  Stream.unfold(rng)(rng => Some(g.sample.run(ring))

def buildMsg[A](s: A, e: Exception): String = 
  s"""
    |test case: $s
    |generated an exception: ${e.getMessage}
    |stack trace:\n ${e.getStackTrace.mkString("\n")}
    |""".stripMargin
```

## 8.3 검례 최소화
- 테스트 실패시, 그 테스트를 실패하게 만드는 가장 작은 또는 가장 간단한 테스트를 찾아내는 기법

### 검례 최소화를 위한 두 가지 접근방식
#### 수축 (shrinking)
- 테스트의 크기를 점점 줄여가면서 테스트 반복 
- 테스트가 더 이상 실패하지 않을 시 멈춤 
- 각 자료형마다 개별적인 코드 작성 필요 
- ScalaCheck등에서 채택

#### 크기별 생성 (sized generation)
- 애초에 크기와 복잡도를 점차 늘려가면서 테스트를 생성 
- 테스트 실행기가 적용 가능한 크기들의 공간을 크게 건너뛰면서도 가장 작은 테스트를 찾을 수 있도록 확장하는 방법도 다양함

```scala
case class SGen[+A](forSize: Int => Gen[A]) {
  def unsized: SGen[A] = ???
  def listOf[A](g: Gen[A]): SGen[List[A]] = ???
```

```scala
  def forAll[A](g: SGen[A])(f: A => Boolean): Prop = ???
```
-  `SGen`은 크기를 알아야 하지만 `Prop`에는 해당 정보가 주어지지 않음 => 구현 불가 
- 크기를 `Prop`의 한 의존성으로 추가 
	- `Prop`은 다양한 크기의 바탕 생성기들을 생성할 수 있어야 함 => `Prop`에 **최대 크기** 를 지정 필요

```scala
type MaxSize = Int
case class Prop(run: (MaxSize, TestCases, RNG) => Result)

def forAll[A](g: SGen[A])(f: A => Boolean): Prop = forAll(g(_))(f)

def forAll[A](g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
  (max, n, rng) =>
    val casesPerSize = (n + (max - 1)) / max
    val props: Stream[Prop] = Stream.from(0).take((n min max) + 1).map(i => forAll(g(i))(f))
    val prop: Prop = 
      props.map(p => Prop { (max, _, rng) =>p.run(max, casesPerSize, rng)}).toList.reduce(_ && _)
    prop.run(max, n, rng)
}
```

## 8.4 라이브러리의 사용과 사용성 개선 
### 사용성
- 편리한 구문과 사용 패턴에 따른 적합한 보조 함수들의 구성 

### 8.4.1 간단한 예제 몇 가지
#### `List.max`
- 목록의 최대값은 목록의 다른 요소보다 크거나 같아야 함 
```scala
val smallInt = Gen.choose(-10, 10)
val maxProp = forAll(listOf(smallInt)) { ns =>
  val max = ns.max
  !ns.exists(_ > max)
}
```
- `max`를 `Prop` 동반 객체의 메서드로 만들기 
```scala
def run(p: Prop, 
        maxSize: Int = 100, 
        testCases: Int = 100, 
        rng: RNG = RNG.Simple(System.currentTimeMillis)): Unit = p.run(maxSize, testCases, rng) match {
  case Falsified(msg, n) => println(s"! Falsified after $n passed tests: \n $msg")
  case Passed => println(s"+ OK, passed $testCases tests.")
}
```
- 스칼라의 기본 인수 활용 
- 검례 개수 기본값: 다양한 경우들을 충분히 검토 & 너무 많아 실행이 오래 걸리지는 않을 정도(?) 

### 8.4.2 병렬 계산을 위한 검사 모음 작성 
```scala
map(unit(1))(_ + 1) == unit(2)
```

```scala
val ES: ExecutorService = Executors.newCachedThreadPool
val p1 = Prop.forAll(Gen.unit(Par.unit(1)))(i =>
  Par.map(i)(_ + 1)(ES).get == Par.unit(2)(ES).get)
```
- 장황하고 지저분
- 테스트 기저의 **착안**(idea)이 그리 중요하지 않은 세부사항에 가림 
	- API 의 표현력 부재 문제는 아님 

#### 속성의 증명
##### 개선점 
- `forAll`이 다소 과하게 일반적 
	- 입력이 가변적이지 않음 (구체적 수치가 하드코딩) 
	- 전통적인 단위테스트 라이브러리 사용시만큼 간단하게 작성이 가능해야 함 
```scala
def check(p: => Boolean): Prop
```

- `forAll` 사용
```scala
def check(p: => Boolean): Prop = {  // 비엄격 함수 
  lazy val result = p  // 재계산을 피하기 위한 memoization
  forAll(unit(()))(_ => result)
}
```
- 단위 생성기를 이용하여 값을 하나만 생성 -> 주어진 `Boolean`을 평가하기 위한 것일 뿐, 생성된 값 자체는 무시
- 결과를 메모한다고 해도 검사 실행기는 여전히 여러개의 검례를 생성하여 Boolean을 여러 번 점검
	- `run(check(true))` 호출 => 검사 실행기는 100번 검사하여 `Ok, passed 100 tests` 출력
	- 항상 `true`인 속성을 100번 검사는 불필요 
- `Prop`의 표현 => `(MaxSize, TestCases, RNG) => Result` 형식의 함수 
	- `Result` => `Passed` 혹은 `Falsified` 

- 검례 개수를 무시하는 `Prop`을 구축하는 `check`라는 기본수단 구현 
```scala
def check(p: => Boolean): Prop = Prop { (_, _, _) =>
  if (p) Passed else Falsified("()", 0)
}
```
- `forAll`사용보다 개선됨 
- `run(check(true))`가 속성을 한 번만 검사하지만, 해당 출력은 여전히 `Ok, passed 100 tests`
	- 이 속성은 다수의 검사에서 반례가 발견되지 않아 검사를 **통과**(passed)한 것이 아님
	- 한 번의 검사에 의해 **증명** 된 것 


- **`Proved`**: 새로운 종류의 `Result`
```scala
case object Proved extends Result
```

- `Proved` 객체를 돌려주도록 수정된 `run`
```scala
def run(p: Prop, 
        maxSize: Int = 100, 
        testCases: Int = 100, 
        rng: RNG = RNG.Simple(System.currentTimeMillis)): Unit = p.run(maxSize, testCases, rng) match {
  case Falsified(msg, n) => println(s"! Falsified after $n passed tests: \n $msg")
  case Passed => println(s"+ OK, passed $testCases tests.")
  case Proved => println(s"+ OK, proved property.")
}
```

#### `Par`의 검사 
> `Par.map(Par.unit(1))(_ + 1) == Par.unit(2)` 에 대한 증명 

- 새 `Prop.check` 기본 수단 사용
```scala
val p2 = Prop.check {
  val p = Par.map(Par.unit(1))(_ + 1)
  val p2 = Par.unit(2)
  p(ES).get == ps2(ES).get
}
```
- 기존에 비해 많이 명확함 
- `p(ES).get`, `p2(ES).get` 개선 필요 
	- 두 `Par`값이 같은지 비교하려는 것 뿐인데 내부 구현 사항이 드러남 
	- `map2`를 이용하여 상등 비교를 `Par`로 **승급**(lifting) 가능 
```scala
def equal[A](p: Par[A], p2: Par[A]): Par[Boolean] = 
  Par.map2(p, p2)(_ == _)

val p3 = check {
  equal(
    Par.map(Par.unit(1))(_ + 1), 
    Par.unit(2)
  )(ES).get
}
```
- 양 변을 개별적으로 실행하는 것에 비해 개선 
- `Par`실행을 개별 함수 `forAllPar`로 이동 
	-  명시하고자 하는 속성을 지저분하게 만들지 않고, 다양한 병렬 전략들을 끼워 넣기에 적합한 장소 제공

```scala
val S = weighted(
  choose(1, 4).map(Executors.newFixedThreadPool) -> .75,
  unit(Executors.newCachedThreadPool ) -> .25)

def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop = 
  forAll(S.map2(g)((_, _))) { case (s, a) => f(a)(s).get }
```

- `S.map2(g)((_, _))` 를 간결하게 개선 
```scala
def **[B](g: Gen[B]): Gen[(A,B)] = (this map2 g)((_, _))

def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop = 
  forAll(s ** g) { case (s, a) => f(a)(s).get }
```

- `**`를 [커스텀 추출기](http://mng.bz/4pUc)를 이용하는 패턴으로 사용 가능 
```scala
def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop = 
  forAll(s ** g) { case s ** a => f(a)(s).get }
```
- 여러 생성기를 튜플로 엮을 때 잘 작동함 
- 튜플 패턴을 직접 사용시와 달리 패턴 매칭에서 괄호들을 중첩시킬 필요가 없음 

##### `unapply`함수가 있는 `**`라는 이름의 객체 생성 
```scala
object ** {
  def unapply[A, B](p: A, B)) = Some(p)
}
```

- **`S` is a `Gen[ExecutorService]`** that
	- 스레드가 4개 이하인 고정 크기 스레드 풀들을 가변적으로 사용
	- 크기가 한정되지 않은 스레드 풀도 고려

```scala
val p2 = checkPar {
  equal (
    Par.map(Par.unit(1))(_ + 1),
    Par.unit(2)
  )
}
```

##### `unit`함수의 일반화 검례
```scala
map(unit(x))(f) == unit(f(x))
```
```scala
map(y)(x => x) == y
```
- 임의의 형식을 모든 `y`에 대해 상등이 성립함을 암묵적으로 명시
- 하지만 속성의 표현에서는 특정한 `y` 값들을 지정해야 함 

```scala
val pint = Gen.choose(0, 10) map (Par.unit(_))
val p4 = forAllPar(pint)(n => equal(Par.map(n)(y => y), n))
```
- 병렬 계산의 값들은 `map`의 작동 방식에 영향을 미치지 않으므로 다른 타입에 대한 검사 구축 불필요 
- `map`에 **실제로** 영향을 미치는 것 => 병렬 계산의 **구조**
- 속성이 성립함을 더 확신하고 싶다면 그 구조에 풍부한(richer) 생성기 제공 필요 

## 8.5 고차 함수의 검사와 향후 개선방향 
- 표현력이 괜찮은 속성 기반 검사 라이브러리 구현 
- 하지만, 고차 함수를 검사하기에 적합한 구현이 아님 
- 생성기들을 사용해 **자료**를 생성하는 수단은 많으나 **함수** 를 생성하는 적당한 수단 부재 

#### `takeWhile`
- 주어진 술어를 만족하는 가장 긴 목록을 반환 
	- `List(1, 2, 3).takeWhile(_ < 3)` => `List(1, 2)`
- 임의의 목록 `s: List[A]`와 임의의 함수 `f: A => Boolean`에 대해 
	- `s.takeWhile(f).forall(f) == true`
	- 함수가 돌려준 목록의 모든 요소는 해당 술어를 만족 

#### 고차함수 검사
- **특정** 인수들만 조사하는 접근방식 
```scala
val isEven = (i: Int) => i%2 == 0
val takeWhileProp = 
  Prop.forAll(Gen.listOf(int))(ns => ns.takeWhile(isEven).forall(isEven))
```
- 테스트 프레임워크가 자동으로 생성해 준다면?
	- `Gen[Int]`에 대해 `Gen[String => Int]`를 생성 
```scala
def genStringIntFn(g: Gen[Int]): Gen[String => Int] = g map (i => (s => i))
```
- 자신의 입력을 무시하는 **상수함수**(constant function)을 생성
- `takeWhile`의 검사를 위해서 `Boolean`을 반환하는 함수가 필요
	- 이 생성기는 항상 `true`를 돌려주거나 항상 `false`를 반환 => 불필요 

## 8.6 생성기의 법칙들 
##### `Par`에서 구현한 `map`
```scala
def map[A, B](a: Par[A])(f: A => B): Par[B]
```

##### `Gen`에서 정의한 `map`
```scala
def map[B](f: A => B): Gen[B]
```
- Option, List, Stream, State등에도 비슷한 모습의 함수를 정의
- 서명만 비슷한 것인가? 혹은 실제로 어떤 동일한 **법칙**을 만족하는가? 
- 서로 각자의 영역에 비슷한 의미 => **영역을 가로지르는 어떤 근본적인 패턴이 존재**

## 8.7 요약
- 추상적인 대수와 구체적인 표현 사이에 각자에서 얻은 지식을 상대에 전파 
- 라이브러리를 특정 표현에 과도하게 적응시키거나, 최종 목표와는 동떨어진 허황된 추상에 도달하는 우를 회피 
- `map` 혹은 `flatMap`등과 같은 기존에 구현했던 것들과 비슷하게, 각 법칙이 **만족해야 하는 법칙**도 비슷
- 서로 다른 **문제점**들이 많지만, **함수적 해법**들의 공간은 한정됨 
- 일반적인 추상을 찾아내기 (3부) 
