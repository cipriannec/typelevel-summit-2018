package org.longcao

import cats.kernel.{ CommutativeMonoid, Monoid }

import org.apache.spark._
import org.apache.spark.sql._

object SimpleFolds {
  // Using List#fold is O(n) and requires _associativity_ but not necessarily _commutativity_.
  def fold[T](list: List[T])(implicit m: Monoid[T]): T = {
    list.fold(m.empty) { (t1, t2) =>
      m.combine(t1, t2)
    }
  }

  // Using RDD#fold is theoretically O(log n) and requires associativity _and_ commutativity.
  // Folds partitions in parallel first, then a final fold across those results.
  // See: https://spark.apache.org/docs/2.2.1/api/scala/index.html#org.apache.spark.rdd.RDD@fold(zeroValue:T)(op:(T,T)=>T):T
  def fold[T](ds: Dataset[T])(implicit cm: CommutativeMonoid[T]): T = {
    ds.rdd.fold(cm.empty) { (t1, t2) =>
      cm.combine(t1, t2)
    }
  }

  // CommutativeMonoid is a Monoid with the additional law of commutativity.
  // This instance exists in cats but is re-implemented here for illustrative purposes.
  val addingMonoid = new CommutativeMonoid[Int] {
    override def empty: Int = 0
    override def combine(x: Int, y: Int): Int = x + y
  }

  def main(args: Array[String]): Unit = {
    implicit val spark = SparkSession.builder().master("local[*]").getOrCreate()
    import spark.implicits._
    spark.sparkContext.setLogLevel("ERROR")

    val nums: List[Int] = (1 to 100).toList

    // Fold a List[Int] to a single Int (sum).
    // Since a CommutativeMonoid[Int] _is_ a Monoid[Int], we can provide
    // the more powerful instance and it still works.
    // We'll pass in the monoid instance explicitly for illustration.
    val listSum: Int = fold(nums)(addingMonoid)
    println(s"Sum from List[Int]: $listSum")

    // Fold a Dataset[Int] to a single Int (sum) using the same monoid as before
    val datasetSum: Int = fold(spark.createDataset(nums))(addingMonoid)
    println(s"Sum from Dataset[Int]: $datasetSum")

    spark.stop()
  }
}
