package org.longcao

import cats.implicits._
import cats.kernel.Monoid

import org.apache.spark._
import org.apache.spark.sql._

object SimpleCustomMonoids {
  implicit val myTypeListMonoid = new Monoid[List[MyType]] {
    override def empty: List[MyType] = List.empty[MyType]

    override def combine(x: List[MyType], y: List[MyType]): List[MyType] = {
      (x ++ y)
        .groupBy(_.name)
        .map { case (k, myTypes) =>
          myTypes.reduce { (mt1, mt2) =>
            MyType(
              name = mt1.name, // Just pick one since this is grouped by key anyways
              x = mt1.x combine mt2.x,
              y = mt1.y combine mt2.y)
          }
        }
        .toList
    }
  }

  implicit def myTypeDatasetMonoid(implicit spark: SparkSession) = new Monoid[Dataset[MyType]] {
    import spark.implicits._

    override def empty: Dataset[MyType] = spark.emptyDataset[MyType]

    override def combine(ds1: Dataset[MyType], ds2: Dataset[MyType]): Dataset[MyType] = {
      ds1.union(ds2)
        .groupByKey(_.name)
        .reduceGroups { (mt1, mt2) =>
          MyType(
            name = mt1.name, // Just pick one since this is grouped by key anyways
            x = mt1.x combine mt2.x,
            y = mt1.y combine mt2.y)
        }
        .map(_._2)
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
      val myTypesDs1 = spark.createDataset(myTypes1)
      val myTypesDs2 = spark.createDataset(myTypes2)

      val combined: Dataset[MyType] = Monoid[Dataset[MyType]].combine(myTypesDs1, myTypesDs2)
      combined.show
    }

    spark.stop()
  }
}
