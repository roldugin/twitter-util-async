/* Copyright (C) 2012-2014 EPFL
 * Copyright (C) 2012-2014 Typesafe, Inc.
 * Copyright (C) 2014 Foursquare Labs, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the EPFL nor the names of its contributors
 *      may be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.foursquare.common.async

import com.twitter.util.{Future, FuturePool}
import scala.language.experimental.macros

/**
  * Async blocks provide a direct means to work with [[com.twitter.util.Future]].
  *
  * For example, to use an API that fetches a web page to fetch
  * two pages and add their lengths:
  *
  * {{{
  *  import com.twitter.util.{Future, FuturePool}
  *  import com.foursquare.common.async.Async.{async, await}
  *
  *  def fetchURL(url: URL): Future[String] = async(FuturePool.unboundedPool) { ... }
  *
  *  val sumLengths: Future[Int] = async {
  *    val body1 = fetchURL("http://scala-lang.org")
  *    val body2 = fetchURL("http://docs.scala-lang.org")
  *    await(body1).length + await(body2).length
  *  }
  * }}}
  *
  * Note that in the following program, the second fetch does *not* start
  * until after the first. If you need to start tasks in parallel, you must do
  * so before `await`-ing a result.
  *
  * {{{
  *  val sumLengths: Future[Int] = async {
  *    await(fetchURL("http://scala-lang.org")).length + await(fetchURL("http://docs.scala-lang.org")).length
  *  }
  * }}}
  *
  * *IMPORTANT* By default Twitter futures are executed in the calling thread and are thus blocking.
  * This is contrary to [[scala.concurrent.Future]] with global execution context.
  * `async` method that takes a [[FuturePool]] is provided for true non-blocking execution.
  *
  * If this is intended to be used in a Finagle service, it is better to keep request processing on the same thread,
  * and only use truly asynchronous Futures when making a call to a database or a remote service.
  */
object Async {
  /**
   * Run the block of code `body` in the caller thread. `body` may contain calls to `await` when the results of
   * a `Future` are needed. The remainder of `body` will be executed in the same thread as the `await`-ed `Future`.
   *
   * *IMPORTANT* By default Twitter futures are executed in the calling thread and are thus blocking.
   * This is contrary to [[scala.concurrent.Future]] with global execution context.
   * `async` method that takes a [[FuturePool]] is provided for true non-blocking execution.
   *
   * If this is intended to be used in a Finagle service, it is better to keep request processing on the same thread,
   * and only use truly asynchronous Futures when making a call to a database or a remote service.
   */
  def async[T](body: => T): Future[T] = macro TwitterAsyncImpl.asyncImplSynchronous[T]

  /**
   * Run the block of code `body` in a future pool. `body` may contain calls to `await` when the results of
   * a `Future` are needed. The remainder of `body` will be executed in the same thread as the `await`-ed `Future`.
   */
  def async[T](futurePool: FuturePool)(body: => T): Future[T] = macro TwitterAsyncImpl.asyncImplWithFuturePool[T]

  /**
   * Non-blocking await on the result of `awaitable`. This may only be used directly within an enclosing `async` block.
   *
   * Internally, this will register the remainder of the code in enclosing `async` block as a callback
   * in the `onComplete` handler of `awaitable`, and will execute on the same thread as the `awaitable`.
   */
  @scala.annotation.compileTimeOnly("`await` must be enclosed in an `async` block")
  def await[T](awaitable: Future[T]): T = ??? // No implementation here, as calls to this are translated to `onComplete` by the macro.
}
