import mill._

trait CModule extends Module {

  def sources = T.source(millSourcePath / "src")

  // cc
  def cc: String = "clang"

  def binName: String

  final def compile = T {
    val allSources = os.walk(sources().path)
    os.makeDir.all(T.dest / "obj")
    allSources.foreach { filePath =>
      println(s"building $filePath into .o file...")

      val fileName = filePath.segments.toList.last

      if (filePath.ext == "c") {

        val objFile =
          fileName
            .dropRight(2)
            .concat(".o")

        val objPath =
          T.dest / "obj" / objFile

        os.proc(cc, "-Wall", "-Wextra", "-g", "-c", filePath, "-o", objPath)
          .call(cwd = T.dest)
      }
    }

    println(s"convert all .c files into .o files!")

    val result = T.dest / binName

    val objects = os.walk(T.dest / "obj").filter(_.ext == "o")

    os.proc(cc, "-o", result, objects)
      .call(cwd = T.dest)

    println(s"built $result binary!")

    PathRef(T.dest / binName)
  }

  final def run(args: String*) = T.command {
    val bin = compile()
    println(s"running ${bin.path} binary!")
    os.proc(bin.path, args)
      .call(cwd = super.millSourcePath, stdout = os.Inherit, stdin = os.Inherit)
    bin
  }
}

object mod extends CModule {
  def binName = "what"
}
