/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

interface PrefetchConfig {
  fun prefetchInOnPrepare(): Boolean

  fun prefetchInOnBoundsDefinedForDynamicSize(): Boolean

  fun prefetchTargetOnPrepare(): PrefetchTarget

  fun prefetchTargetOnBoundsDefined(): PrefetchTarget

  fun closePrefetchDataSourceOnBindorOnMount(): Boolean
}
