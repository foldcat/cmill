import mill._

trait CModule extends Module {
  def sources = T.source(millSourcePath / "src")
  def cc: String = "clang"
  def cflags: Seq[String] = Seq.empty
  def name: String
  def compile: Target[PathRef]
  def assemble: Target[PathRef]
  def run(args: String*): Command[os.CommandResult]
}

trait CBinary extends CModule {
  final def compile = T {
    val allSources = os.walk(sources().path)
    allSources.foreach { filePath =>
      val fileName = filePath.segments.toList.last

      if (filePath.ext == "c") {
        val objFile =
          fileName
            .dropRight(2)
            .concat(".o")
        val objPath =
          T.dest / objFile

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
    PathRef(T.dest)
  }

  final def assemble = T {
    val objPath = compile().path
    val result = T.dest / name
    val objects = os.walk(objPath).filter(_.ext == "o")

    os.proc(cc, cflags, "-o", result, objects)
      .call(cwd = T.dest)

    PathRef(T.dest / name)
  }

  final def run(args: String*) = T.command {
    val bin = assemble()
    os.proc(bin.path, args)
      .call(
        cwd = super.millSourcePath,
        stdout = os.Inherit,
        stdin = os.Inherit
      )
  }
}

object mod extends CBinary {
  def name = "mod"
  def cflags = Seq("-O3")
}
