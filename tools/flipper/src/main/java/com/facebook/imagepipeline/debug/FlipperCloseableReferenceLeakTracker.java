/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.debug;

import android.util.Log;
import com.facebook.common.references.SharedReference;
import java.io.Closeable;

public class FlipperCloseableReferenceLeakTracker implements CloseableReferenceLeakTracker {

  @Override
  public void trackCloseableReferenceLeak(SharedReference<Closeable> reference) {
    Log.w("FRESCO", "Unimplemented: Track closeable reference leak.");
  }
}
