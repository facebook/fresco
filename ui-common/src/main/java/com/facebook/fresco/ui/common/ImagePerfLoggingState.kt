/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

open class ImagePerfLoggingState {
  var callingClassNameOnVisible: String? = null
  var rootContextNameOnVisible: String? = null
  var contextChainArrayOnVisible: Array<String>? = null
  var contextChainExtrasOnVisible: String? = null
  var contentIdOnVisible: String? = null
  var surfaceOnVisible: String? = null
  var subSurfaceOnVisible: String? = null
  var msSinceLastNavigationOnVisible: Long? = null
  var startupStatusOnVisible: String? = null
}
