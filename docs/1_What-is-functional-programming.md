# 1. 함수형 프로그래밍이란 무엇인가?

> - 부수효과를 제거한 함수형 프로그래밍 작성 및 그 이점
- **참조 투명성**(referential transparency)
- **치환 모형**(substitution model)


- **부수효과**가 없는 **순수함수** 들로만 이루어진 프로그램
- 부수효과: 그냥 결과를 돌려주는 일 외의 다른 임의의 일을 수행하는 함수
- 프로그램 작성**방식**에 대한 제약일 뿐 표현 가능한 프로그램의 **종류**에 대한 제약이 아님

## 1.1 FP의 이점: 간단한 예제 하나

### 1.1.1 부수 효과가 있는 프로그램

```scala
class Cafe {
  def buyCoffee(cc: CreditCard): Coffee = {
    val cup = new Coffee()
    cc.charge(cup.price)
    cup
  }
}
```

- `cc.charge(cup.price)`: 부수효과
  - 신용카드 청구시 외부 세계와의 일정한 상호작용 필요
  - `buyCoffee`함수는 하나의 `Coffee`객체를 돌려 줄 뿐 그 외의 동작은 모두 **부수적으로** 발생

- 부수효과로 인한 검사의 어려움 개선
  - 실제 신용카드 회사와의 카드 청구 테스트 불가
  - `Payment`객체를 `buyCoffee`에 제공하여 모듈성&검사성 향상

```scala
class Cafe {
  def buyCoffee(cc: CreditCard, p: Payments): Coffee = {
    val cup = new Coffee()
    p.charge(cc, cup.price)
    cup
  }
}
```
#### 부수효과
- `p.charce` 호출시 가능성이 여전히 상존:

#### 검사성
- Pros: 적어도 검사성은 조금 향상
  - Payments를 인터페이스화 하여, 모의(mock) 구현 작성하여 검사를 **보다** 수월하게 진행 가능
- Cons: 하지만...
  - 반드시 `Payments`라는 `interface`를 생성해야 함
  - 구체(concrete)클래스를 잘 구현한다고 해도 모의 구현은 사용상 어색함이 따름
    - `buyCoffee`이후 조사해야 할 어떤 내부 상태 변경시 `charge`호출에 의해 그 상태가 적절히 변경(변이[mutation]) 되었는지 확인 필요
    - 모의 프레임워크(mock framework)등의 툴이 있지만 간단한 테스트시 배보다 배꼽이 더 커질 수 있다

#### 재사용성
- 커피를 여러 잔 주문시 대금 청구를 해당 수에 비례하여 호출: **모아서 한번에 청구하는 프로세스 불가능**

### 1.1.2 함수적 해법: 부수 효과의 제거
```scala
class Cafe {
  def buyCoffee(cc: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(cc, cup.price))
  }
}
```
- `buyCoffee`가 `Coffee`뿐 아니라 **청구건을 하나의 값으로 반환**하도록 프로그램 수정
- 청구건의 **생성** 문제가 청구건의 **처리** 또는 **연동** 문제와 분리됨

- `Charge`의 구현: [Charge.scala](../src/main/scala/examples/chapter1/Charge.scala)
  - `CreditCard`와 `amount`를 담는다
  - `combine`: 동일한 `CreditCard`를 취합하는 메서드

- `buyCoffees`: `buyCoffee`를 직접 재사용하여 여러잔의 커피 구매에 대응
  - `buyCoffee`, `buyCoffees` 모두 `Payments`인터페이스에 대한 복잡한 모의구현 없이 테스트 가능
  - `Cafe`는 `Charge`가 어떻게 처리되는 지 알 필요 없음

- `Charge`를 일급 값으로 만들면 청구건들을 다루는 business-logic을 좀 더 쉽게 조립 가능
  - 복수의 주문(한 번의 주문에 여러개의 커피가 아닌)에 대응하는 예제
```scala
def coalesce(charges: List[Charge]): List[Charge] =
  charges.groupBy(_.cc).values.map(_.reduce(_ combine _)).toList
```

- 함수형 프로그래밍이란...
  - 많은 사람들이 좋다고 여기는 생각을 논리적인 극한까지 밀어붙이고, 적용하는 규율의 하나
  - 간단한 루프에서부터 고수준 프로그램 기반구조에 이르기까지 모든 수준에서 프로그램의 조직화 방식을 급진적으로 변화시킴

## 1.2 (순수) 함수란 구체적으로 무엇인가?
#### 수학적인 정의
- 입력 형식이 **A**이고, 출력 형식이 **B**인 함수 **f**는, 형식이 **A**인 모든 값 **a**를 각각 형식이 **B**인 모든 값 **b**에 연관시키되, **b**가 오직 **a**에 의해서만 결정된다는 조건을 만족하는 계산
#### 스칼라에서는...
- `A => B`라고 표기
- 주어진 입력(A)으로 뭔가를 계산하여 어떤 결과(B)를 반환하는 외에 다른 일을 하지 않는 것


#### 참조 투명성
- 함수가 아닌 **표현식**의 한 속성
  - 표현식: 프로그램 구성 코드 중 하나의 결과로 평가될 수 있는 임의의 코드 조각
  - ex> `2 + 3` 은 `2`와 `3`이라는 표현식에 `+`라는 함수를 적용시킨 또 하나의 표현식, 이는 이 표현식에 대한 평가 결과인 `5`라는 표현식으로 대체해도 의미가 같다
- 임의의 프로그램에서 만일 어떤 표현식을 그 평과 결과로 바꾸어도 의미가 변하지 않는다면, 이 표현식은 **참조에 투명한 것**
- 어떤 함수를 참조에 투명한 인수들로 호출하면 그 함수도 참조에 투명

> **참조 투명성과 순수성**
모든 프로그램(`p`)에 대해 표현식(`e`)의 모든 출현(occurrence)을 `e`의 평가 결과로 치환해도 `p`의 의미에 아무 영향이 미치지 않는다면,
이 표현식 `e`는 **참조에 투명하다**(referentially transparent)
만일 표현식 `f(x)`가 참조에 투명한 모든 `x`에 대해 참조에 투명하면, 함수 `f`는 **순수하다**(pure)


