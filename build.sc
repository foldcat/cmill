import mill._

trait CModule extends Module {
  def sources = T.source(millSourcePath / "src")
  def cc: String = "clang"
  def cflags: Seq[String] = Seq.empty
  def name: String
  def archiveDeps: Seq[CArchive] = Seq.empty
  def compile: Target[PathRef]
  def upstream: T[Seq[PathRef]] = T {
    T.traverse(archiveDeps)(_.modPath)().flatten
  }
  def modPath = T { Seq(compile()) }

}

trait CArchive extends CModule {
  def ar: String = "ar"

  final def compile: Target[PathRef] = T {
    val allSources =
      os.walk(sources().path)
        .filter(_.ext == "c")

    val srcCount = allSources.count(z => true)
    var completed = 0

    allSources.foreach { filePath =>
      val fileName = filePath.segments.toList.last

      os.makeDir(T.dest / "obj")

      if (filePath.ext == "c") {
        val objFile =
          fileName
            .dropRight(2)
            .concat(".o")
        val obj =
          T.dest / "obj" / objFile

        os.proc(
          cc,
          cflags,
          "-Wall",
          "-Wextra",
          "-c",
          filePath,
          "-o",
          obj
        ).call(cwd = T.dest)

        completed += 1
        println(s"[$completed/$srcCount] compiled: $filePath")
      }
    }
    val libName = "lib".concat(name).concat(".a")
    val result = T.dest / libName

    val objects = os.walk(T.dest / "obj").filter(_.ext == "o")

    os.proc(ar, "rcs", result, objects)
      .call(cwd = T.dest)

    println(s"archive built: $result")
    PathRef(result)
  }
}

trait CBinary extends CModule {
  final def compile: Target[PathRef] = T {
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
        val obj =
          T.dest / objFile

        os.proc(
          cc,
          cflags,
          "-Wall",
          "-Wextra",
          "-c",
          filePath,
          "-o",
          obj
        ).call(cwd = T.dest)

        completed += 1
        println(s"[$completed/$srcCount] compiled $filePath")
      }
    }
    PathRef(T.dest)
  }

  final def assemble = T {
    val archives = upstream().map(_.path)
    val archiveInstruct =
      archives
        .map(ar =>
          Seq(
            "-L".concat({ ar / os.up }.toString), // -L/path/with/stuff.a
            "-l".concat(
              os.walk(ar / os.up)
                .filter(_.ext == "a")
                .map(inner =>
                  inner.segments.toList.last
                    .stripPrefix("lib")
                    .stripSuffix(".a")
                )(0) // -lstuff
            )
          )
        )
        .flatten

    val objPath = compile().path
    val result = T.dest / name
    val objects = os.walk(objPath).filter(_.ext == "o")

    os.proc(cc, cflags, archiveInstruct, "-o", result, objects)
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
  def archiveDeps = Seq(sec)
}
