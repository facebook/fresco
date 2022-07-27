/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import java.lang.RuntimeException

/** Thrown if a bitmap pool cap or other limit on the number of bitmaps is exceeded. */
class TooManyBitmapsException(detailMessage: String?) : RuntimeException(detailMessage)
