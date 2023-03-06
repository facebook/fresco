/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.middleware

typealias Extras = Map<String, Any?>

interface HasExtraData {

  fun <E> putExtra(key: String, value: E?)

  fun <E> getExtra(key: String): E?

  fun <E> getExtra(key: String, valueIfNotFound: E? = null): E?

  fun getExtras(): Extras

  fun putExtras(extras: Extras)
}
