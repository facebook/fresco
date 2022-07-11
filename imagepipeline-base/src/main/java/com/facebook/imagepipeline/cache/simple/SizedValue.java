/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache.simple;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;

class SizedValue {

  final CloseableReference<CloseableImage> value;
  final int size;

  public SizedValue(CloseableReference<CloseableImage> value, int size) {
    this.value = value;
    this.size = size;
  }
}
