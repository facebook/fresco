/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:Suppress("DEPRECATION_ERROR", "EXTENSION_SHADOWED_BY_MEMBER")

package com.facebook.imagepipeline.backends.okhttp3

import java.util.concurrent.ExecutorService
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody

// okhttp 3.x compatibility shims.
//
// This backend's source uses okhttp 4.x property accessors (e.g. `response.body`). These extension
// properties back-fill those accessors by delegating to the okhttp 3.x method forms, so the same
// source also compiles against okhttp 3.x. The file is compiled unconditionally: on okhttp 4.x the
// real member properties shadow these extensions, so they are simply unused. That is why the
// delegating bodies need `DEPRECATION_ERROR` suppressed -- on 4.x the 3.x method forms they call
// are `DeprecationLevel.ERROR` -- and `EXTENSION_SHADOWED_BY_MEMBER` suppressed, since being
// shadowed on 4.x is the intended outcome rather than a mistake.
//
// Compiling it unconditionally is deliberate. Gating it on an exact okhttp version meant every new
// version string needed a matching `srcs` select key here, and a miss failed the build in a way
// that pointed at this backend rather than at the version that was added. Temporary: remove once
// every consumer is on okhttp 4.x.

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
