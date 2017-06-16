# 5. 엄격성과 나태성
> 홀수 카드를 모두 제거하고 퀸 카드를 모두 뒤집는 놀이의 예
> 1. 카드를 훑으며 홀수가 나오면 빼고, 퀸이 나오면 뒤집는 작업을 동시에 수행 O(n)
> 2. 홀수 카드를 먼저 제거 후, 다시 훑으며 퀸 카드를 뒤집는 작업 수행 O(2n)

스칼라는 보통 2번의 방식으로 일을 수행
```scala
List(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).map(_ * 3)

// 위 코드가 어떤 방식으로 일을 수행하는지 생각 해 볼 것

List(11, 12, 13, 14).filter(_ % 2 == 0).map(_ * 3)
List(12, 14).map(_ * 3)
List(36, 42)
```

## 5.1 엄격한 함수와 엄격하지 않은 함수

- **비엄격성(Laziness)**: 가능한 경우 모든 표현식을 다 평가하지 않고도 작업 수행
  - ex> `&&`, `||`와 같은 연산자는 short-circuit 이 대표적인 laziness
- **`if`**
  - 조건 표현식 2. 참일때 돌려주는 인수가 셋인 함수

```scala
def if2[A](cond: Boolean, onTrue: () => A, onFalse: () => A): A =
  if(cond) onTrue() else onFalse()
```

- `() =>` **thunk** : 표현식의 평가되지 않은 형태

```scala
def if2[A](cond: Boolean, onTrue: => A, onFalse: => A): A =
  if(cond) onTrue else onFalse
```

- 비엄격 함수의 인수: **값(by value)** 이 아닌 **이름으로(by name) 전달**

> **엄격성의 공식적 정의**  
> 바닥으로 평가되는 모든 x에 대하여 표현식 `f(x)`도 바닥으로 평가시 **`f`는 엄격한 함수**  
> **바닥 평가**: 어떤 표현식의 평가가 무한히 실행 혹은 한정된 값을 돌려주지 않고 오류를 던짐

## 5.2 확장 예제: 게으른 목록

[Stream.scala](../src/main/scala/fpinscala/laziness/Stream.scala)

- `List`와 비슷
- `Cons`자료 생성자가 명시적인 **성크**(`() => A`, `() => Stream[A]`)를 받음
  - `Stream`을 조사하거나 순회하려면 이 성크들의 평가를 강제 필요
  - ex> `def headOption`


### 5.2.1 스트림의 메모화를 통한 재계산 피하기
#### 캐싱
- `Cons` 노드가 일단 강제된 경우 그 값을 **캐싱** 해 두는 것이 유리

ex> 다음과 같이 Cons 생성자를 직접 사용시  `expensive(x)`가 두 번 계산됨
```scala
val x = Cons(() => expensive(x), tl)
val h1 = x.headOption
val h2 = x.headOption
```
##### 해결책
- 추가적인 불변식(invariant) 보장
- **똑똑한 생성자** (`cons`)
  - 패턴 매칭에 쓰이는 실제 생성자와는 조금 다른 서명을 제공하는 자료 형식을 생헝하는 함수
  - 이름으로 해당 자료 생성자의 첫 글자를 소문자로 변경
  - `cons`는 `Cons`의 머리와 꼬리를 이름으로 전달받아 **메모화(memoization) 수행**
  - 성크는 오직 한 번만 평가되고 이후의 강제에서는 캐싱된 lazy val 이 반환

### 5.2.2 스트림의 조사를 위한 보조 함수들

[Stream.scala](../src/main/scala/fpinscala/laziness/Stream.scala)

- `def toList: List[A]`
- `def take(n)`, `def drop(n)`
- `def takeWhile(p: A => Boolean): Stream[A]`
- `Stream(1, 2, 3).take(2).toList`


## 5.3 프로그램 서술과 평가의 분리
#### 관심사의 분리 (separation of concerns)
- 일급 함수: 일부 계산을 본문에 담고 있으나, 그 계산은 **인수들이 전달되어야** 수행 가능
- `Option`: 오류가 발생했다는 사실만 담고 있을 뿐, 오류에 대해 무엇을 수행할 지는 **분리된 관심사**
- `Stream`: 요소들의 순차열을 생성하는 계산을 구축하되, 이의 실행은 요소가 필요할 때 까지 **미를 수 있음**

- **나태성을 통해 표현식의 서술을 그 표현식의 평가와 분리 가능**
  - 필요한 것 보다 **더 큰** 표현식을 서술하되 그 표현식의 일부만 평가 가능

##### 예제
```scala
def exists(p: A => Boolean): Boolean = this match {
  case Cons(h, t) => p(h()) || t().exists(p)
  case _ => false
}
```  
- 꼬리를 생성하도록 되어 있는 코드의 수행 여부: **FALSE**
  - `||`는 둘째 인수에 대해 **lazy**: `p(h())`가 참인 경우 순회 종료
  - Stream의 꼬리 역시 **lazy**: 스트림의 꼬리는 평가되지 않음

##### `foldRight`
```scala
def foldRight[B](z: => B)(f: (A, => B) => B): B = this match
  case Cons(h, t) => f(h(), t().foldRight(z)(f))
  case _ => z
```  
- `f`가 둘째 매개변수에 대해 lazy -> `f`가 둘째 매개변수를 평가치 않기로 했다면 순회 종료

```scala
def exists(p: A => Boolean): Boolean =
  foldRight(false)((a, b) => p(a) || b)
```

- `foldRight`가 순회를 일찍 종료할 수 있기에 `exists` 구현에 재사용 가능
- Strict한 형태에서는 불가능 => 조기 종료를 처리하도록 특화된 재귀적 `exists` 작성 필요
- **나태성은 코드 재사용성을 좀 더 증가시킴**

- **점진적(incremental) 구현**
  - 전체 결과를 생성하지 않음
  - 결과 Stream 요소들을 다른 어떤 계산이 참조되는 시점이 되어서야 그 Stream을 생성하는 계산이 실제 진행
  - 구현은 요청된 요소들을 생성하는 데 필요한 작업만 수행
  - 중간 결과를 완전히 인스턴스화 하지 않고도 이 함수들을 연이어 호출 가능

```scala
Stream(1, 2, 3, 4).map(_ + 10).filter(_ % 2 == 0).toList
cons(11, Stream(2, 3, 4).map(_ + 10)).filter(_ % 2 == 0).toList
Stream(2, 3, 4).map(_ + 10).filter(_ % 2 == 0).toList
cons(12, Stream(3, 4).map(_ + 10)).filter(_ % 2 == 0).toList
12 :: Stream(3, 4).map(_ + 10).filter(_ % 2 == 0).toList
12 :: cons(13, Stream(4).map(_ + 10)).filter(_ % 2 == 0).toList
12 :: Stream(4).map(_ + 10).filter(_ % 2 == 0).toList
12 :: cons(14, Stream().map(_ + 10)).filter(_ % 2 == 0).toList
12 :: 14 :: Stream().map(_ + 10).filter(_ % 2 == 0).toList
12 :: 14 :: List()
```

- `map`의 출력의 한 요소를 생성하는 계산과 그 요소가 2로 나누어지는지 `filter`로 판정하는 계산이 번갈아 수행
- `map`에서 비롯된 중간 스트림은 완전히 인스턴스화 되지 않음
- 특수 목적의 루프를 이용해 변환 논리를 엇갈려 수행
- **일급 루프(first-class loop)**: `map`과 `filter`등과 같은 고차 함수를 이용해 논리를 결합
- 메모리 활용에 이점: `filter`등으로 걸러진 불필요한 공간을 gc등이 재확보 가능

```scala
def find(p: A => Boolean): Option[A] =
  filter(p).headOption
```
- `filter`는 전체 스트림 변환을 하지만 **lazy하게 일어나므로** find가 부합하는 요소를 발견 즉시 종료


## 5.4 무한 스트림과 공재귀
#### 무한 스트림(infinite stream)

```scala
val ones: Stream[Int] = Stream.cons(1, ones)
```
- 1이 무한히 나열되는 Stream
- 무한하지만 위에서 작성한 함수들은 요두뇌 출력을 산출하는 데 필요한 만큼만 스트림 조사

- Stream을 사용한 연산 시 무한 루프에 빠지거나 스택에 안전하지 않은 표현식이 만들어지기 쉬움
  - ex> `ones.forAll(_ == 1)`
- **입력이 무한수열을 생성한다고 해도 유한한 자원을 이용해서 평가 가능한 함수들이 많이 있다**

#### 공재귀(corecursive)
- 재귀 함수는 자료를 소비하지만, 공재귀 함수는 자료를 생산
- **생산성** 을 유지하는 한 종료될 필요가 없음
- 유한한 시간 내에 더 많은 결과를 평가하는 것이 항상 가능
- 공재귀 = **보호되는 재귀**(guarded recursion) / 생산성 = **공종료**(cotermination)

> `unfold`를 이용해 `constant`, `ones` 등을 정의: 재귀적 정의와는 달리 공유가 발생하지 않음
> 재귀적 정의는 순회중 메모리에 대한 참조를 유지한다고 해도 일정한 양의 메모리 소진. 공재귀는 그렇지 않음
> `Stream`으로 프로그래밍시 공유 유지에 의존하는 경우는 드물다 -> 공유 관계가 극도로 예민하고, 형식으로는 추적되지 않음

## 5.5 요약
#### 배운 것
##### 관심사의 분리
- **모듈성 증가**: 표현식의 서술과 표현식의 평가 방법 및 시기를 분리
- 표현식의 서술을 여러 문맥에서 재사용 가능
  - 필요에 따라 표현식의 서로 다른 부분을 평가하여 서로 다른 결과를 얻는 것이 가능
  - 엄격한 코드에서 서술과 평가가 엮여 있으면 이런 재사용이 불가능  

#### 배울 것
- **상태**(state)에 대한 순수 함수적 접근방식
