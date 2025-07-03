package foo

import org.apache.spark.sql.{Encoders, SparkSession}

object Main {
  def main(args: Array[String]): Unit = {
    val config = CliParser.parse(args) match {
      case Some(c) => c
      case None => sys.exit()
    }

    val spark = SparkSession
      .builder()
      .appName("instacart-map-reduce")
      .getOrCreate()

    import spark.implicits._

    val schema = Encoders.product[Purchase].schema
    val purchases = spark.read.schema(schema).csv(config.input.getPath).as[Purchase]
    purchases.printSchema()

    val combinations = purchases.groupByKey(_.orderId).flatMapGroups { (_, iter) =>
      iter.map(_.itemId).toSet.toSeq.sorted.combinations(2).map { case Seq(itemA, itemB) => (itemA, itemB) }
    }

    val counts = combinations.groupByKey(identity).count().sort($"count(1)".desc)

    val writableDf = counts
      .map { case ((a, b), count) => (a, b, count) }
      .toDF("item1", "item2", "count")

    writableDf.write.csv(config.output.getPath)


    println(writableDf.take(10).mkString("- ", "\n- ", "\n---"))
  }
}