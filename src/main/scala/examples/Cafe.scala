package examples

class Cafe {
  def buyCoffee(cc: CreditCard, p: Payments): Coffee = {
    val cup = new Coffee()
    p.charge(cc, cup.price)    // remove side-effect and call Payment object
    cup
  }
}
