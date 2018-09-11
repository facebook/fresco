/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.references.OOMSoftReference;
import java.util.LinkedList;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A Bucket that uses OOMSoftReferences to store its free list.
 */
@NotThreadSafe
class OOMSoftReferenceBucket<V> extends Bucket<V> {

  private LinkedList<OOMSoftReference<V>> mSpareReferences;

  public OOMSoftReferenceBucket(int itemSize, int maxLength, int inUseLength) {
    super(itemSize, maxLength, inUseLength, false);
    mSpareReferences = new LinkedList<>();
  }

  @Override
  public V pop() {
    OOMSoftReference<V> ref = (OOMSoftReference<V>) mFreeList.poll();
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
