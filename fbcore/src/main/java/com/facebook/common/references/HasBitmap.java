/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import com.facebook.infer.annotation.Nullsafe;

/** Marker interface for closeable references containing a bitmap */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface HasBitmap {}
