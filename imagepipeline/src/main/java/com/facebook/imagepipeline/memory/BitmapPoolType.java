/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public @interface BitmapPoolType {
  String LEGACY = "legacy";
  String LEGACY_DEFAULT_PARAMS = "legacy_default_params";
  String DUMMY = "dummy";
  String DUMMY_WITH_TRACKING = "dummy_with_tracking";
  String EXPERIMENTAL = "experimental";

  String DEFAULT = LEGACY;
}
