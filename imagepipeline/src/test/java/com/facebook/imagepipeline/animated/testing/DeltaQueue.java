/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.testing;

/**
 * A queue of nodes sorted by timestamp for the purpose of implementing a scheduled executor.
 * Used for {@link ScheduledQueue}.
 *
 * @param <T> the type of node
 */
public class DeltaQueue<T> {
  /**
   * A node in the queue.
   *
   * @param <T> the type of node
   */
  private static class Node<T> {
    public final T value;
    public long delay;
    public Node<T> next = null;

    public Node(T value, long nanos) {
      this.value = value;
      this.delay = nanos;
    }
  }

  private Node<T> head = null;
  private int size;

  /**
   * Gets whether the queue is empty.
   *
   * @return whether the queue is empty
   */
  public boolean isEmpty() {
    return head == null;
  }

  /**
   * Gets whether there are items in the queue.
   *
   * @return whether there are items in the queue
   */
  public boolean isNotEmpty() {
    return !isEmpty();
  }

  /**
   * Gets the next item in the queue without removing it.
   *
   * @return the next item in the queue
   */
  public T next() {
    return head.value;
  }

  /**
   * Gets the delay until the next item in the queue.
   *
   * @return the delay until the next item
   */
  public long delay() {
    return head.delay;
  }

  /**
   * Adds a node to the queue.
   *
   * @param delay the delay
   * @param value the node to add
   */
  public void add(long delay, T value) {
    Node<T> newNode = new Node<T>(value, delay);

    Node<T> prev = null;
    Node<T> next = head;

    while (next != null && next.delay <= newNode.delay) {
      newNode.delay -= next.delay;
      prev = next;
      next = next.next;
    }

    if (prev == null) {
      head = newNode;
    } else {
      prev.next = newNode;
    }

    if (next != null) {
      next.delay -= newNode.delay;

      newNode.next = next;
    }
    size++;
  }

  /**
   * Simulates the passage of time.
   *
   * @param timeUnits the units of time that are desired to have passed
   * @return the time units that were not yet consumed.
   */
  public long tick(long timeUnits) {
    if (head == null) {
      return 0L;
    } else if (head.delay >= timeUnits) {
      head.delay -= timeUnits;
      return 0L;
    } else {
      long leftover = timeUnits - head.delay;
      head.delay = 0L;
      return leftover;
    }
  }

  /**
   * Pops the next element off the queue. Only valid to call if the head is ready to be pop'd
   * because of the passage of time.
   *
   * @return the next element off the queue.
   */
  public T pop() {
    if (head.delay > 0) {
      throw new IllegalStateException("cannot pop the head element when it has a non-zero delay");
    }

    T popped = head.value;
    head = head.next;
    size--;
    return popped;
  }

  /**
   * Removes the specified element from the queue.
   *
   * @param element the element to remove
   * @return whether the element was removed
   */
  public boolean remove(T element) {
    Node<T> prev = null;
    Node<T> node = head;
    while (node != null && node.value != element) {
      prev = node;
      node = node.next;
    }

    if (node == null) {
      return false;
    }

    if (node.next != null) {
      node.next.delay += node.delay;
    }

    if (prev == null) {
      head = node.next;
    } else {
      prev.next = node.next;
    }
    size--;
    return true;
  }

  /**
   * Gets the number of items in the queue. This returns in constant time.
   *
   * @return number of elements in the queue
   */
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName())
        .append("[");

    Node<T> node = head;
    while (node != null) {
      if (node != head) {
        sb.append(", ");
      }
      sb.append("+")
          .append(node.delay)
          .append(": ")
          .append(node.value);

      node = node.next;
    }
    sb.append("]");

    return sb.toString();
  }
}
