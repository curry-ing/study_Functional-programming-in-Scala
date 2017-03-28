package examples.chapter1

class Cafe {
  def buyCoffee(cc: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(cc, cup.price))
  }

  def buyCoffees(cc: CreditCard, n: Int): (List[Coffee], Charge) = {
    val purchases: List[(Coffee, Charge)] = List.fill(n)(buyCoffee(cc))   // List.fill(n)(x) : x의 복사본 n개로 이루어진 List 생성
    val (coffees, charges) = purchases.unzip    // unzip: Tuple의 목록을 목록들의 Tuple로 분리.
    (coffees, charges.reduce((c1, c2) => c1.combine(c2)))   // 청구건 2개를 combine 함수를 이용하여 하나로 결합하는 과정을 반복. (청구건 목록 전체를 하나의 청구건으로 환원)
  }
}
