/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Mockito.*;

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
public class NullProducerTest {
  @Mock public Consumer mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ProducerListener mProducerListener;
  private NullProducer mNullProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mNullProducer = new NullProducer();
  }

  @Test
  public void testNullProducerReturnsNull() {
    mNullProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
  }
}
