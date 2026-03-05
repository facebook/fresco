/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.cache

import com.facebook.common.util.ExceptionWithNoStacktrace

/** Exception to indicate an image was not found in the cache. */
class CacheMissException(detailMessage: String?) : ExceptionWithNoStacktrace(detailMessage!!)
