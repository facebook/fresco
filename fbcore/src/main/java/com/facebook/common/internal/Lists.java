/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.common.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.facebook.common.internal.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to {@link List} instances. Also see this
 * class's counterparts {@link Sets}, {@link Maps} and {@link Queues}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Lists">
 * {@code Lists}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Lists {
  private Lists() {}

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given
   * elements.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use an overload of {@link ImmutableList#of()} (for varargs) or
   * {@link ImmutableList#copyOf(Object[])} (for an array) instead.
   *
   * @param elements the elements that the list should contain, in order
   * @return a new {@code ArrayList} containing those elements
   */
  public static <E> ArrayList<E> newArrayList(E... elements) {
    checkNotNull(elements); // for GWT
    // Avoid integer overflow when a large array is passed in
    int capacity = computeArrayListCapacity(elements.length);
    ArrayList<E> list = new ArrayList<>(capacity);
    Collections.addAll(list, elements);
    return list;
  }

  @VisibleForTesting static int computeArrayListCapacity(int arraySize) {
    Preconditions.checkArgument(arraySize >= 0);
    long desiredSize = 5L + arraySize + (arraySize / 10);

    if (desiredSize > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (desiredSize < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) desiredSize;
  }
}
