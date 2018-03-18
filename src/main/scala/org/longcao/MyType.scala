package org.longcao

case class MyType(
  name: String,
  x: Int,
  y: Option[Long])

object MyType {
  case class Values(
    x: Int,
    y: Option[Long])
}
