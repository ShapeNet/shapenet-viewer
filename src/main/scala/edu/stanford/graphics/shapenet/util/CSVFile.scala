package edu.stanford.graphics.shapenet.util

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import java.io.InputStreamReader

/**
 *  Utility for working with CSV file
 *  @author Angel Chang
 */
class CSVFile(fileName: String, charset: String = "UTF-8",
              // take care with the escape character - default CSVWriter uses '"',
              // CSVReader handles that automatically, but then escape should be '\0' (no character)
              separator: Char = ',', quote: Char = '"', escape: Char = '\0', trimWhitespace: Boolean = false,
              includesHeader: Boolean = false,
              headerFieldLength: Map[String,Int] = null) extends Traversable[Array[String]] with FieldIndexer  {
  private val fieldIndices = new collection.mutable.HashMap[String, Int]
  var fullHeader: Array[String]  = null
  var header: Array[String] = null

  def getIndices() = {
    if (includesHeader && fieldIndices.isEmpty) {
      readHeader(null)
    }
    fieldIndices.toMap
  }

  def getIndex(fieldName: String) = {
    if (includesHeader && fieldIndices.isEmpty) {
      readHeader(null)
    }
    fieldIndices.get(fieldName)
  }

  def getHeader() = {
    if (includesHeader && header == null) {
      readHeader(null)
    }
    fullHeader
  }

  // TODO: Fix this so entire file not not being rebuffered for this
  def rows = this.map( row => new CSVRow(row, this))

  def toMap[K,V](f: Array[String] => (K,V)): Map[K,V] = {
    val m = new collection.mutable.HashMap[K, V] {}
    this.foreach( row => m += f(row) )
    m.toMap
  }

  def toMap(keyField: Int): Map[String, Array[String]] = {
    val m = new collection.mutable.HashMap[String, Array[String]]
    this.foreach( row => m(row(keyField)) = row )
    m.toMap
  }

  def toMap(keyField: String): Map[String, Array[String]] = {
    toMap(index(keyField))
  }

  def rowToMap(row: Array[String]): Map[String,String] = {
    val m = new collection.mutable.HashMap[String, String]
    var i = 0
    row.foreach( item => { m(fullHeader(i)) = item; i=i+1 } )
    m.toMap
  }

  def toFieldMap[K](f: Array[String] => (K)): Map[K,Map[String,String]] = {
    val m = new collection.mutable.HashMap[K, Map[String,String]]
    this.foreach( row => m(f(row)) = rowToMap(row) )
    m.toMap
  }

  def toPairMap: Map[(String,String),String] = {
    val m = new collection.mutable.HashMap[(String,String),String]
    foreach( row => {
      val rowName = row(0)
      for (i <- 1 until row.size) {
        val v = row(i).trim
        val colName = fullHeader(i)
        if (v.nonEmpty) m((rowName,colName)) = v
      }
    })
    m.toMap
  }

  def toMap2D[K1,K2,V](f: Array[String] => (K1,K2,V)): Map[K1,Map[K2,V]] = {
    val m = new collection.mutable.HashMap[K1, collection.mutable.HashMap[K2,V]]
    this.foreach( row => {
      val t = f(row)
      m.getOrElseUpdate(t._1, new collection.mutable.HashMap[K2,V]())(t._2) = t._3
    })
    m.mapValues( hm => hm.toMap ).toMap
  }

  def toMap2DSym[K,V](f: CSVRow => (K,K,V,V,Boolean)): Map[K,Map[K,List[V]]] = {
    val m = new collection.mutable.HashMap[K, collection.mutable.HashMap[K,collection.mutable.MutableList[V]]]
    this.foreach( a => {
      val row = new CSVRow(a, this)
      val t = f(row)
      if (t._5) {
        m.getOrElseUpdate(t._1, new collection.mutable.HashMap[K,collection.mutable.MutableList[V]]())
          .getOrElseUpdate(t._2, collection.mutable.MutableList[V]()) += t._3
        m.getOrElseUpdate(t._2, new collection.mutable.HashMap[K,collection.mutable.MutableList[V]]())
          .getOrElseUpdate(t._1, collection.mutable.MutableList[V]()) += t._4
      }
    })
//    m.foreach( k1v => k1v._2.foreach( k2v => println(k1v._1 + "," + k2v._1 + "," + k2v._2.size )))
    m.mapValues( hm => hm.mapValues( ml => ml.toList ).toMap ).toMap
  }

  def toCSVMap(fieldName: String) = new CSVMap(toMap(fieldName), fieldIndices.toMap)

  def readHeader(csvReader: CSVReader = null) {
    val reader = if (csvReader != null) csvReader else open()
    try {
      header = reader.readNext().map(s => if (trimWhitespace) s.trim else s)
      fullHeader = new Array[String](header.length +
        (if (headerFieldLength != null) headerFieldLength.values.sum else 0))
      var i = 0
      header.foreach( name => {
        fieldIndices(name) = i
        var cnt = 1
        if (headerFieldLength != null) {
          cnt = headerFieldLength.getOrElse(name, 1)
        }
        if (cnt > 1) {
          for (j <- 0 until cnt) {
            fullHeader.update(i+j, name + "." + j)
          }
        } else {
          fullHeader.update(i, name)
        }
        i = i+cnt
      } )
    } finally {
      if (csvReader == null) reader.close()
    }
  }

  private def open() = {
    new CSVReader(new InputStreamReader(IOUtils.fileInputStream(fileName, verbosity = IOUtils.Verbosity.QUIET), charset), separator, quote, escape)
  }

  override def foreach[U](f: Array[String] => U) {
    val csvReader = open()
    try {
      if (includesHeader) {
        readHeader(csvReader)
      }
      var next = true
      while (next) {
        val values = csvReader.readNext()
        if (values != null) f(values.map(s => if (trimWhitespace) s.trim else s))
        else next = false
      }
    } finally {
      csvReader.close()
    }
  }
}

trait FieldIndexer {
  def contains(field: String) = getIndex(field).isDefined

  def index(field: String) = getIndex(field).getOrElse(-1)

  def getIndex(field: String): Option[Int]

  def splitField(row: Array[String], f: Any, separator: String = ","): Array[String] = {
    val v = f match {
      case name: String => field(row, name)
      case index: Int => field(row, index)
      case _ => throw new IllegalArgumentException("Invalid field index " + f)
    }
    if (v != null) {
      v.split(separator).filter(s => !s.isEmpty)
    } else Array()
  }

  def field(row: Array[String], fieldIndex: Int): String =
    if (fieldIndex >= 0 && fieldIndex < row.size) row(fieldIndex) else null

  def fields(row: Array[String], fieldIndex: Int, count: Int): Array[String] = {
    val start = fieldIndex
    row.slice(start, start+count)
  }

  def field(row: Array[String], fieldName: String): String = field(row, index(fieldName))

  def fields(row: Array[String], fieldName: String, count: Int): Array[String] = {
    val start = index(fieldName)
    row.slice(start, start+count)
  }

}

class CSVRow(val row: Array[String], val fieldIndexer: FieldIndexer) {
  def apply(fieldName: String): String = fieldIndexer.field(row, fieldName)
  def apply(i: Int): String = row(i)
  def get(fieldName: String):Option[String] =
    if (fieldIndexer.contains(fieldName)) Option(fieldIndexer.field(row, fieldName)) else None
  def get(i: Int):Option[String] = if (i >= 0 && i < row.length) Option(row(i)) else None
  def getOrElse(fieldName: String, default: String): String = get(fieldName).getOrElse(default)
  def getOrElse(i: Int, default: String) = get(i).getOrElse(default)
}

object CSVMap {
  def apply(filename: String, keyField: String): CSVMap = new CSVFile(filename, includesHeader = true).toCSVMap(keyField)
}

class CSVMap(val map: Map[String, Array[String]], val fieldIndices: Map[String, Int]) extends FieldIndexer  {
  def row(k: String) = new CSVRow(map.get(k).get, this)
  def get(k: String) = map.get(k)
  def get(k1: String, k2: String) = field(map.get(k1).get, k2)
  def getIndex(fieldName: String) = fieldIndices.get(fieldName)
  def rows = map.map( row => new CSVRow(row._2, this))
}

object CSVFile {

  def filterCSV(id: String, values: Set[String], in: String, out: String) {
    val csv = new CSVFile(in, includesHeader = true)
    val filteredRows = csv.rows.toSeq.filter(row => {values.contains(row(id))})

    // Write out
    val fileWriter = new CSVWriter(IOUtils.filePrintWriter(out))
    fileWriter.writeNext(csv.header)
    filteredRows.foreach( row => fileWriter.writeNext(row.row) )
    fileWriter.close()
  }

  def mergeCSVs(id: String, in1: String, in2: String, out: String) {
    val csv1 = new CSVFile(in1, includesHeader = true)
    val csv2 = new CSVFile(in2, includesHeader = true)
    val csv1map = csv1.toCSVMap(id)
    val csv2map = csv2.toCSVMap(id)

    val header1 = csv1.fullHeader
    val header2 = csv2.fullHeader
    assert(header1.contains(id) && header2.contains(id))
    val idIdx1 = csv1map.index(id)
    val idIdx2 = csv2map.index(id)

    val headerOut = id +: (header1.filterNot(s => s == id) ++ header2.filterNot(s => s == id))
    val allIds = csv1map.map.keySet.intersect(csv2map.map.keySet)

    val rowsOut = allIds.map(rowId => {
      val row1 = csv1map.row(rowId).row
      val row2 = csv2map.row(rowId).row
      rowId +: (row1.take(idIdx1) ++ row1.drop(idIdx1+1) ++ row2.take(idIdx2) ++ row2.drop(idIdx2+1))
    })

    // Write out
    val fileWriter = new CSVWriter(IOUtils.filePrintWriter(out))
    fileWriter.writeNext(headerOut)
    rowsOut.foreach( row => fileWriter.writeNext(row) )
    fileWriter.close()
  }
}