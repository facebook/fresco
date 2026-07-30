/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3

import java.util.concurrent.ExecutorService
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody

// okhttp 3.x compatibility shims.
//
// This backend's source uses okhttp 4.x property accessors (e.g. `response.body`). These
// extension properties back-fill those accessors by delegating to the okhttp 3.x method forms,
// so the same source also compiles against okhttp 3.14.9. This file is compiled ONLY when okhttp
// resolves to 3.14.9 -- see the `srcs` select in BUCK. On okhttp 4.x it is excluded, and the real
// member properties are used instead (a member always shadows an extension). Temporary: remove
// once every consumer is on okhttp 4.x.

val OkHttpClient.dispatcher: Dispatcher
  get() = dispatcher()

val Dispatcher.executorService: ExecutorService
  get() = executorService()

val Response.body: ResponseBody?
  get() = body()

val Response.networkResponse: Response?
  get() = networkResponse()

val Response.code: Int
  get() = code()

val Response.headers: Headers
  get() = headers()
