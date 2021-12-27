/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext

interface LithoSample {
  fun createLithoComponent(
      c: ComponentContext,
      uris: ImageUriProvider,
      callerContext: Any
  ): Component
}
