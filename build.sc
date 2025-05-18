package build

import mill._
import mill.scalalib._

object foo extends ScalaModule {
  def scalaVersion = "3.3.6"

  override def ivyDeps = Agg(
    ivy"org.apache.spark::spark-core:3.5.5".withDottyCompat(scalaVersion()),
    ivy"org.apache.spark::spark-sql:3.5.5".withDottyCompat(scalaVersion()),
    ivy"io.github.vincenzobaz::spark-scala3-encoders:0.2.6",
    ivy"io.github.vincenzobaz::spark-scala3-udf:0.2.6",
  )

  override def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")

  override def prependShellScript = ""

  object test extends ScalaTests {
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.8.5")

    def testFramework = "utest.runner.Framework"

    override def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }

}