/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.data;

/** Simple Decorator for the SimpleAdapter */
public interface Decorator<E> {

  /**
   * We use this to decorate an object E
   *
   * @param decoratee The SimpleAdapter to decorate
   * @param position The position of the object to decorate
   * @return The decorated object
   */
  E decorate(SimpleAdapter<E> decoratee, int position);
}
