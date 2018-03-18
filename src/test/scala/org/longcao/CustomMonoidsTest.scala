package org.longcao

import cats.Eq
import cats.kernel.laws.discipline._
import cats.tests.CatsSuite

// generates arbitrary `MyType`s
import org.scalacheck.ScalacheckShapeless._

class CustomMonoidsTest extends CatsSuite {
  // A ~little~ bit of cheating since my Monoid[List[MyType]] can
  // change order of records but contain the same elements
  implicit def myTypeListEq: Eq[List[MyType]] =
    Eq.instance((x, y) => x.sortBy(_.name) == x.sortBy(_.name))

  // explicitly instantiate this since implicit resolution can be tricky *shrug*
  val kvMonoid = CustomMonoids.kvListMonoid(CustomMonoids.valuesSemigroup, CustomMonoids.keyValue)

  checkAll("Monoid[List[MyType]]", MonoidTests[List[MyType]](kvMonoid).monoid)
}
