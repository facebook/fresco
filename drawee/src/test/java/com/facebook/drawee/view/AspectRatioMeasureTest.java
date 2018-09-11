/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.view;

import static android.view.View.MeasureSpec.*;
import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.LayoutParams.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AspectRatioMeasureTest {

  AspectRatioMeasure.Spec mSpec = new AspectRatioMeasure.Spec();

  @Before
  public void setUp() {
  }

  @Test
  public void testAspectRatio() {
    // width
    mSpec.width = makeMeasureSpec(410, UNSPECIFIED);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams1 = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 1.5f, layoutParams1, 10, 10);
    assertEquals(makeMeasureSpec(460, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);

    // height
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(310, UNSPECIFIED);
    LayoutParams layoutParams2 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 2f, layoutParams2, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(210, EXACTLY), mSpec.height);
  }

  @Test
  public void testNoAspectRatio() {
    // width
    mSpec.width = makeMeasureSpec(410, UNSPECIFIED);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams1 = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 0, layoutParams1, 10, 10);
    assertEquals(makeMeasureSpec(410, UNSPECIFIED), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);

    // height
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(310, UNSPECIFIED);
    LayoutParams layoutParams2 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 0, layoutParams2, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, UNSPECIFIED), mSpec.height);
  }

  @Test
  public void testAtMost() {
    // width exceeded
    mSpec.width = makeMeasureSpec(410, AT_MOST);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams1 = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 1.5f, layoutParams1, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);

    // width within limits
    mSpec.width = makeMeasureSpec(510, AT_MOST);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams2 = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 1.5f, layoutParams2, 10, 10);
    assertEquals(makeMeasureSpec(460, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);

    // height exceeded
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(110, AT_MOST);
    LayoutParams layoutParams3 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 2f, layoutParams3, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(110, EXACTLY), mSpec.height);

    // height within limits
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(310, AT_MOST);
    LayoutParams layoutParams4 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 2f, layoutParams4, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(210, EXACTLY), mSpec.height);
  }

  @Test
  public void testExactly() {
    // width
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams1 = new LayoutParams(WRAP_CONTENT, MATCH_PARENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 1.5f, layoutParams1, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);

    // height
    mSpec.width = makeMeasureSpec(410, EXACTLY);
    mSpec.height = makeMeasureSpec(310, EXACTLY);
    LayoutParams layoutParams2 = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    AspectRatioMeasure.updateMeasureSpec(mSpec, 2f, layoutParams2, 10, 10);
    assertEquals(makeMeasureSpec(410, EXACTLY), mSpec.width);
    assertEquals(makeMeasureSpec(310, EXACTLY), mSpec.height);
  }
}
