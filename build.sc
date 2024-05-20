import mill._

trait CModule extends Module {

  def sources = T.source(millSourcePath / "src")

  // cc
  def cc: String = "clang"

  def cflags: Seq[String] = Seq.empty

  def binName: Option[String] = None

  private final def extract =
    binName match {
      case Some(s) => s
      case None =>
        throw new java.lang.RuntimeException(
          "module is headless, set binName to use said feature"
        )
    }

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

        os.proc(
          cc,
          cflags,
          "-Wall",
          "-Wextra",
          "-g",
          "-c",
          filePath,
          "-o",
          objPath
        ).call(cwd = T.dest)
      }
    }

    println(s"converted all .c files into .o files!")

    PathRef(T.dest)
  }

  final def assemble = T {
    val objPath = compile().path
    val result = T.dest / extract
    val objects = os.walk(objPath / "obj").filter(_.ext == "o")

    os.proc(cc, "-o", result, objects)
      .call(cwd = T.dest)

    println(s"built $result binary!")

    PathRef(T.dest / extract)
  }

  final def run(args: String*) = T.command {
    val bin = assemble()
    println(s"running ${bin.path} binary!")
    os.proc(bin.path, args)
      .call(
        cwd = super.millSourcePath,
        stdout = os.Inherit,
        stdin = os.Inherit
      )
  }
}

object mod extends CModule {
  def binName = Some("what")
  def cflags = Seq("-O3")
}
