/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;
import java.io.IOException;
import org.junit.Test;

public class SettableCacheEventTest {

  @Test
  public void testRecycleClearsAllFields() {
    SettableCacheEvent event = SettableCacheEvent.obtain();
    event.setCacheKey(mock(CacheKey.class));
    event.setCacheLimit(21L);
    event.setCacheSize(12332445L);
    event.setEvictionReason(CacheEventListener.EvictionReason.CACHE_MANAGER_TRIMMED);
    event.setException(mock(IOException.class));
    event.setItemSize(1435L);
    event.setResourceId("sddqrtyjf");

    event.recycle();

    assertThat(event.getCacheKey()).isNull();
    assertThat(event.getCacheLimit()).isZero();
    assertThat(event.getCacheSize()).isZero();
    assertThat(event.getEvictionReason()).isNull();
    assertThat(event.getException()).isNull();
    assertThat(event.getItemSize()).isZero();
    assertThat(event.getResourceId()).isNull();
  }

  @Test
  public void testSecondObtainGetsNewEventIfNoRecycling() {
    SettableCacheEvent firstEvent = SettableCacheEvent.obtain();
    SettableCacheEvent secondEvent = SettableCacheEvent.obtain();

    assertThat(secondEvent).isNotSameAs(firstEvent);
  }

  @Test
  public void testSecondObtainAfterRecyclingGetsRecycledEvent() {
    SettableCacheEvent firstEvent = SettableCacheEvent.obtain();
    firstEvent.recycle();
    SettableCacheEvent secondEvent = SettableCacheEvent.obtain();

    assertThat(secondEvent).isSameAs(firstEvent);
  }
}
