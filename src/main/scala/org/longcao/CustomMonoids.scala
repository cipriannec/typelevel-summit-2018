package org.longcao

import cats.implicits._
import cats.kernel.{ CommutativeSemigroup, Monoid }

import org.apache.spark._
import org.apache.spark.sql._

/**
 * Typeclass for converting value T to a key/value pair and back.
 */
trait KeyValue[T, K, V] extends Serializable {
  def to(t: T): (K, V)
  def from(k: K, v: V): T
}

object CustomMonoids {
  // Typeclass instance for converting MyType to a (K, V) representation and back.
  implicit val keyValue = new KeyValue[MyType, String, MyType.Values] {
    override def to(mt: MyType): (String, MyType.Values) =
      (mt.name, MyType.Values(mt.x, mt.y))
    override def from(k: String, v: MyType.Values): MyType =
      MyType(k, v.x, v.y)
  }

  // CommutativeSemigroup that combines only the _values_ of MyType
  implicit val valuesSemigroup = new CommutativeSemigroup[MyType.Values] {
    override def combine(v1: MyType.Values, v2: MyType.Values): MyType.Values = {
      MyType.Values(
        x = v1.x combine v2.x, // delegate to Semigroup instances for these fields
        y = v1.y combine v2.y)
    }
  }

  // Monoid typeclass instance that combines `List[T]`s.
  // Specifically, it combines the values of T in each list by key if T can be converted to/from (K, V).
  implicit def kvListMonoid[T, K, V: CommutativeSemigroup](implicit kver: KeyValue[T, K, V]) = new Monoid[List[T]] {
    override def empty: List[T] = List.empty[T]

    override def combine(x: List[T], y: List[T]): List[T] = {
      (x ++ y)
        .map(kver.to)
        .groupBy(_._1)
        .map { case (k, kvs) =>
          val combined: V = kvs.map(_._2)
            .reduce(_ combine _)

          kver.from(k, combined)
        }
        .toList
    }
  }

  // Monoid typeclass instance that combines `Dataset[T]`s.
  // Specifically, it combines the values of T in each dataset by key if T can be converted to/from (K, V).
  // The combination of the values of T can be done in parallel and is defined by a CommutativeSemigroup.
  def kvDatasetMonoid[T: Encoder, K: Encoder, V: Encoder: CommutativeSemigroup](implicit kver: KeyValue[T, K, V], spark: SparkSession) = new Monoid[Dataset[T]] {
    import cats.implicits._

    private val tupleEncoder: Encoder[(K, V)] = Encoders.tuple[K, V](
      implicitly[Encoder[K]],
      implicitly[Encoder[V]])

    override def empty: Dataset[T] = spark.emptyDataset[T]

    override def combine(ds1: Dataset[T], ds2: Dataset[T]): Dataset[T] = {
      ds1.union(ds2)
        .map(kver.to(_))(tupleEncoder)
        .groupByKey((kv: (K, V)) => kv._1)
        .reduceGroups { (kv1: (K, V), kv2: (K, V)) =>
          val (k1, v1) = kv1
          val (k2, v2) = kv2

          val combined = v1 combine v2

          (k1, combined)
        }
        .map { kkv: (K, (K, V)) =>
          val (k, (kv)) = kkv
          kver.from(k, kv._2)
        }
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val spark = SparkSession.builder().master("local[*]").getOrCreate()
    import spark.implicits._
    spark.sparkContext.setLogLevel("ERROR")

    // First collection of MyTypes
    val myTypes1 = List(
      MyType("a", 1, Some(500L)),
      MyType("b", 2, Some(1000L)),
      MyType("c", 3, Some(5000L)))

    // Second collection of MyTypes
    val myTypes2 = List(
      MyType("a", 10, Some(5000L)),
      MyType("b", 20, Some(10000L)))

    {
      println(Monoid[List[MyType]].combine(myTypes1, myTypes2))
    }

    {
      implicit val cm: Monoid[Dataset[MyType]] = kvDatasetMonoid[MyType, String, MyType.Values]

      val myTypesDs1 = spark.createDataset(myTypes1)
      val myTypesDs2 = spark.createDataset(myTypes2)

      val combined: Dataset[MyType] = Monoid[Dataset[MyType]].combine(myTypesDs1, myTypesDs2)
      combined.show
    }

    spark.stop()
  }
}
