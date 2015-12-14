package edu.stanford.graphics.shapenet.util

import java.io.File
import java.util.regex.Pattern
import java.text.SimpleDateFormat

/**
 * String Utilities
 * @author Angel Chang
 */
object StringUtils {
  private final val camelCasePattern: Pattern = Pattern.compile("([a-z])([A-Z])")
  final val underscorePattern: Pattern = Pattern.compile("_")
  final val whitespacePattern: Pattern = Pattern.compile("\\s+")
  val datetimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss")
  val datetimeFormatMillis = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS")

  def ensureDir(str:String):String = if (str.endsWith(File.separator)) str else str + File.separator

  def formatDatetime(millis: Long): String = {
    datetimeFormat.format(millis)
  }

  def formatMillis(millis: Long): String = {
    datetimeFormatMillis.format(millis)
  }

  def currentDatetime(): String = {
    formatDatetime(System.currentTimeMillis())
  }

  def toMillis(str:String): Long = {
    datetimeFormat.parse(str).getTime
  }

  // If a string begins and ends with '"', trims the quotes
  def trimQuotes(str: String): String = {
    if (str != null && str.length > 0) {
      if (str.head == '"' && str.last == '"') {
        return str.substring(1, str.length-1)
      }
    }
    return str
  }

  // Converts string of the form LivingRoom to Living Room
  def camelCaseToText(cc: String, lowercase: Boolean = false): String = {
    if (cc != null) {
      val s = camelCasePattern.matcher(cc).replaceAll("$1 $2")
      if (lowercase) s.toLowerCase()
      else s
    } else null
  }

  def splitCamelCase(cc: String): Array[String] = {
    val s = camelCasePattern.matcher(cc).replaceAll("$1 $2")
    s.split(" ")
  }

  def textToCamelCase(x: String): String = {
    if (x != null)
      x.split("\\s+").map( a => a.capitalize ).mkString("")
    else null
  }

  // Converts string of the form living_room to living room
  def delimitedToText(x: String, delimiterPattern: Pattern = underscorePattern, lowercase: Boolean = false): String = {
    if (x != null) {
      val s = delimiterPattern.matcher(x).replaceAll(" ")
      if (lowercase) s.toLowerCase()
      else s
    } else null
  }

  def textToDelimited(x: String, delimiter: String = "_", lowercase: Boolean = false): String = {
    if (x != null) {
      val s = whitespacePattern.matcher(x).replaceAll(delimiter)
      if (lowercase) s.toLowerCase()
      else s
    } else null
  }

  def toCamelCase(s: String) = {
    val t = StringUtils.delimitedToText(s)
    StringUtils.textToCamelCase(t)
  }

  def toLowercase(s: String, delimiter: String = " ") = {
    val t = StringUtils.camelCaseToText(s, lowercase = true)
    StringUtils.textToDelimited(t, delimiter)
  }

  def isAllDigits(s: String): Boolean = {
    s.matches("^\\d*$")
  }

  def isInt(s: String) = isAllDigits(s)

  private def _vowels = Set('a','A','e','E','i','I','o','O','u','U','y','Y')
  def isVowel(ch: Char): Boolean = {
    _vowels.contains(ch)
  }

  def toVerticalString[T<:IndexedSeq[String]](rows: Seq[T]): String = {
    val maxLengths = new scala.collection.mutable.ArrayBuffer[Int]()
    for (row <- rows) {
      for (j <- maxLengths.size until row.length) {
        maxLengths.append(0)
      }
      for (i <- 0 until row.length) {
        val length = row(i).length
        if (length > maxLengths(i)) {
          maxLengths(i) = length
        }
      }
    }
    val sb = new StringBuilder()
    for (row <- rows) {
      if (sb.length > 0) {
        sb.append("\n")
      }
      for (i <- 0 until row.length) {
        sb.append(row(i))
        val width = maxLengths(i) + 4
        for (j <- row(i).length until width)
          sb.append(" ")
      }
    }
    sb.toString
  }
}
