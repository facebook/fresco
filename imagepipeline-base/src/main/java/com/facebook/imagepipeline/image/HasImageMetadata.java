/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import java.util.Map;
import javax.annotation.Nonnull;

public interface HasImageMetadata {

  @Nonnull
  Map<String, Object> getExtras();
}
