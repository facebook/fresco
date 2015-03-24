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

import java.lang.Integer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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

  // ArrayList

  /**
   * Creates a <i>mutable</i>, empty {@code ArrayList} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableList#of()} instead.
   *
   * @return a new, empty {@code ArrayList}
   */
  public static <E> ArrayList<E> newArrayList() {
    return new ArrayList<E>();
  }

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
    ArrayList<E> list = new ArrayList<E>(capacity);
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

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given
   * elements.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use {@link ImmutableList#copyOf(Iterator)} instead.
   *
   * @param elements the elements that the list should contain, in order
   * @return a new {@code ArrayList} containing those elements
   */
  public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
    checkNotNull(elements);
    // Let ArrayList's sizing logic work, if possible
    return (elements instanceof Collection)
        ? new ArrayList<E>((Collection<E>) elements)
        : newArrayList(elements.iterator());
  }

  /**
   * Creates a <i>mutable</i> {@code ArrayList} instance containing the given
   * elements.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use {@link ImmutableList#copyOf(Iterator)} instead.
   *
   * @param elements the elements that the list should contain, in order
   * @return a new {@code ArrayList} containing those elements
   */
  public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
    checkNotNull(elements);
    ArrayList<E> list = newArrayList();
    while (elements.hasNext()) {
      list.add(elements.next());
    }
    return list;
  }

  /**
   * Creates an {@code ArrayList} instance backed by an array of the
   * <i>exact</i> size specified; equivalent to
   * {@link ArrayList#ArrayList(int)}.
   *
   * <p><b>Note:</b> if you know the exact size your list will be, consider
   * using a fixed-size list ({@link Arrays#asList(Object[])}) or an {@link
   * ImmutableList} instead of a growable {@link ArrayList}.
   *
   * <p><b>Note:</b> If you have only an <i>estimate</i> of the eventual size of
   * the list, consider padding this estimate by a suitable amount, or simply
   * use {@link #newArrayListWithExpectedSize(int)} instead.
   *
   * @param initialArraySize the exact size of the initial backing array for
   *     the returned array list ({@code ArrayList} documentation calls this
   *     value the "capacity")
   * @return a new, empty {@code ArrayList} which is guaranteed not to resize
   *     itself unless its size reaches {@code initialArraySize + 1}
   * @throws IllegalArgumentException if {@code initialArraySize} is negative
   */
  public static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) {
    return new ArrayList<E>(initialArraySize);
  }

  /**
   * Creates an {@code ArrayList} instance sized appropriately to hold an
   * <i>estimated</i> number of elements without resizing. A small amount of
   * padding is added in case the estimate is low.
   *
   * <p><b>Note:</b> If you know the <i>exact</i> number of elements the list
   * will hold, or prefer to calculate your own amount of padding, refer to
   * {@link #newArrayListWithCapacity(int)}.
   *
   * @param estimatedSize an estimate of the eventual {@link List#size()} of
   *     the new list
   * @return a new, empty {@code ArrayList}, sized appropriately to hold the
   *     estimated number of elements
   * @throws IllegalArgumentException if {@code estimatedSize} is negative
   */
  public static <E> ArrayList<E> newArrayListWithExpectedSize(
      int estimatedSize) {
    return new ArrayList<E>(computeArrayListCapacity(estimatedSize));
  }
}
