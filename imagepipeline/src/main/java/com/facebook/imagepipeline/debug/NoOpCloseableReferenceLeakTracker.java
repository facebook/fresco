/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.common.references.SharedReference;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpCloseableReferenceLeakTracker implements CloseableReferenceLeakTracker {

  @Override
  public void trackCloseableReferenceLeak(
      SharedReference<Object> reference, @Nullable Throwable stacktrace) {}

  @Override
  public void setListener(@Nullable Listener listener) {}

  @Override
  public boolean isSet() {
    return false;
  }
}
