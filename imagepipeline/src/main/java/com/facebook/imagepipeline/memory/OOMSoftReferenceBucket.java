/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.OOMSoftReference;
import com.facebook.infer.annotation.Nullsafe;
import java.util.LinkedList;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/** A Bucket that uses OOMSoftReferences to store its free list. */
@NotThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
class OOMSoftReferenceBucket<V> extends Bucket<V> {

  private LinkedList<OOMSoftReference<V>> mSpareReferences;

  public OOMSoftReferenceBucket(int itemSize, int maxLength, int inUseLength) {
    super(itemSize, maxLength, inUseLength, false);
    mSpareReferences = new LinkedList<>();
  }

  @Override
  public @Nullable V pop() {
    OOMSoftReference<V> ref = (OOMSoftReference<V>) mFreeList.poll();
    Preconditions.checkNotNull(ref);
    V value = ref.get();
    ref.clear();
    mSpareReferences.add(ref);
    return value;
  }

  @Override
  void addToFreeList(V value) {
    OOMSoftReference<V> ref = mSpareReferences.poll();
    if (ref == null) {
      ref = new OOMSoftReference<>();
    }
    ref.set(value);
    mFreeList.add(ref);
  }
}
