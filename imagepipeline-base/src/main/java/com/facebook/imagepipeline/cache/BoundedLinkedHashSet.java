/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.infer.annotation.Nullsafe;
import java.util.LinkedHashSet;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Nullsafe(Nullsafe.Mode.STRICT)
public class BoundedLinkedHashSet<E> {

  private int mMaxSize;
  private LinkedHashSet<E> mLinkedHashSet;

  public BoundedLinkedHashSet(final int maxSize) {
    mLinkedHashSet = new LinkedHashSet<>(maxSize);
    mMaxSize = maxSize;
  }

  public synchronized boolean contains(E o) {
    return mLinkedHashSet.contains(o);
  }

  public synchronized boolean add(E key) {
    if (mLinkedHashSet.size() == mMaxSize) {
      mLinkedHashSet.remove(mLinkedHashSet.iterator().next());
    }
    mLinkedHashSet.remove(key);
    return mLinkedHashSet.add(key);
  }
}
