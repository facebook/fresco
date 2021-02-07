/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public interface FrescoVitoConfig {

  PrefetchConfig getPrefetchConfig();

  boolean submitFetchOnBgThread();

  boolean useBindOnly();

  boolean useNewReleaseCallback();
}
