/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Image source that can be passed to Fresco's image components. New image sources can be created
 * via {@link ImageSourceProvider}.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface ImageSource {}
