/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.NotThreadSafe;

import java.util.LinkedList;

import com.facebook.common.references.OOMSoftReference;

/**
 * A Bucket that uses OOMSoftReferences to store its free list.
 */
@NotThreadSafe
class OOMSoftReferenceBucket<V> extends Bucket<V> {

  private LinkedList<OOMSoftReference<V>> mSpareReferences;

  public OOMSoftReferenceBucket(int itemSize, int maxLength, int inUseLength) {
    super(itemSize, maxLength, inUseLength);
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
