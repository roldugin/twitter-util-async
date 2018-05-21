// Copyright 2011 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.common.async

import com.foursquare.common.async.Async.{async, await}
import org.junit.{Test, Assert => T}
import com.twitter.util.{Await, Duration, Future, FuturePool}
import java.util.concurrent.TimeUnit

class TwitterFutureSystemTest {
  @Test
  def testSynchronousAsync() {
    val f1: Future[Int] = Future.value(5)
    val f2: Future[Int] = Future.value(10)
    val f3 = async {
      val v1 = await(f1)
      val v2 = await(f2)
      Thread.sleep(10)
      v1 + v2
    }
    T.assertTrue(f3.map(_ => ()).isDone)
    T.assertEquals(Await.result(f3, Duration(2, TimeUnit.SECONDS)), 15)
  }

  @Test
  def testAsynchronousAsync() {
    val f1: Future[Int] = Future.value(5)
    val f2: Future[Int] = Future.value(10)
    val f3 = async(FuturePool.unboundedPool) {
      val v1 = await(f1)
      val v2 = await(f2)
      Thread.sleep(10)
      v1 + v2
    }
    T.assertFalse(f3.map(_ => ()).isDone)
    T.assertEquals(Await.result(f3, Duration(2, TimeUnit.SECONDS)), 15)
  }

  @Test
  def testExceptions() {
    val f1: Future[Int] = Future.exception(new IllegalArgumentException("foo"))
    val f2 = async {
      val v = await(f1)
      2 * v
    }
    try {
      Await.result(f2, Duration(2, TimeUnit.SECONDS))
      T.fail("should have thrown an exception")
    } catch {
      case _: IllegalArgumentException => // pass
      case _: Exception => T.fail("unexpected exception")
    }
  }
}
