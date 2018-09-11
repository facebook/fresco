/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.common.internal.Sets;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

/**
 * Tests for {@link ForwardingRequestListener}
 */
@RunWith(RobolectricTestRunner.class)
public class ForwardingRequestListenerTest {
  @Mock public ImageRequest mRequest;
  @Mock public Object mCallerContext;
  @Mock public Exception mException;
  @Mock public Map<String, String> mImmutableMap;
  private RequestListener mRequestListener1;
  private RequestListener mRequestListener2;
  private RequestListener mRequestListener3;
  private ForwardingRequestListener mListenerManager;
  private final String mRequestId = "DummyRequestId";
  private final String mProducerName = "DummyProducerName";
  private final String mProducerEventName = "DummyProducerEventName";
  private final boolean mIsPrefetch = true;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mRequestListener1 = mock(RequestListener.class);
    mRequestListener2 = mock(RequestListener.class);
    mRequestListener3 = mock(RequestListener.class);
    when(mRequestListener1.requiresExtraMap(mRequestId)).thenReturn(false);
    when(mRequestListener2.requiresExtraMap(mRequestId)).thenReturn(false);
    when(mRequestListener3.requiresExtraMap(mRequestId)).thenReturn(false);
    mListenerManager = new ForwardingRequestListener(
        Sets.newHashSet(mRequestListener1, mRequestListener2, mRequestListener3));
  }

  @Test
  public void testOnRequestStart() {
    mListenerManager.onRequestStart(mRequest, mCallerContext, mRequestId, mIsPrefetch);
    verify(mRequestListener1).onRequestStart(mRequest, mCallerContext, mRequestId, mIsPrefetch);
    verify(mRequestListener2).onRequestStart(mRequest, mCallerContext, mRequestId, mIsPrefetch);
    verify(mRequestListener3).onRequestStart(mRequest, mCallerContext, mRequestId, mIsPrefetch);
  }

  @Test
  public void testOnRequestSuccess() {
    mListenerManager.onRequestSuccess(mRequest, mRequestId, mIsPrefetch);
    verify(mRequestListener1).onRequestSuccess(mRequest, mRequestId, mIsPrefetch);
    verify(mRequestListener2).onRequestSuccess(mRequest, mRequestId, mIsPrefetch);
    verify(mRequestListener3).onRequestSuccess(mRequest, mRequestId, mIsPrefetch);
  }

  @Test
  public void testOnRequestFailure() {
    mListenerManager.onRequestFailure(mRequest, mRequestId, mException, mIsPrefetch);
    verify(mRequestListener1).onRequestFailure(mRequest, mRequestId, mException, mIsPrefetch);
    verify(mRequestListener2).onRequestFailure(mRequest, mRequestId, mException, mIsPrefetch);
    verify(mRequestListener3).onRequestFailure(mRequest, mRequestId, mException, mIsPrefetch);
  }

  @Test
  public void testOnProducerStart() {
    mListenerManager.onProducerStart(mRequestId, mProducerName);
    verify(mRequestListener1).onProducerStart(mRequestId, mProducerName);
    verify(mRequestListener2).onProducerStart(mRequestId, mProducerName);
    verify(mRequestListener3).onProducerStart(mRequestId, mProducerName);
  }

  @Test
  public void testOnProducerFinishWithSuccess() {
    mListenerManager.onProducerFinishWithSuccess(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener1)
        .onProducerFinishWithSuccess(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener2)
        .onProducerFinishWithSuccess(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener3)
        .onProducerFinishWithSuccess(mRequestId, mProducerName, mImmutableMap);
  }

  @Test
  public void testOnProducerFinishWithFailure() {
    mListenerManager.onProducerFinishWithFailure(
        mRequestId, mProducerName, mException, mImmutableMap);
    verify(mRequestListener1)
        .onProducerFinishWithFailure(mRequestId, mProducerName, mException, mImmutableMap);
    verify(mRequestListener2)
        .onProducerFinishWithFailure(mRequestId, mProducerName, mException, mImmutableMap);
    verify(mRequestListener3)
        .onProducerFinishWithFailure(mRequestId, mProducerName, mException, mImmutableMap);
  }

  @Test
  public void testOnProducerFinishWithCancellation() {
    mListenerManager.onProducerFinishWithCancellation(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener1)
        .onProducerFinishWithCancellation(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener2)
        .onProducerFinishWithCancellation(mRequestId, mProducerName, mImmutableMap);
    verify(mRequestListener3)
        .onProducerFinishWithCancellation(mRequestId, mProducerName, mImmutableMap);
  }

  @Test
  public void testOnProducerEvent() {
    mListenerManager.onProducerEvent(mRequestId, mProducerName, mProducerEventName);
    verify(mRequestListener1).onProducerEvent(
        mRequestId,
        mProducerName,
        mProducerEventName);
    verify(mRequestListener2).onProducerEvent(
        mRequestId,
        mProducerName,
        mProducerEventName);
    verify(mRequestListener3).onProducerEvent(
        mRequestId,
        mProducerName,
        mProducerEventName);
  }

  @Test
  public void testRequiresExtraMap() {
    assertFalse(mListenerManager.requiresExtraMap(mRequestId));
    when(mRequestListener2.requiresExtraMap(mRequestId)).thenReturn(true);
    assertTrue(mListenerManager.requiresExtraMap(mRequestId));
  }
}
