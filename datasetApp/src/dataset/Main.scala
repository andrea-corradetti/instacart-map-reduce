package dataset

import com.typesafe.scalalogging.Logger
import shared.CliParser.getConfigOrExit
import shared.Shared.sortPair
import shared.{Config, Purchase}
import org.apache.spark.sql._

object Main {
  private val logger = Logger("DatasetMain")

  def main(args: Array[String]): Unit = {
    val config: Config = getConfigOrExit(args)

    val spark: SparkSession = SparkSession
      .builder()
      .appName("instacart-map-reduce-ds")
      .getOrCreate()

    import spark.implicits._

    val schema = Encoders.product[Purchase].schema
    val purchases = spark.read.schema(schema).csv(config.input).as[Purchase]

    logger.info(s"""Using dataset with schema:
        |${purchases.schema.treeString}
        |""".stripMargin)

    val writableDf: DataFrame = computeWithDataset(spark)(purchases)

    writableDf.cache()

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

  private def computeWithDataset(
      spark: SparkSession
  )(purchases: Dataset[Purchase]) = {
    import spark.implicits._

    val combinations =
      purchases.groupByKey(_.orderId).flatMapGroups { (_, itemIds) =>
        itemIds.toStream.distinct.map(_.itemId).combinations(2).map {
          case Seq(itemA, itemB) => sortPair(itemA, itemB)
        }
      }

    val counts: Dataset[((Int, Int), Long)] =
      combinations.groupByKey(identity).count().sort($"count(1)".desc)

    val writableDf: DataFrame = counts
      .map { case ((a, b), count) => (a, b, count) }
      .toDF("item1", "item2", "count")

    writableDf
  }

}
