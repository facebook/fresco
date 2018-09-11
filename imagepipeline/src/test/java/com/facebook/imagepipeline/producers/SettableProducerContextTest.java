/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Checks basic properties of NullProducer, that is that it always returns null.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SettableProducerContextTest {
  @Mock public ImageRequest mImageRequest;
  private final String mRequestId = "mRequestId";
  private ProducerContextCallbacks mCallbacks1;
  private ProducerContextCallbacks mCallbacks2;
  private SettableProducerContext mSettableProducerContext;

  @Before
  public void setUp() {
    mSettableProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mock(ProducerListener.class),
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mCallbacks1 = mock(ProducerContextCallbacks.class);
    mCallbacks2 = mock(ProducerContextCallbacks.class);
  }

  @Test
  public void testGetters() {
    assertEquals(mImageRequest, mSettableProducerContext.getImageRequest());
    assertEquals(mRequestId, mSettableProducerContext.getId());
  }

  @Test
  public void testIsPrefetch() {
    assertFalse(mSettableProducerContext.isPrefetch());
  }

  @Test
  public void testCancellation() {
    mSettableProducerContext.addCallbacks(mCallbacks1);
    verify(mCallbacks1, never()).onCancellationRequested();
    mSettableProducerContext.cancel();
    verify(mCallbacks1).onCancellationRequested();
    verify(mCallbacks1, never()).onIsPrefetchChanged();

    mSettableProducerContext.addCallbacks(mCallbacks2);
    verify(mCallbacks2).onCancellationRequested();
    verify(mCallbacks2, never()).onIsPrefetchChanged();
  }

  @Test
  public void testSetPrefetch() {
    mSettableProducerContext.addCallbacks(mCallbacks1);
    assertFalse(mSettableProducerContext.isPrefetch());
    mSettableProducerContext.setIsPrefetch(true);
    assertTrue(mSettableProducerContext.isPrefetch());
    verify(mCallbacks1).onIsPrefetchChanged();
    mSettableProducerContext.setIsPrefetch(true);
    // only one callback is expected
    verify(mCallbacks1).onIsPrefetchChanged();
  }

  @Test
  public void testSetIsIntermediateResultExpected() {
    mSettableProducerContext.addCallbacks(mCallbacks1);
    assertTrue(mSettableProducerContext.isIntermediateResultExpected());
    mSettableProducerContext.setIsIntermediateResultExpected(false);
    assertFalse(mSettableProducerContext.isIntermediateResultExpected());
    verify(mCallbacks1).onIsIntermediateResultExpectedChanged();
    mSettableProducerContext.setIsIntermediateResultExpected(false);
    // only one callback is expected
    verify(mCallbacks1).onIsIntermediateResultExpectedChanged();
  }

  @Test
  public void testNoCallbackCalledWhenIsPrefetchDoesNotChange() {
    assertFalse(mSettableProducerContext.isPrefetch());
    mSettableProducerContext.addCallbacks(mCallbacks1);
    mSettableProducerContext.setIsPrefetch(false);
    verify(mCallbacks1, never()).onIsPrefetchChanged();
  }

  @Test
  public void testCallbackCalledWhenIsPrefetchChanges() {
    assertFalse(mSettableProducerContext.isPrefetch());
    mSettableProducerContext.addCallbacks(mCallbacks1);
    mSettableProducerContext.addCallbacks(mCallbacks2);
    mSettableProducerContext.setIsPrefetch(true);
    assertTrue(mSettableProducerContext.isPrefetch());
    verify(mCallbacks1).onIsPrefetchChanged();
    verify(mCallbacks1, never()).onCancellationRequested();
    verify(mCallbacks2).onIsPrefetchChanged();
    verify(mCallbacks2, never()).onCancellationRequested();
  }
}
