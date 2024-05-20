import mill._

trait CModule extends Module {
  def sources = T.source(millSourcePath / "src")
  def cc: String = "clang"
  def cflags: Seq[String] = Seq.empty
  def name: String
  def assemble: Target[PathRef]
  def compile: Target[PathRef] = T {
    val allSources =
      os.walk(sources().path)
        .filter(_.ext == "c")

    val srcCount = allSources.count(z => true)
    var completed = 0

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
          "-c",
          filePath,
          "-o",
          objPath
        ).call(cwd = T.dest)

        completed += 1
        println(s"[$completed/$srcCount] compiled $filePath")
      }
    }
    PathRef(T.dest)
  }
}

trait CArchive extends CModule {
  def ar: String = "ar"
  final def assemble = T {
    val objPath = compile().path
    val result = T.dest / "lib".concat(name).concat(".a")

    val objects = os.walk(objPath).filter(_.ext == "o")

    os.proc(ar, "rcs", result, objects)
      .call(cwd = T.dest)

    println(s"assembled $result")

    PathRef(T.dest / name)
  }
}

trait CBinary extends CModule {
  final def assemble = T {
    val objPath = compile().path
    val result = T.dest / name
    val objects = os.walk(objPath).filter(_.ext == "o")

    os.proc(cc, cflags, "-o", result, objects)
      .call(cwd = T.dest)

    println(s"assembled $result")

    PathRef(T.dest / name)
  }

  final def run(args: String*) = T.command {
    val bin = assemble()
    println(s"executing ${bin.path}")
    os.proc(bin.path, args)
      .call(
        cwd = super.millSourcePath,
        stdout = os.Inherit,
        stdin = os.Inherit
      )
  }
}

object sec extends CArchive {
  def name = "sec"
  def cflags = Seq("-O3")
}

object mod extends CBinary {
  def name = "mod"
  def cflags = Seq("-O3")
}
