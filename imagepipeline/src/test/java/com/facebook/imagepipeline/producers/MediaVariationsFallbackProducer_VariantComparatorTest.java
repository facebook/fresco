/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import static org.fest.assertions.api.Assertions.assertThat;

import android.net.Uri;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.producers.MediaVariationsFallbackProducer.VariantComparator;
import com.facebook.imagepipeline.request.MediaVariations.Variant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class MediaVariationsFallbackProducer_VariantComparatorTest {

  private static final int TARGET_WIDTH = 100;
  private static final int TARGET_HEIGHT = 200;
  private static final ResizeOptions RESIZE_OPTIONS =
      ResizeOptions.forDimensions(TARGET_WIDTH, TARGET_HEIGHT);

  private static final Uri URI = Uri.parse("http://frescolib.org/variant.jpg");

  private VariantComparator mComparator;

  @Before
  public void setup() {
    mComparator = new VariantComparator(RESIZE_OPTIONS);
  }

  @Test
  public void testTwoItemsSmallerThanTargetSizeAreSortedLargestFirst() {
    Variant smallerVariant = createWithOffsetFromTargetSize(-10);
    Variant evenSmallerVariant = createWithOffsetFromTargetSize(-20);

    assertThat(mComparator.compare(smallerVariant, evenSmallerVariant)).isNegative();
    assertThat(mComparator.compare(evenSmallerVariant, smallerVariant)).isPositive();
  }

  @Test
  public void testTwoItemsLargerThanOrEqualToTargetSizeAreSortedSmallestFirst() {
    Variant largerVariant = createWithOffsetFromTargetSize(10);
    Variant targetMatchingVariant = createWithOffsetFromTargetSize(0);

    assertThat(mComparator.compare(targetMatchingVariant, largerVariant)).isNegative();
    assertThat(mComparator.compare(largerVariant, targetMatchingVariant)).isPositive();
  }

  @Test
  public void testItemsEqualToTargetSizeAreSortedAheadOfSmallerThanTargetItems() {
    Variant smallerVariant = createWithOffsetFromTargetSize(-10);
    Variant targetMatchingVariant = createWithOffsetFromTargetSize(0);

    assertThat(mComparator.compare(targetMatchingVariant, smallerVariant)).isNegative();
    assertThat(mComparator.compare(smallerVariant, targetMatchingVariant)).isPositive();
  }

  @Test
  public void testItemsLargerThanTargetSizeAreSortedAheadOfSmallerThanTargetItems() {
    Variant smallerVariant = createWithOffsetFromTargetSize(-10);
    Variant largerVariant = createWithOffsetFromTargetSize(10);

    assertThat(mComparator.compare(largerVariant, smallerVariant)).isNegative();
    assertThat(mComparator.compare(smallerVariant, largerVariant)).isPositive();
  }

  private static Variant createWithOffsetFromTargetSize(int offset) {
    return new Variant(URI, TARGET_WIDTH + offset, TARGET_HEIGHT + offset);
  }
}
