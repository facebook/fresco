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

import com.facebook.infer.annotation.Nullsafe;

/**
 * A class that can supply objects of a single type. Semantically, this could be a factory,
 * generator, builder, closure, or something else entirely. No guarantees are implied by this
 * interface.
 *
 * @author Harry Heymann
 * @since 2.0 (imported from Google Collections Library)
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface Supplier<T> {
  /**
   * Retrieves an instance of the appropriate type. The returned object may or may not be a new
   * instance, depending on the implementation.
   *
   * @return an instance of the appropriate type
   */
  T get();
}
