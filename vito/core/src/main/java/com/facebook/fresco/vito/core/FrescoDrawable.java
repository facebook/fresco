/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class FrescoDrawable extends BaseFrescoDrawable implements Closeable {

  private @Nullable FrescoState mFrescoState;

  @Nullable
  public FrescoState getFrescoState() {
    return mFrescoState;
  }

  public void setFrescoState(@Nullable FrescoState frescoState) {
    mFrescoState = frescoState;
  }
}
