/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import java.io.IOException;
import org.fest.assertions.api.AbstractAssert;
import org.fest.assertions.api.Assertions;

/**
 * Assertion methods for {@link CacheEvent}s.
 *
 * <p> To create an instance of this class, invoke
 * <code>{@link CacheEventAssert#assertThat(CacheEvent)}</code>.
 */
public class CacheEventAssert extends AbstractAssert<CacheEventAssert, CacheEvent> {

  public static CacheEventAssert assertThat(CacheEvent actual) {
    return new CacheEventAssert(actual);
  }

  protected CacheEventAssert(CacheEvent actual) {
    super(actual, CacheEventAssert.class);
  }

  public CacheEventAssert hasCacheKey(CacheKey expected) {
    Assertions.assertThat(actual.getCacheKey())
        .overridingErrorMessage(
            "Cache event mismatch - cache key <%s> does not match <%s>",
            actual.getCacheKey(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasException(IOException expected) {
    Assertions.assertThat(actual.getException())
        .overridingErrorMessage(
            "Cache event mismatch - exception <%s> does not match <%s>",
            actual.getCacheKey(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasEvictionReason(CacheEventListener.EvictionReason expected) {
    Assertions.assertThat(actual.getEvictionReason())
        .overridingErrorMessage(
            "Cache event mismatch - exception <%s> does not match <%s>",
            actual.getEvictionReason(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasItemSize(long expected) {
    Assertions.assertThat(actual.getItemSize())
        .overridingErrorMessage(
            "Cache event mismatch - item size <%s> does not match <%s>",
            actual.getItemSize(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasCacheSize(long expected) {
    Assertions.assertThat(actual.getCacheSize())
        .overridingErrorMessage(
            "Cache event mismatch - cache size <%s> does not match <%s>",
            actual.getCacheSize(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasResourceId(String expected) {
    Assertions.assertThat(actual.getResourceId())
        .overridingErrorMessage(
            "Cache event mismatch - resource ID:%s does not match:%s",
            actual.getResourceId(),
            expected)
        .isEqualTo(expected);
    return this;
  }

  public CacheEventAssert hasResourceIdSet() {
    Assertions.assertThat(actual.getResourceId())
        .overridingErrorMessage("Cache event mismatch - resource ID should not be null")
        .isNotNull();
    return this;
  }
}
