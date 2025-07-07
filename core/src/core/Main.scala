package core

import com.typesafe.scalalogging.Logger
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}

object Main {
  private val logger = Logger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val config = CliParser.parse(args) match {
      case Some(c) => c
      case None    => sys.exit(1)
    }

    val spark = SparkSession
      .builder()
      .appName("instacart-map-reduce")
      .getOrCreate()

    import spark.implicits._

    val schema = Encoders.product[Purchase].schema
    val purchases = spark.read.schema(schema).csv(config.input).as[Purchase]

    logger.info(s"""Using Schema:
        |${purchases.schema.treeString}
        |""".stripMargin)


    val combinations =
      purchases.groupByKey(_.orderId).flatMapGroups { (_, iter) =>
        iter.toStream.distinct.map(_.itemId).combinations(2).map {
          case Seq(itemA, itemB) => if (itemA <= itemB) (itemA, itemB) else (itemB, itemA)
        }
      }

    val counts =
      combinations.groupByKey(identity).count().sort($"count(1)".desc)

    val writableDf = counts
      .map { case ((a, b), count) => (a, b, count) }
      .toDF("item1", "item2", "count").cache()

    val head = writableDf.take(10)

    logger.info(s"""Printing first 10:
                   |${head.mkString("- ", "\n- ", "\n---")}
                   |""".stripMargin)

    val saveMode =
      if (config.forceWrite) SaveMode.Overwrite else SaveMode.ErrorIfExists

    logger.info(
      s"Writing to ${config.output} with force-write=${config.forceWrite}"
    )
    writableDf.write.mode(saveMode).csv(config.output)

    spark.stop()
  }
}
