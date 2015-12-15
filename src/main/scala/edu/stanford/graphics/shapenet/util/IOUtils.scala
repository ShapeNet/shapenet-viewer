package edu.stanford.graphics.shapenet.util

import java.nio.charset.{Charset, CharsetDecoder}
import java.nio.file.{StandardCopyOption, Files}

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorOutputStream, BZip2CompressorInputStream}
import java.io._
import java.nio.channels.FileChannel
import java.nio.{CharBuffer, ByteOrder, ByteBuffer}
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import scala.io.Source
import scala.util.matching.Regex
import scala.util.Random

/**
 *  IO utility routines
 *  @author Angel Chang
 *  @author Manolis Savva
 */
object IOUtils extends Loggable {
  // Can be serialized to a stream...
  trait Serializer[T] {
    def serialize(ostream: OutputStream, obj: T)
    def deserialize(istream: InputStream): Option[T]
  }

  object Verbosity extends Enumeration {
    type Verbosity = Value
    val DEBUG, QUIET = Value
  }

  val defaultEncoding = "UTF-8"
  val endl = sys.props("line.separator")
  val SLURP_BUFFER_SIZE = 16384

  def deserialize[T](path: String, serializer: Serializer[T]): Stream[T] = {
    val istream = fileInputStream(path)

    def _getStream(): Stream[T] = {
      logger.debug("istream is at " + istream.available())
      val next: Option[T] = serializer.deserialize(istream)
      if (next.isEmpty) {
        istream.close()
        Stream()
      } else {
        Stream.cons( next.get, _getStream() )
      }
    }

    _getStream()
  }

  def ensureDirname(str:String):String = if (str.endsWith(File.separator)) str else str + File.separator

  def getFilename(path: String, separators: Seq[Char] = "\\/"): String = {
    val slashIndex: Int = separators.map( s => path.lastIndexOf(s) ).max
    path.substring(slashIndex + 1)
  }

  def getParentDir(path: String, separators: Seq[Char] = "\\/"): String = {
    val slashIndex: Int = separators.map( s => path.lastIndexOf(s) ).max
    if (slashIndex >= 0) {
      path.substring(0, slashIndex)
    } else ""
  }

  def getExtension(url: String): String = {
    val filename: String = getFilename(url)
    val dotIndex: Int = filename.lastIndexOf('.')
    if (dotIndex < 0) "" else filename.substring(dotIndex + 1)
  }

  def stripExtension(filename: String, separators: Seq[Char] = "\\/"): String = {
    val slashIndex: Int = separators.map( s => filename.lastIndexOf(s) ).max
    val dotIndex: Int = filename.lastIndexOf('.')
    if (dotIndex <= slashIndex) filename else filename.substring(0, dotIndex)
  }

  val compressedExtensions = Set("gz", "gzip", "bz2", "bzip2")
  def stripExtension(filename: String, extensions: Iterable[String]): String = {
    for (x <- extensions) {
      if (filename.toLowerCase().endsWith("." + x)) {
        return filename.substring(filename.length - x.length - 1)
      }
    }
    filename
  }

  private def inputStreamAutoDecompress(file: String, verbosity: Verbosity.Value = Verbosity.DEBUG): InputStream = {
    inputStreamAutoDecompress(file, new BufferedInputStream(new FileInputStream(file)), verbosity)
  }

  private def inputStreamAutoDecompress(file: String, input: InputStream, verbosity: Verbosity.Value): InputStream = {
    if (verbosity == Verbosity.DEBUG) logger.info("Opening " + file)
    var is: InputStream = input
    if (file.endsWith(".gz") || file.endsWith(".gzip")) {
      is = new GZIPInputStream(is)
    } else if (file.endsWith("bz2") || file.endsWith(".bzip2")) {
      is = new BZip2CompressorInputStream(is)
    }
    is
  }

  private def inputStreamAutoDecompress(file: String, bytes: Array[Byte], verbosity: Verbosity.Value): InputStream = {
    if (verbosity == Verbosity.DEBUG) logger.info("Opening " + file)
    var is: InputStream = new ByteArrayInputStream(bytes)
    if (file.endsWith(".gz") || file.endsWith(".gzip")) {
      is = new GZIPInputStream(is)
    } else if (file.endsWith("bz2") || file.endsWith(".bzip2")) {
      is = new BZip2CompressorInputStream(is)
    }
    is
  }

  def slurpFile(inputFilename: String, encoding: String = defaultEncoding): String = {
   val inputStream = inputStreamAutoDecompress(inputFilename)
   return slurp(inputStream, encoding)
  }

  /**
    * Read the contents of an input stream, decoding it according to the given character encoding.
    * @param input The input stream to read from
    * @return The String representation of that input stream
    */
  def slurp(input: InputStream, encoding: String = defaultEncoding): String = {
    val buff: StringBuilder = new StringBuilder
    val decoder: CharsetDecoder = Charset.forName(encoding).newDecoder
    val chars: Array[Byte] = new Array[Byte](SLURP_BUFFER_SIZE)
    var amountRead = 0
    while (amountRead >= 0) {
      amountRead = input.read(chars, 0, SLURP_BUFFER_SIZE)
      if (amountRead >= 0) {
        val chunk: CharBuffer = decoder.decode(ByteBuffer.wrap(chars))
        buff.append(chunk.array, 0, amountRead)
      }
    }
    input.close
    buff.toString
  }

  def getByteArrayFromFile(inputFilename: String): Array[Byte] = {
    val inputStream = inputStreamAutoDecompress(inputFilename)
    org.apache.commons.io.IOUtils.toByteArray(inputStream)
  }

  def isWebFile(fileName: String): Boolean = {
    fileName.startsWith("http:") || fileName.startsWith("https:")
  }

  def isAbsolute(fileName: String): Boolean = {
    new File(fileName).isAbsolute
  }

  def getAbsolutePath(parent: String, path: String): String = {
    if (isWebFile(parent)) {
      if (isWebFile(path)) {
        return path
      } else {
        return parent + "/" + path
      }
    } else {
      new File(parent, path).getAbsolutePath
    }
  }

  def getLines(file: String, verbosity: Verbosity.Value = Verbosity.DEBUG): Iterator[String] = {
    Source.fromInputStream(fileInputStream(file, verbosity)).getLines()
  }

  def listFiles(file: String, r: Regex = null): Array[File] = {
    val f = new File(file)
    if (f.isDirectory) {
      if (r != null)
        f.listFiles().filter(x => r.pattern.matcher(x.getName).matches())
      else f.listFiles()
    } else {
      Array()
    }
  }

  def listDirs(file: String): Array[File] = {
    val f = new File(file)
    if (f.isDirectory) {
      f.listFiles().filter( x => x.isDirectory )
    } else {
      Array()
    }
  }

  def getMaxIdForRegex(file: String, regex: String): Int = {
    val r = new Regex(regex)
    val fileList = listFiles(file, r)
    val ids = fileList.map( file => file.getName match {
      case r(id) => id.toInt
    })
    if (ids.isEmpty) 0
    else ids.max
  }

  def loadMap(file: String, separator: Char = ','): Map[String,String] = {
    IOUtils.getLines(file).map{ l =>
      val fs = l.split(separator)
      if (fs.size > 1) fs(0) -> fs(1) else null
    }.filter(_ != null).toMap
  }

  def iterablesToJSONObject[T](its: Iterable[Iterable[T]], names: List[String]) : String = {
    assert(its.size == names.size, "Number of Iterators and given names do not match.")
    val start = "{" + endl
    val sep = "," + endl
    val end = "}" + endl

    val children = its.zip(names).map{case(it, name) => iterableToJSONArray(it, name)}
    children.mkString(start, sep, end)
  }

  // Convert iterable it with optional name to a JSON string representing an array of its elements
  def iterableToJSONArray[T](it : Iterable[T] , name : String = "nanashiArei") : String = {
    val start = "\"" + name + "\": [" + endl
    val sep = "," + endl
    val end = endl + "]" + endl
    it.map(item => "\"" + item.toString + "\"").mkString(start, sep, end)
  }

  // Split a dataset of items into a set of splits indicated by (percentage,filename) and then save each
  // into filename
  def datasetSplit[T](dataset: List[T], splits: List[(Double,String)]) : List[(List[T],String)] = {
    val rand: Random = Random
    rand.setSeed(12345678)
    var D = rand.shuffle(dataset)
    val N = D.size
    val partitions = splits.map {case (percent,file) =>
      val Nitems = math.round(N * percent).toInt
      val partition = D.take(Nitems)
      D = D.drop(Nitems)
      (partition,file)
    }
    partitions.foreach{case (partition,file) => IOUtils.saveList(file, partition)}
    partitions
  }

  def createDirs(dirs: String*) {
    createDirs(dirs.toIterable)
  }

  def createDirs(dirs: Iterable[String]) {
    for (dir <- dirs) {
      val f = new File(dir)
      f.mkdirs()
    }
  }

  def printList[T](pw: PrintWriter, items: Iterable[T]) {
    for (item <- items) {
      pw.println(item)
    }
    pw.flush()
  }

  def save[T](file: String, item: String) {
    val pw = filePrintWriter(file)
    pw.print(item)
    pw.close()
  }

  def saveList[T](file: String, items: Iterable[T]) {
    val pw = filePrintWriter(file)
    printList(pw, items)
    pw.close()
  }

  def saveIterablesToCSV[T](file: String, it: Iterable[Iterable[T]], header: Iterable[String] = null,
                            annotationColumns: Iterable[(String,String)] = null) {
    var rows = it.map(row => row.mkString(",")).toList
    var headerStr = if (header != null) header.mkString(",") else ""
    if (annotationColumns != null) {
      headerStr = headerStr + "," + annotationColumns.map(_._1).mkString(",")
      rows = rows.map(row => row + "," + annotationColumns.map(_._2).mkString(","))
    }
    val out = if (headerStr != "") {
      headerStr +: rows
    } else rows
    IOUtils.saveList(file, out)
  }

  def filePrintWriter(filename: String, append: Boolean = false): PrintWriter = {
    val outputStream = fileOutputStream(filename, append)
    new PrintWriter(outputStream)
  }

  def fileOutputStream(filename: String, append: Boolean = false): OutputStream = {
    logger.info("Opening file for output " + filename)
    val file: File = new File(filename)
    val parent: File = file.getParentFile
    if (parent != null) parent.mkdirs
    var outputStream: OutputStream = new BufferedOutputStream(new FileOutputStream(filename, append))
    if (filename.endsWith(".gz") || filename.endsWith(".gzip")) {
      outputStream = new GZIPOutputStream(outputStream)
    } else if (filename.endsWith("bz2")|| filename.endsWith(".bzip2")) {
      outputStream = new BZip2CompressorOutputStream(outputStream)
    }
    outputStream
  }

  def fileInputStream(filename: String, verbosity: Verbosity.Value = Verbosity.DEBUG): InputStream = {
    if (IOUtils.isWebFile(filename)) {
      // Some thing on the web....
      val is = WebUtils.inputStream(filename)
      if (is.isDefined) {
        IOUtils.inputStreamAutoDecompress(filename, is.get, verbosity)
      } else {
        throw new FileNotFoundException("Cannot open " + filename)
      }
    } else {
      IOUtils.inputStreamAutoDecompress(filename, verbosity)
    }
  }

  def fileReader(filename: String) = new BufferedReader(new InputStreamReader(fileInputStream(filename)))

  def fileSource(filename: String, charset: String = defaultEncoding): Source = {
    Source.fromInputStream(fileInputStream(filename), charset)
  }

  def removeFile(filename: String) {
    val file: File = new File(filename)
    if (file.exists) {
      file.delete
    }
  }

  def moveFile(filename: String, destname: String) {
    val file: File = new File(filename)
    var dest: File = new File(destname)
    if (dest.isDirectory) {
      dest = new File(dest, file.getName)
    }
    Files.move(file.toPath(), dest.toPath(), StandardCopyOption .REPLACE_EXISTING)
  }

  def copyFile(filename: String, destname: String) {
    val file: File = new File(filename)
    var dest: File = new File(destname)
    if (dest.isDirectory) {
      dest = new File(dest, file.getName)
    }
    Files.copy(file.toPath(), dest.toPath(), StandardCopyOption .REPLACE_EXISTING)
  }

  def isDir(filename: String): Boolean = {
    val file: File = new File(filename)
    file.canRead && file.isDirectory
  }

  def isReadableFileWithData(filename: String): Boolean = {
    val file: File = new File(filename)
    file.canRead && file.length > 0
  }

  def isReadableFileWithDataAndNotExpired(filename: String, expireMs: Long): Boolean = {
    val file: File = new File(filename)
    val readable = file.canRead && file.length > 0
    if (readable) {
      val lastModified = file.lastModified()
      (expireMs > System.currentTimeMillis) || (lastModified > expireMs)
    } else {
      false
    }
  }

  def writeBytes(filename: String, bytes: Array[Byte]) {
    val file: File = new File(filename)
    val parent: File = file.getParentFile
    if (parent != null) parent.mkdirs
    val out = fileOutputStream(filename)
    out.write(bytes)
    out.close()
  }

  def writeBytes(filename: String, bytes: ByteBuffer) {
    writeBytes(filename, bytes.array())
  }

  def readBytesUnsigned3(filename: String, dim1: Int, dim2: Int, dim3: Int): Array[Array[Array[Short]]] = {
    IOUtils.readBytesUnsigned(filename, dim1*dim2*dim3).grouped(dim3).map( x => x.toArray ).
      grouped(dim2).map ( x => x.toArray).toArray
  }

  def readBytesUnsigned2(filename: String, dim1: Int, dim2: Int): Array[Array[Short]] = {
    IOUtils.readBytesUnsigned(filename, dim1*dim2).grouped(dim2).map( x => x.toArray ).toArray
  }

  def readBytesUnsigned(filename: String, expected: Int): Array[Short] = {
    readBytes(new File(filename), expected).map( b =>  (0xff & b).toShort )
  }

  def readBytes3(filename: String, dim1: Int, dim2: Int, dim3: Int): Array[Array[Array[Byte]]] = {
    IOUtils.readBytes(filename, dim1*dim2*dim3).grouped(dim3).map( x => x.toArray ).
      grouped(dim2).map ( x => x.toArray).toArray
  }

  def readBytes2(filename: String, dim1: Int, dim2: Int): Array[Array[Byte]] = {
    IOUtils.readBytes(filename, dim1*dim2).grouped(dim2).map( x => x.toArray ).toArray
  }

  def readBytes(filename: String, expected: Int): Array[Byte] = {
    readBytes(new File(filename), expected)
  }

  def readBytes(file: File, expected: Int): Array[Byte] = {
    if (expected > 0 && file.length != expected) {
      throw new IOException("Unexpected number of bytes in: " + file.getAbsolutePath +
        ", expected " + expected + ", got " + file.length)
    }
    val raf: RandomAccessFile = new RandomAccessFile(file.getAbsoluteFile, "r")
    val fc: FileChannel = raf.getChannel
    val input: ByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length)
    val array = Array.ofDim[Byte](file.length.toInt)
    input.get(array)
    raf.close()
    array
  }

  def readFloats(filename: String, expected: Int, bo: ByteOrder = ByteOrder.BIG_ENDIAN): Array[Float] = {
    readFloats(new File(filename), expected, bo)
  }

  def readFloats(file: File, expected: Int, bo: ByteOrder): Array[Float] = {
    logger.info("Reading " + file.toString)
    val expectedFileSize = expected*4
    if (expected > 0 && file.length != expectedFileSize) {
      throw new IOException("Unexpected number of bytes in: " + file.getAbsolutePath +
        ", expected " + expectedFileSize + ", got " + file.length)
    }
    val raf: RandomAccessFile = new RandomAccessFile(file.getAbsoluteFile, "r")
    val fc: FileChannel = raf.getChannel
    val input: ByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length)
    input.order(bo)
    val n = file.length.toInt/4
    val array = Array.ofDim[Float](n)
    input.asFloatBuffer().get(array)
    raf.close()
    array
  }

  def readDoubles(filename: String, expected: Int, bo: ByteOrder = ByteOrder.BIG_ENDIAN): Array[Double] = {
    readDoubles(new File(filename), expected, bo)
  }

  def readDoubles(file: File, expected: Int, bo: ByteOrder): Array[Double] = {
    logger.info("Reading " + file.toString)
    val expectedFileSize = expected*8
    if (expected > 0 && file.length != expectedFileSize) {
      throw new IOException("Unexpected number of bytes in: " + file.getAbsolutePath +
        ", expected " + expectedFileSize + ", got " + file.length)
    }
    val raf: RandomAccessFile = new RandomAccessFile(file.getAbsoluteFile, "r")
    val fc: FileChannel = raf.getChannel
    val input: ByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length)
    input.order(bo)
    val n = file.length.toInt/8
    val array = Array.ofDim[Double](n)
    input.asDoubleBuffer().get(array)
    raf.close()
    array
  }

  def getMapped(filename: String, expected: Int,  bo: ByteOrder = ByteOrder.BIG_ENDIAN): (RandomAccessFile, ByteBuffer) = {
    getMapped(new File(filename), expected, bo)
  }

  def getMapped(file: File, expected: Int,  bo: ByteOrder): (RandomAccessFile, ByteBuffer) = {
    val expectedFileSize = expected
    if (expected > 0 && file.length != expectedFileSize) {
      throw new IOException("Unexpected number of bytes in: " + file.getAbsolutePath +
        ", expected " + expectedFileSize + ", got " + file.length)
    }
    val raf: RandomAccessFile = new RandomAccessFile(file.getAbsoluteFile, "r")
    val fc: FileChannel = raf.getChannel
    val input: ByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length)
    input.order(bo)
    (raf,input)
  }

  def hexString2ByteArray(hex: String): Array[Byte] = {
    val ints = hex.sliding(2,2).toArray.map(Integer.parseInt(_, 16))
    ints.map(_.toByte)
  }
  def byteArray2HexString(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString
  def hexString2Float(hex: String): Float = {
    import java.lang.{Long,Float}
    Float.intBitsToFloat(Long.parseLong(hex, 16).toInt)
  }
  def hexString2FloatArray(hex: String): Array[Float] = {
    val floatBuf = ByteBuffer.wrap(hexString2ByteArray(hex)).asFloatBuffer()
    var arr = Array[Float]()
    while (floatBuf.hasRemaining) arr = arr :+ floatBuf.get()
    arr
  }
}