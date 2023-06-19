/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import androidx.fragment.app.Fragment

data class ExampleItem(
    val title: String,
    val helpText: String? = null,
    val backstackTag: String? = null,
    val createFragment: () -> Fragment
) {
  constructor(
      title: String,
      lithoSample: LithoSample,
      helpText: String? = null,
      backstackTag: String? = null
  ) : this(title, helpText, backstackTag, { LithoSampleHostFragment(lithoSample, helpText) })

  val itemId = title.hashCode()
}
