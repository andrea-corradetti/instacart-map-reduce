import org.apache.spark.sql.SparkSession

@main def main(): Unit = {
  val spark = SparkSession
    .builder()
    .appName("Spark SQL basic example")
    .config("spark.master", "local")
    .getOrCreate()

  val df = spark.read.option("header", "true").csv("data/order_products.csv")

  println(f"test ${df.columns.mkString("Array(", ", ", ")")}")
}

