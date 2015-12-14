package edu.stanford.graphics.shapenet.util

import scala.util.Random
import java.util.PriorityQueue
import scala.collection.JavaConversions._

/**
 * Given set of elements, samples n elements
 *
 * @author Angel Chang
 */
class BatchSampler {
  def random(size: Int, rand: Random = Random): Int = {
    rand.nextInt(size)
  }

  def sampleOne[E](samples: IndexedSeq[E], rand: Random = Random): Option[E] = {
    val total = samples.size
    if (total > 0) {
      val i = rand.nextInt(total)
      Some(samples(i))
    } else {
      None
    }
  }

  def sampleIndices(sampleSize: Int, totalSize: Int, shuffleOrder: Boolean = true, rand: Random = Random): IndexedSeq[Int] = {
    if (sampleSize >= totalSize) {
      val samples = (0 until totalSize).toIndexedSeq
      if (shuffleOrder) rand.shuffle(samples)
      else samples
    } else {
      val samples = Array.ofDim[Int](sampleSize)
      var t = 0  // # of total processed
      var m = 0  // # of samples selected
      while (m < sampleSize && t < totalSize) {
        val r = rand.nextFloat()
        if ( (totalSize - t)*r < sampleSize - m) {
          samples(m) = t
          m = m+1
        }
        t = t+1
      }
      if (shuffleOrder)
        rand.shuffle(samples.toIndexedSeq)
      else samples.toIndexedSeq
    }
  }

  def sampleWithoutReplacement[E](allSamples: Iterable[E], nSamples: Int, randOrder: Boolean = true, rand: Random = Random): IndexedSeq[E] = {
    val indexed = allSamples.toIndexedSeq
    if (nSamples < 10 || nSamples < indexed.size/2) {
      val indices = sampleIndices(nSamples, indexed.size, randOrder)
      indices.map( x => indexed(x) )
    } else {
      val permutedList = rand.shuffle(indexed)
      permutedList.slice(0, nSamples)
    }
  }

  def sampleWithReplacement[E](allSamples: Iterable[E], nSamples: Int, rand: Random = Random): IndexedSeq[E] = {
    val ordered = allSamples.toIndexedSeq
    for (i <- 0 until nSamples) yield {
      val r = rand.nextInt(ordered.size)
      ordered(r)
    }
  }

}
