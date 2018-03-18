package org.longcao

import cats.kernel.laws.discipline._
import cats.tests.CatsSuite

class SimpleFoldsTest extends CatsSuite {
  // Test addingMonoid CommutativeMonoid
  checkAll("addingMonoid", CommutativeMonoidTests[Int](SimpleFolds.addingMonoid).commutativeMonoid)
}

