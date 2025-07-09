package core

object Shared {
  def sortPair(itemA: Int, itemB: Int): (Int, Int) = {
    if (itemA <= itemB) (itemA, itemB) else (itemB, itemA)
  }
}
