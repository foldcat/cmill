import mill._

trait CModule extends Module {

  def sources = T.source(millSourcePath / "src")

  private final def isOfType(target: os.Path, test: String) =
    target.toString
      .endsWith("." + test)

  // cc
  def cc: String = "clang"

  def binName: String

  final def compile = T {
    val allSources = os.walk(sources().path)
    os.makeDir.all(T.dest / "obj")
    allSources.foreach { filePath =>
      println(s"building $filePath into .o file...")

      val fileName = filePath.segments.toList.last

      if (fileName.endsWith(".c")) {

        val objFile =
          fileName
            .dropRight(2)
            .concat(".o")

        val objPath =
          T.dest.toString
            .concat("/obj/")
            .concat(objFile)

        os.proc(cc, "-Wall", "-Wextra", "-g", "-c", filePath, "-o", objPath)
          .call(cwd = T.dest)
      }
    }

    println(s"convert all .c files into .o files!")

    val result = T.dest.toString.concat(s"/$binName")

    val objects = os.walk(T.dest / "obj").filter(isOfType(_, "o"))

    os.proc(cc, "-o", result, objects)
      .call(cwd = T.dest)

    println(s"built $result binary!")

    PathRef(T.dest / binName)
  }

  final def run = T {
    val bin = compile()
    println(s"running ${bin.path} binary!")
    os.proc(bin.path)
      .call(cwd = T.dest, stdout = os.Inherit, stdin = os.Inherit)
    bin
  }
}

object mod extends CModule {
  def binName = "what"
}
