package build

import mill._
import mill.scalalib._

object Deps {}

object core extends ScalaModule {
  def scalaVersion = "2.12.18"

  override def ivyDeps = Agg(
    ivy"com.github.scopt::scopt:4.1.1-M3",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.5"
  )

  override def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  override def prependShellScript = ""

  object test extends ScalaTests {
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.8.5")

    def testFramework = "utest.runner.Framework"

    override def forkArgs =
      Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }
}

object rddApp extends ScalaModule {
  override def compileIvyDeps = Agg(
    ivy"org.apache.spark::spark-core:3.5.5",
    ivy"org.apache.spark::spark-sql:3.5.5"
  )

  def scalaVersion = core.scalaVersion
  override def moduleDeps = Seq(core)
  override def mainClass = Some("rdd.Main")
}

object datasetApp extends ScalaModule {
  override def compileIvyDeps = Agg(
    ivy"org.apache.spark::spark-core:3.5.5",
    ivy"org.apache.spark::spark-sql:3.5.5"
  )

  def scalaVersion = core.scalaVersion
  override def moduleDeps = Seq(core)
  override def mainClass = Some("dataset.Main")
}
