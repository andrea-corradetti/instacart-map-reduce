package foo

import scopt.OParser

import java.io.File

case class Config(
    input: File = new File("."),
    output: File = new File("."),
    verbose: Boolean = false
)

object CliParser {
  private val builder = OParser.builder[Config]
  private val parser = {
    import builder._
    OParser.sequence(
      programName("instacart-map-reduce"),
      opt[File]('i', "input")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(input = x))
        .text("input file is required"),
      opt[File]('o', "output")
        .required()
        .valueName("<file>")
        .action((x, c) => c.copy(output = x))
        .text("output location is required"),
      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("enable verbose mode")
    )
  }

  def parse(args: Array[String]): Option[Config] =
    OParser.parse(parser, args, Config()) match {
      case Some(config) =>
        println(s"Input: ${config.input}")
        println(s"Output: ${config.output}")
        println(s"Verbose: ${config.verbose}")
        Some(config)
      case _ =>
        None // arguments are bad, error message will have been displayed
    }
}
