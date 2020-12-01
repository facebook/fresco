/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import androidx.fragment.app.Fragment

data class ExampleItem(
    val title: String,
    val backstackTag: String? = null,
    val createFragment: () -> Fragment
) {
  val itemId = title.hashCode()
}
