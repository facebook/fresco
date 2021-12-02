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
  final String LEGACY = "legacy";
  final String LEGACY_DEFAULT_PARAMS = "legacy_default_params";
  final String DUMMY = "dummy";
  final String DUMMY_WITH_TRACKING = "dummy_with_tracking";
  final String EXPERIMENTAL = "experimental";

  final String DEFAULT = LEGACY;
}
