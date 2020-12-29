/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import com.facebook.infer.annotation.Functional;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Generic tri-state enum for boolean values that can also be unset. */
@Nullsafe(Nullsafe.Mode.STRICT)
public enum TriState {
  YES,
  NO,
  UNSET,
  ;

  /** @return whether this value is set; that is, whether it is YES or NO. */
  @Functional
  public boolean isSet() {
    return this != UNSET;
  }

  /**
   * Returns the value of the {@link TriState} enum that corresponds to the specified {@code
   * boolean}.
   *
   * <p>This method deliberately declares {@code boolean} as its param type rather than {@link
   * Boolean} because:
   *
   * <ol>
   *   <li>Declaring {@link Boolean} would likely result in a bunch of unnecessary autoboxing.
   *   <li>Anyone who finds himself using a {@link Boolean} instead of a {@code boolean} for its
   *       nullability should replace the {@link Boolean} with a {@link TriState}, anyway.
   * </ol>
   */
  @Functional
  public static TriState valueOf(boolean bool) {
    return bool ? YES : NO;
  }

  @Functional
  public static TriState valueOf(@Nullable Boolean bool) {
    return bool != null ? valueOf(bool.booleanValue()) : TriState.UNSET;
  }

  /**
   * Returns the {@code boolean} value that corresponds to this {@link TriState}, if appropriate.
   *
   * @return {@code true} if {@code this} is {@link TriState#YES} or {@code false} if {@code this}
   *     is {@link TriState#NO}
   * @throws IllegalStateException if {@code this} is {@link TriState#UNSET}.
   */
  @Functional
  public boolean asBoolean() {
    switch (this) {
      case YES:
        return true;
      case NO:
        return false;
      case UNSET:
        throw new IllegalStateException("No boolean equivalent for UNSET");
      default:
        throw new IllegalStateException("Unrecognized TriState value: " + this);
    }
  }

  /**
   * Returns the {@code boolean} value that corresponds to this {@link TriState}, if appropriate.
   *
   * @param defaultValue default value to use if not set
   * @return {@code true} if {@code this} is {@link TriState#YES} or {@code false} if {@code this}
   *     is {@link TriState#NO} or {@code defaultValue} if {@code this} is {@link TriState#UNSET}.
   */
  @Functional
  public boolean asBoolean(boolean defaultValue) {
    switch (this) {
      case YES:
        return true;
      case NO:
        return false;
      case UNSET:
        return defaultValue;
      default:
        throw new IllegalStateException("Unrecognized TriState value: " + this);
    }
  }

  /**
   * Returns the {@code Boolean} value that corresponds to this {@link TriState}, if appropriate.
   *
   * @return {@link Boolean#TRUE} if {@code this} is {@link TriState#YES} or {@link Boolean#FALSE}
   *     if {@code this} is {@link TriState#NO} or {@code null} if {@code this} is {@link
   *     TriState#UNSET}.
   */
  @Functional
  public @Nullable Boolean asBooleanObject() {
    switch (this) {
      case YES:
        return Boolean.TRUE;
      case NO:
        return Boolean.FALSE;
      case UNSET:
        return null;
      default:
        throw new IllegalStateException("Unrecognized TriState value: " + this);
    }
  }

  @Functional
  public int getDbValue() {
    switch (this) {
      case YES:
        return 1;
      case NO:
        return 2;
      case UNSET:
      default:
        return 3;
    }
  }

  @Functional
  public static TriState fromDbValue(int value) {
    switch (value) {
      case 1:
        return YES;
      case 2:
        return NO;
      case 3:
      default:
        return UNSET;
    }
  }
}
