package edu.stanford.graphics.shapenet.util

import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.util.ClientUtils
import org.apache.solr.client.solrj.{SolrQuery, SolrClient}
import org.apache.solr.common.{SolrDocumentList, SolrDocument}
import scala.collection.JavaConversions._
import org.apache.solr.client.solrj.response.QueryResponse

/**
 * Utilities for querying solr
 * @author Angel Chang
 */
object SolrUtils {
  def query(solrClient: SolrClient, solrQuery: SolrQuery,
            start: Int = 0, batchSize: Int = 1000, totalLimit: Int = -1): Stream[SolrDocument] = {
    if (batchSize <= 0) throw new IllegalArgumentException("Invalid batchSize " + batchSize + ": batchSize must be positive")
    def _queryHelper(buffered: Seq[SolrDocument], curStart: Int, count: Int, total: Long): Stream[SolrDocument] = {
      if (buffered.nonEmpty) {
        buffered.head #:: _queryHelper(buffered.tail, curStart, count, total)
      } else if (total >= 0 && count >= total) {
        Stream()
      } else {
        solrQuery.setRows(batchSize)
        solrQuery.setStart(curStart)
        //println("Querying with " + curStart)
        val responses = solrClient.query(solrQuery)
        val results = responses.getResults
        val newTotal = if (total < 0) { results.getNumFound } else total
        val needed = newTotal.toInt - count
        val n = results.size
        if (n == 0) {
          Stream()
        } else if (n >= needed) {
          // Done!!!
          results.take(needed).toStream
        } else {
          results.head #:: _queryHelper(results.tail, curStart + n, count + n, newTotal)
        }
      }
    }
    _queryHelper(Stream(), curStart = start, count = 0, total = totalLimit)
  }

  def getFieldCount(url: String, fieldName: String): Map[String, Long] = {
    getFieldCount(url, fieldName, "*:*")
  }

  def getFieldCount(url: String, fieldName: String, queryStr: String): Map[String, Long] = {
    getFieldCount(url, fieldName, queryStr, null)
  }

  def getFieldCount(url: String, fieldName: String, queryStr: String, filterQueries: Seq[String]): Map[String, Long] = {
    val server: SolrClient = new HttpSolrClient(url)
    val map: Map[String, Long] = getFieldCount(server, fieldName, queryStr)
    server.close
    map
  }

  def getFieldCount(server: SolrClient, fieldName: String): Map[String, Long] = {
    getFieldCount(server, fieldName, "*:*")
  }

  def getFieldCount(server: SolrClient, fieldName: String, queryStr: String): Map[String, Long] = {
    getFieldCount(server, fieldName, queryStr, null)
  }

  def getFieldCount(server: SolrClient, fieldName: String, queryStr: String, filterQueries: Seq[String]): Map[String, Long] = {
    val query: SolrQuery = new SolrQuery
    query.setQuery(queryStr)
    if (filterQueries != null) {
      query.setFilterQueries(filterQueries:_*)
    }
    query.setFacet(true)
    query.setFacetLimit(-1)
    query.setRows(0)
    query.addFacetField(fieldName)
    val response: QueryResponse = server.query(query)
    System.out.println("Look up counts for " + fieldName)
    val map = new scala.collection.mutable.HashMap[String, Long]
    val facetFields = response.getFacetFields
    import scala.collection.JavaConversions._
    for (f <- facetFields) {
      val values = f.getValues
      for (count <- values) {
        map.put(count.getName, count.getCount)
      }
    }
    //System.out.println("Got " + map.size + " entries")
    map.toMap
  }

  def queryAndProcessSolrResults(server: SolrClient, query: SolrQuery, p: (SolrDocument,Int) => _,
                                 batchSize: Int = 1000,
                                 totalLimit: Int = -1): Int = {
    if (batchSize <= 0) throw new IllegalArgumentException("Invalid batchSize " + batchSize + ": batchSize must be positive")
    var done: Boolean = false
    var start: Int = 0
    var count: Int = 0
    query.setRows(batchSize)
    while (!done) {
      query.setStart(start)
      val response: QueryResponse = server.query(query)
      val results: SolrDocumentList = response.getResults
      val total: Long = results.getNumFound
      start = (results.getStart + results.size).asInstanceOf[Int]
      done = (start >= total)
      import scala.collection.JavaConversions._
      for (doc <- results) {
        p(doc,count)
        count += 1
        if (totalLimit > 0 && count >= totalLimit) return count
      }
    }
    count
  }

  def makeQueryString(pairs: Seq[(String,String)], conj: String = "AND"): String = {
    pairs.map( x => x._1 + ":" + x._2 ).mkString( " " + conj + " " )
  }

  def makeFieldQueryString(fieldName: String, value: String): String = {
    fieldName + ":\"" + ClientUtils.escapeQueryChars(value) + "\""
  }

  def makeFieldQueryString(fieldName: String, parts: Seq[String], conj: String): String = {
    fieldName + ":(" + parts.mkString(" " + conj + " ") + ")"
  }

}
