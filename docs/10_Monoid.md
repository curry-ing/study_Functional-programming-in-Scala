# 10 모노이드
> **오직 대수에 의해서만 정의되는** 간단한 구조 
### 모노이드의 유용함
- 병렬 계산 용이: 문제를 병렬로 계산할 수 있는 여러 조각으로 분리 가능 
- 조합이 용이: 간단한 계산을 조립해서 더 복잡한 계산으로 발전 가능 

## 10.1 모노이드란 무엇인가? 
#### 모노이드 법칙
- 항등원(identity element)
- 결합법칙(associative law)

#### 모노이드 구성 요소
- 어떤 형식 `A`
- `op`: `A`형식의 값 두 개를 받아 하나의 값을 산출하는 결합적 이항 연산
	- 임의의 `x: A`, `y: A`, `z: A`에 대해 `op(op(x, y), z) == op(x, op(y, z))` 가 성립한다
- `zero`: 그 연산의 항등원인 값
	- `op(x, zero) == x`, `op(zero, x) == x` 

#### Scala `trait`으로 표현
```scala
trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}
```

##### String 모노이드 
```scala
val stringMonoid = new Monoid[String] = {
  dev op(a1: String, a2: String) = a1 + a2
  val zero = ""
}
```

##### List 모노이드 
```scala
def listMonoid[A] = new Monoid[List[A]] { 
  def op(a1: List[A], a2: List[A]) = a1 ++ a2
  def zero = Nil
}
```
  
#### 모노이드 타입 과 모노이드 인스턴스를 가진 타입 
- `Monoid[A]` 타입의 인스턴스 외에도, 타입과 해당 법칙들을 만족하는 인스턴스 모두 **모노이드 이다**
- 타입 `A`는 `Monoid[A]` 인스턴스에 정의된 연산들에 의해 하나의 **모노이드를 형성**(form)
- 타입 `A`는 **곧** 모노이드 이다 / 타입 `A`는 **모노이드적**(monoidal)이다 
  
> **모노이드란 도대체 무엇인가?**  
> 하나의 형식이되 그 형식에 대해 **결합법칙**을 만족한 **항등원**(`zero`)을 가진 **이항연산**(`op`)  

## 10.2 모노이드를 이용한 목록 접기 
##### `List`에 대한 목록 접기(`foldLeft`, `foldRight`) 서명
```scala
def foldRight[B](z: B)(f: (A, B) => B): B
def foldLeft[B](z: B)(f: (A, B) => B): B
```

##### 위 예제에서 `A`, `B`가 같은 형식인 경우?
```scala
def foldRight(z: A)(f: (A, A) => A): A
def foldLeft(z: A)(f: (A, A) => A): A
```
- Monoid의 구성요소들이 이 인수 형식과 일치 
- `List[String]`에 `stringMonoid`의 `op`와 `zero`만 넘겨주면, 모노이드로 List를 접어서 문자 연결 가능 

```scala
val words = List("Hic", "Est", "Index")
words.foldRight(stringMonoid.zero)(stringMonoid.op)
words.foldLeft(stringMonoid.zero)(stringMonoid.op)
```
- `foldLeft` / `foldRight` 사용여부는 중요치 않음 => 결합법칙 & 항등법칙 
  
```scala
words.foldLeft("")(_ + _) == (("" + "Hic") + "Est") + "Index"
words.foldRight("")(_ + _) == "Hic" + ("Est" + ("Index" + ""))
```

##### `concatenate`
- 위 내용을 일반화한, _목록을 접는 **일반적인** 함수_
```scala
def concatenate[A](as: List[A], m: Monoid[A]): A = 
  as.foldLeft(m.zero)(m.op)
```

##### `foldMap`
- 목록의 원소 형식이 `Monoid`인스턴스와 부합하지 않는 경우: `map`적용 
```scala
def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B
```

## 10.3 결합법칙과 병렬성 
#### 일반 접기(sequential fold)
- 방향 선택 가능 
- foldRight: `op(a, op(b, op(c, d)))`
- foldLeft: `op(op(op(a, b), c), d)`

#### 균형 접기(balanced fold)
-  **병렬 처리 가능**
- _트리의 균형이 좋을수록_ 전체 계산의 효율성 증가
- balancedFold: `op(op(a, b), op(c, d))`


## 10.4 예제: 병렬 파싱 
### 임의 `String` 단어 수 세기 
#### 간단한 방법
- 문자열을 한 문자씩 훑으면서 공백(whitesapce)을 확인
- 공백이 아닌 문자들의 연속열(run) 개수 세기 
- 마지막 조사 문자가 공백인지 여부를 뜻하는 간단한 상태정도만 필요 

#### 메모리에 다 담기지 않는 거대한 텍스트라면 
- **chunking**: 파일을 감당 가능한 여러 조각으로 분할하여 
- 병렬로 단어 수를 세어서 결과들을 merge
- **결합법칙 성립이 필수 조건**: 지금 조사하는 조각이 파일의 처음이든, 끝이든 중간 어디든 상관없어야 함 

#### 예제
`lorem ipsum dolor sit amen, `

##### 무작정 반으로 나누면...
- dolor 사이에서 나뉘어짐(`~do` + `lor~`) => 중복 집계됨 
- `do`, `lor` 같은 부분 결과들을 처리가능해야 함 
- 지금까지 찾은 잘리지 않은 **완전한 단어** 등을 기억하는 자료구조도 필요 

```scala
sealed trait WC
case class Stub(chars: String) extends WC
case class Part(lStub: String, words: Int, rStub: String) extends WC
```
- `Stub`: 아직 완전한 단어를 하나도 보지 못한 상태
- `Part`: 지금까지 조사한 완전한 단어들의 개수를 `words`에 유지
	- `lStub`, `rStub`: 그 단어들의 왼쪽/ 오른쪽에서 발견한 부분 단어들의 개수를 담음 

- `"lorem ipsum do"` => `Part("lorem", 1, "do")`
	- `lorem`의 왼쪽이나 `do`의 오른쪽엔 공백이 없으므로 확실한 단어라고 볼 수 없음 
	-  확실한 단어는 `ipsum`
- `"lor sit amet, "` => `Part("lor", 2, "")`

#### 모노이드의 준동형사상 (homomorphic)
- 모노이드 **사이의** 함수들에 성립하는 법칙 발견 
- `String` 연결 모노이드 & `Int` 덧셈 모노이드
	- 두 문자열의 길이의 합 == 두 문자열을 연결한 문자열의 길이
	- `"foo".length + "bar".length == ("foo" + "bar").length`
		- `length`: `String => Int`타입의 함수, 
		- **모노이드의 구조를 보존** => 준동형사상(homomorphism)

##### 일반법칙
> 모노이드 `M`과 모노이드 `N`사이의 모노이드 준동형사상 `f`는 모든 `x`, `y`값에 대해 다음 일반법칙을 따름
```scala
M.op(f(x), f(y)) == f(N.op(x, y))
```

##### 실제 유용한 경우 
- 라이브러리 설계시, 사용하는 두 타입이 모노이드이고, 그 둘 사이에 함수들이 존재하는 경우
- 함수들이 **모노이드 구조를 유지해야 마땅한지 고려** 후 _준동형사상_을 자동화 테스트를 해 보는 것이 좋을 것

##### 양방향 준동형사상
- 두 모노이드가 하나의 모노이드 동형사상(isomorphism, _iso~_는 **상등**(equal)을 의미)을 만족시, 이 두 모노이드를 **동형**(isomorphic)이라 칭함 
- 모노이드 `M`과 `N` 사이의 동형사상에는 두 준동형사상 `f`와 `g`가 있는데, `f andThen g`와 `g andThen f`는 모두 **항등함수** 이다 
	- `String`모노이드와 `List[Char]`모노이드는 **연결(결합) 연산**에 의해 **동형**임 
	- 두 Bool 모노이드 `(false, ||)`, `(true, &&)`도 **`!`(부정)** 함수에 의해 **동형**이다


## 10.5 접을 수 있는 자료구조
- `List`, `Tree`, `Stream`, `IndexedSeq` 등 접을 수 있는 자료구조 구현시, 이런 자료구조의 형태나 지연 여부, 효율적인 Random Access 능력등은 크게 신경쓰지 않음 
- _정수들이 가득한(?)_ 자료구조에서 정수들의 합을 계산시 `foldRight` 사용 가능 => `ints.foldRight(0)(_ + _)`

##### `Foldable`
```scala
trait Foldable[F[_]] {
  def foldRight[A, B](as: F[A])(z: B)(f: (A,B) => B): B
  def foldLeft[A, B](as: F[A])(z: B)(f: (A,B) => B): B
  def foldMap[A, B](as: F[A])(f: A => B)(mb: Monoid[B]): B
  def concatenate[A](as: F[A])(m: Monoid[A]): A = foldLeft(as)(m.zero)(m.op)
}
```
- 타입 생성자 `F`에 대한 추상화를 통해 구현된 Trait
- **`F[_]`**: `F`가 특정 타입이 아닌 어떤 타입 파라미터를 받는 **타입 생성자**(type consturctor)임을 나타냄
- **고차 타입 생성자**(higher-order type constructor) 혹은 **상위 종류 타입**(higher-kinded type)이라고 부름 


## 10.6 모노이드 합성
- Monoid추상 자체는 그리 대단할 것이 없음 (일반화된 `foldMap`정도가 약간...)
- Monoid 의 진정한 위력은 **합성 능력** 에서 비롯함 
	- 모노이드의 **곱**(product)
		- `A`, `B` 두 타입이 모두 모노이드 인 경우, 튜플 형식인 `(A, B)` 역시 모노이드임을 뜻함

### 10.6.1 좀 더 복잡한 모노이드 합성 
> 컬렉션에 담긴 타입들이  모노이드를 형성한다면, 그 컬렉션 자체도 흥미로운 모노이드를 형성할 때가 있음 

#### `Map`
##### `Map`들의 병합을 위한 Monoid
- Key-value pair의 `Map`이 있을때, 값 형식이 모노이드인 경우 작성 가능   
```scala
def mapMergeMonoid[K, V](V: Monoid[V]): Monoid[Map[K, V]] = 
  new Monoid[Map[K, V]] {
    def zero = Map[K, V]()
    def op(a: Map[K, V], b: Map[K. V]) = 
      (a.keySet ++ b.keySet).foldLeft(zero) { (acc, k) =>
        acc.updated(k, V.op(a.getOrElse(k, V.zero), b.getOrElse(k, V.zero)))
      }
  }
```

##### 좀 더 복잡한 모노이드를 수월하게 조립 가능 
```scala
val M: Monoid[Map[String, Map[String, Int]]] = mapMergeMonoid(mapMergeMonoid(intAddition))

val m1 = Map("o1" -> Map("i1" -> 1, "i2" -> 2))
// m1: Map[String, Map[String, Int]] = Map(o1 -> Map(i1 -> 1, i2 -> 2))
val m2 = Map("o1" -> Map("i2" -> 3))
// m2: Map[String, Map[String, Int]] = Map(o1 -> Map(i2 -> 3))
val m3 = M.op(m1, m2)
// m3: Map[String, Map[String, Int]] = Map(o1 -> Map(i1 -> 1, i2 -> 5))
```

### 10.6.2 모노이드 합성을 이용한 순회 융합 
- 여러 모노이드를 하나로 합성 가능하다 => **자료구조를 접을 때 여러 계산을 동시에 수행 가능하다**

##### 목록의 평균을 구하는 예제
```scala
val m = productMonoid(intAddition, intAddition)
// m: Monoid[(Int, Int)] = $anon$1@8ff557a
val p = ListFoldable.foldMap(List(1, 2, 3, 4))(a => (1, a))(m)
// p: (Int, Int) = (4, 10)
val mean = p._1 / p._2.toDouble
// mean: Double = 2.5
```
- 모노이드를  `productMonoid`와 `foldMap`을 이용해 일일이 조립하는 게 번거로움 
	- `foldMap`의 매핑 함수로부터 Monoid 구축시, 이들의 타입을 일일이 맞추어야 함 
- 합성된 모노이드들을 조립하는 작업과, 병렬화하여 하나의 패스로 실행할 수 있는 복잡한 계산을 정의하는 작업 등은 훨씬 수월하게 수행하는 조합기 라이브러리 구축은 가능 

## 10.7 요약
### 배운 것
#### 모노이드: 가장 간단한 순수 대수적 추상
- 추상적인 연산과, 그 연산들을 관장하는 법칙들로만 정의되는 순수 추상적 대수의 첫 사례
- 인수의 형식이 하나의 모노이드를 형성한다는 사실만으로도 유용한 함수들이 파생 가능 
##### 결합법칙
- `Foldable`을 지원하는 자료형식은 모두 접는 행위 가능 
- 병렬적으로 수행할 수 있는 유연성 확보
##### 모노이드 합성
- 선언적이고 재사용 가능한 방식으로 접기 연산을 조립 가능 

### 배울 것
- 순수 대수적 인터페이스 소개
- 이런 인터페이스들을 활용해 여러 공통의 패턴을 어떻게 캡슐화 하는가?
