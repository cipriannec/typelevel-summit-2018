package org.longcao

import cats.Eq
import cats.kernel.CommutativeSemigroup
import cats.kernel.laws.discipline._
import cats.tests.CatsSuite

// generates arbitrary `MyType`s
import org.scalacheck.ScalacheckShapeless._

class SimpleCustomMonoidsTest extends CatsSuite {
  // Can't construct a lawful CommutativeSemigroup[MyType] instance!
  // This test fails.
  {
    // boilerplate for equality checking in laws
    implicit def eqMyType: Eq[MyType] = Eq.fromUniversalEquals

    implicit val invalidCg = new CommutativeSemigroup[MyType] {
      override def combine(mt1: MyType, mt2: MyType): MyType = {
        MyType(
          name = mt1.name,
          x = mt1.x combine mt2.x,
          y = mt1.y combine mt2.y)
      }
    }

    checkAll("CommutativeSemigroup[MyType]", CommutativeSemigroupTests[MyType].commutativeSemigroup)
  }

  {
    // A ~little~ bit of cheating since my Monoid[List[MyType]] can
    // change order of records but should contain the same elements
    implicit def myTypeListEq: Eq[List[MyType]] =
      Eq.instance((x, y) => x.sortBy(_.name) == x.sortBy(_.name))

    checkAll("simple Monoid[List[MyType]]", MonoidTests[List[MyType]](SimpleCustomMonoids.myTypeListMonoid).monoid)
  }
}

