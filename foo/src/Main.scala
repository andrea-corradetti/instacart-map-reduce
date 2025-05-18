import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{StructField, StructType}
import scala3encoders.given


@main def main(): Unit = {
  val spark = SparkSession
    .builder()
    .appName("Spark SQL basic example")
    .config("spark.master", "local[*]")
    .getOrCreate()

  val schema = scala3encoders.encoder[Purchase].schema
  val purchases = spark.read.schema(schema).csv("local/order_products.csv").as[Purchase]
  purchases.printSchema()

  val pairs = purchases.groupByKey(_.orderId).flatMapGroups { case (orderId, iter) =>
    iter.map(_.itemId).toSet.toSeq.sorted.combinations(2).collect { case Seq(a, b) => (a, b) }.toSeq
  }

  val counts = pairs
    .groupByKey(identity)
    .count()
    .orderBy(col("count(1)").desc)

  val writableDf = counts
    .map { case ((a, b), count) => (a, b, count) }
    .toDF("item1", "item2", "count")

  writableDf.write.csv("local/out/counts_sorted")
  println(writableDf.take(10).mkString("- ", "\n- ", "\n---"))
}

