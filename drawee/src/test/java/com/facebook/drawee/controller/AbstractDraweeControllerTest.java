/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.controller;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Throwables;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.SimpleDataSource;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

/** * Tests for AbstractDraweeController */
@RunWith(RobolectricTestRunner.class)
public class AbstractDraweeControllerTest {

  public static class FakeImageInfo {}

  public static class FakeImage {
    private final Drawable mDrawable;
    private final FakeImageInfo mImageInfo;
    private boolean mIsOpened;
    private boolean mIsClosed;

    protected FakeImage(Drawable drawable, FakeImageInfo imageInfo) {
      mDrawable = drawable;
      mImageInfo = imageInfo;
      mIsOpened = false;
      mIsClosed = false;
    }

    public Drawable getDrawable() {
      return mDrawable;
    }

    public @Nullable FakeImageInfo getImageInfo() {
      return mImageInfo;
    }

    public void open() {
      mIsOpened = true;
    }

    public boolean isOpened() {
      return mIsOpened;
    }

    public void close() {
      mIsClosed = true;
    }

    public boolean isClosed() {
      return mIsClosed;
    }

    public static FakeImage create(Drawable drawable) {
      return new FakeImage(drawable, null);
    }

    public static FakeImage create(Drawable drawable, FakeImageInfo imageInfo) {
      return new FakeImage(drawable, imageInfo);
    }
  }

  public static class FakeDraweeController
      extends AbstractDraweeController<FakeImage, FakeImageInfo> {

    private Supplier<DataSource<FakeImage>> mDataSourceSupplier;
    public boolean mIsAttached = false;

    public FakeDraweeController(
        DeferredReleaser deferredReleaser,
        Executor uiThreadExecutor,
        Supplier<DataSource<FakeImage>> dataSourceSupplier,
        String id,
        Object callerContext) {
      super(deferredReleaser, uiThreadExecutor, id, callerContext);
      mDataSourceSupplier = dataSourceSupplier;
    }

    @Override
    public void onAttach() {
      mIsAttached = true;
      super.onAttach();
    }

    @Override
    public void onDetach() {
      mIsAttached = false;
      super.onDetach();
    }

    public boolean isAttached() {
      return mIsAttached;
    }

    @Override
    protected DataSource<FakeImage> getDataSource() {
      return mDataSourceSupplier.get();
    }

    @Override
    protected Drawable createDrawable(FakeImage image) {
      return image.getDrawable();
    }

    @Override
    protected @Nullable FakeImageInfo getImageInfo(FakeImage image) {
      return image.getImageInfo();
    }

    @Override
    protected void releaseImage(@Nullable FakeImage image) {
      if (image != null) {
        image.close();
      }
    }

    @Override
    protected void releaseDrawable(@Nullable Drawable drawable) {}

    @Nullable
    @Override
    public Map<String, Object> obtainExtrasFromImage(FakeImageInfo fakeImageInfo) {
      return Collections.emptyMap();
    }

    @Override
    public boolean isSameImageRequest(DraweeController other) {
      return false;
    }
  }

  private DeferredReleaser mDeferredReleaser;
  private Object mCallerContext;
  private Supplier<DataSource<FakeImage>> mDataSourceSupplier;
  private SettableDraweeHierarchy mDraweeHierarchy;

  private Executor mUiThreadExecutor;
  private FakeDraweeController mController;

  @Before
  public void setUp() {
    mDeferredReleaser = mock(DeferredReleaser.class);
    mCallerContext = mock(Object.class);
    mDataSourceSupplier = mock(Supplier.class);
    mDraweeHierarchy = mock(SettableDraweeHierarchy.class);
    mUiThreadExecutor = CallerThreadExecutor.getInstance();
    mController =
        new FakeDraweeController(
            mDeferredReleaser, mUiThreadExecutor, mDataSourceSupplier, "id", mCallerContext);
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                ((DeferredReleaser.Releasable) invocation.getArguments()[0]).release();
                return null;
              }
            })
        .when(mDeferredReleaser)
        .scheduleDeferredRelease(any(DeferredReleaser.Releasable.class));
    when(mDataSourceSupplier.get()).thenReturn(SimpleDataSource.<FakeImage>create());
  }

  @Test
  public void testOnAttach() {
    mController.setHierarchy(mDraweeHierarchy);
    mController.onAttach();
    verify(mDeferredReleaser, atLeastOnce()).cancelDeferredRelease(eq(mController));
    verify(mDataSourceSupplier).get();
  }

  @Test
  public void testOnAttach_ThrowsWithoutHierarchy() {
    try {
      mController.setHierarchy(null);
      mController.onAttach();
      fail("onAttach should fail if no drawee hierarchy is set!");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void testOnDetach() {
    mController.setHierarchy(mDraweeHierarchy);
    mController.onAttach();
    mController.onDetach();
    assertSame(mDraweeHierarchy, mController.getHierarchy());
    verify(mDeferredReleaser).scheduleDeferredRelease(mController);
  }

  @Test
  public void testSettingControllerOverlay() {
    Drawable controllerOverlay1 = mock(Drawable.class);
    Drawable controllerOverlay2 = mock(Drawable.class);
    SettableDraweeHierarchy draweeHierarchy1 = mock(SettableDraweeHierarchy.class);
    SettableDraweeHierarchy draweeHierarchy2 = mock(SettableDraweeHierarchy.class);
    InOrder inOrder = inOrder(draweeHierarchy1, draweeHierarchy2);

    // initial state
    assertNull(mController.getHierarchy());

    // set controller overlay before hierarchy
    mController.setControllerOverlay(controllerOverlay1);

    // set drawee hierarchy
    mController.setHierarchy(draweeHierarchy1);
    assertSame(draweeHierarchy1, mController.getHierarchy());
    inOrder.verify(draweeHierarchy1, times(1)).setControllerOverlay(controllerOverlay1);
    inOrder.verify(draweeHierarchy1, times(0)).reset();

    // change drawee hierarchy
    mController.setHierarchy(draweeHierarchy2);
    assertSame(draweeHierarchy2, mController.getHierarchy());
    inOrder.verify(draweeHierarchy1, times(1)).setControllerOverlay(null);
    inOrder.verify(draweeHierarchy1, times(0)).reset();
    inOrder.verify(draweeHierarchy2, times(1)).setControllerOverlay(controllerOverlay1);
    inOrder.verify(draweeHierarchy2, times(0)).reset();

    // clear drawee hierarchy
    mController.setHierarchy(null);
    assertSame(null, mController.getHierarchy());
    inOrder.verify(draweeHierarchy1, times(0)).setControllerOverlay(any(Drawable.class));
    inOrder.verify(draweeHierarchy1, times(0)).reset();
    inOrder.verify(draweeHierarchy2, times(1)).setControllerOverlay(null);
    inOrder.verify(draweeHierarchy2, times(0)).reset();

    // set drawee hierarchy
    mController.setHierarchy(draweeHierarchy1);
    assertSame(draweeHierarchy1, mController.getHierarchy());
    inOrder.verify(draweeHierarchy1, times(1)).setControllerOverlay(controllerOverlay1);
    inOrder.verify(draweeHierarchy1, times(0)).reset();
    inOrder.verify(draweeHierarchy2, times(0)).setControllerOverlay(any(Drawable.class));
    inOrder.verify(draweeHierarchy2, times(0)).reset();

    // change controller overlay
    mController.setControllerOverlay(controllerOverlay2);
    inOrder.verify(draweeHierarchy1, times(1)).setControllerOverlay(controllerOverlay2);
    inOrder.verify(draweeHierarchy1, times(0)).reset();
    inOrder.verify(draweeHierarchy2, times(0)).setControllerOverlay(any(Drawable.class));
    inOrder.verify(draweeHierarchy2, times(0)).reset();

    // clear controller overlay
    mController.setControllerOverlay(null);
    inOrder.verify(draweeHierarchy1, times(1)).setControllerOverlay(null);
    inOrder.verify(draweeHierarchy1, times(0)).reset();
    inOrder.verify(draweeHierarchy2, times(0)).setControllerOverlay(any(Drawable.class));
    inOrder.verify(draweeHierarchy2, times(0)).reset();
  }

  @Test
  public void testListeners() {
    ControllerListener<FakeImageInfo> listener1 = mock(ControllerListener.class);
    ControllerListener<Object> listener2 = mock(ControllerListener.class);
    InOrder inOrder = inOrder(listener1, listener2);

    mController.getControllerListener().onRelease("id");
    inOrder.verify(listener1, never()).onRelease(anyString());
    inOrder.verify(listener2, never()).onRelease(anyString());

    mController.addControllerListener(listener1);
    mController.getControllerListener().onRelease("id");
    inOrder.verify(listener1, times(1)).onRelease("id");
    inOrder.verify(listener2, never()).onRelease(anyString());

    mController.addControllerListener(listener2);
    mController.getControllerListener().onRelease("id");
    inOrder.verify(listener1, times(1)).onRelease("id");
    inOrder.verify(listener2, times(1)).onRelease("id");

    mController.removeControllerListener(listener1);
    mController.getControllerListener().onRelease("id");
    inOrder.verify(listener1, never()).onRelease(anyString());
    inOrder.verify(listener2, times(1)).onRelease("id");

    mController.removeControllerListener(listener2);
    mController.getControllerListener().onRelease("id");
    inOrder.verify(listener1, never()).onRelease(anyString());
    inOrder.verify(listener2, never()).onRelease(anyString());
  }

  @Test
  public void testListenerReentrancy_AfterIntermediateSet() {
    testListenerReentrancy(INTERMEDIATE_FAILURE);
  }

  @Test
  public void testListenerReentrancy_AfterIntermediateFailed() {
    testListenerReentrancy(INTERMEDIATE_GOOD);
  }

  @Test
  public void testListenerReentrancy_AfterFinalSet() {
    testListenerReentrancy(SUCCESS);
  }

  @Test
  public void testListenerReentrancy_AfterFailure() {
    testListenerReentrancy(FAILURE);
  }

  private void testListenerReentrancy(int outcome) {
    final SimpleDataSource<FakeImage> dataSource0 = SimpleDataSource.create();
    final SimpleDataSource<FakeImage> dataSource = SimpleDataSource.create();
    when(mDataSourceSupplier.get()).thenReturn(dataSource0);
    FakeImage image0 = FakeImage.create(mock(Drawable.class), mock(FakeImageInfo.class));
    finish(dataSource0, image0, outcome);

    ControllerListener listener =
        new BaseControllerListener<FakeImageInfo>() {
          @Override
          public void onIntermediateImageSet(String id, @Nullable FakeImageInfo imageInfo) {
            initializeAndAttachController("id_AfterIntermediateSet", dataSource);
          }

          @Override
          public void onIntermediateImageFailed(String id, Throwable throwable) {
            initializeAndAttachController("id_AfterIntermediateFailed", dataSource);
          }

          @Override
          public void onFinalImageSet(
              String id, @Nullable FakeImageInfo imageInfo, @Nullable Animatable animatable) {
            initializeAndAttachController("id_AfterFinalSet", dataSource);
          }

          @Override
          public void onFailure(String id, Throwable throwable) {
            initializeAndAttachController("id_AfterFailure", dataSource);
          }
        };

    mController.addControllerListener(listener);
    mController.setHierarchy(mDraweeHierarchy);
    mController.onAttach();

    switch (outcome) {
      case INTERMEDIATE_GOOD:
        verifyDhInteraction(SET_IMAGE_P50, image0.getDrawable(), true);
        Assert.assertEquals("id_AfterIntermediateSet", mController.getId());
        break;
      case INTERMEDIATE_FAILURE:
        verifyDhInteraction(IGNORE, image0.getDrawable(), true);
        Assert.assertEquals("id_AfterIntermediateFailed", mController.getId());
        break;
      case SUCCESS:
        verifyDhInteraction(SET_IMAGE_P100, image0.getDrawable(), true);
        Assert.assertEquals("id_AfterFinalSet", mController.getId());
        break;
      case FAILURE:
        verifyDhInteraction(SET_FAILURE, image0.getDrawable(), true);
        Assert.assertEquals("id_AfterFailure", mController.getId());
        break;
    }
    verify(mDraweeHierarchy).reset();

    FakeImage image = FakeImage.create(mock(Drawable.class), mock(FakeImageInfo.class));
    finish(dataSource, image, SUCCESS);
    verifyDhInteraction(SET_IMAGE_P100, image.getDrawable(), false);
  }

  private void initializeAndAttachController(String id, DataSource<FakeImage> dataSource) {
    try {
      when(mDataSourceSupplier.get()).thenReturn(dataSource);
      mController.initialize(id, mCallerContext);
      mController.setHierarchy(mDraweeHierarchy);
      mController.onAttach();
    } catch (Throwable throwable) {
      System.err.println(
          "Exception thrown in listener: " + Throwables.getStackTraceAsString(throwable));
    }
  }

  @Test
  public void testLoading1_DelayedSuccess() {
    testLoading(false, SUCCESS, SET_IMAGE_P100);
  }

  @Test
  public void testLoading1_DelayedFailure() {
    testLoading(false, FAILURE, SET_FAILURE);
  }

  @Test
  public void testLoading1_ImmediateSuccess() {
    testLoading(true, SUCCESS, SET_IMAGE_P100);
  }

  @Test
  public void testLoading1_ImmediateFailure() {
    testLoading(true, FAILURE, SET_FAILURE);
  }

  @Test
  public void testLoadingS_S() {
    testStreamedLoading(new int[] {SUCCESS}, new int[] {SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_F() {
    testStreamedLoading(new int[] {FAILURE}, new int[] {SET_FAILURE});
  }

  @Test
  public void testLoadingS_LS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_LOW, SUCCESS}, new int[] {SET_IMAGE_P20, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_GS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_GOOD, SUCCESS}, new int[] {SET_IMAGE_P50, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_FS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_FAILURE, SUCCESS}, new int[] {IGNORE, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_LF() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_LOW, FAILURE}, new int[] {SET_IMAGE_P20, SET_FAILURE});
  }

  @Test
  public void testLoadingS_GF() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_GOOD, FAILURE}, new int[] {SET_IMAGE_P50, SET_FAILURE});
  }

  @Test
  public void testLoadingS_FF() {
    testStreamedLoading(new int[] {INTERMEDIATE_FAILURE, FAILURE}, new int[] {IGNORE, SET_FAILURE});
  }

  @Test
  public void testLoadingS_LLS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_LOW, INTERMEDIATE_LOW, SUCCESS},
        new int[] {SET_IMAGE_P20, SET_IMAGE_P20, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_FLS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_FAILURE, INTERMEDIATE_LOW, SUCCESS},
        new int[] {IGNORE, SET_IMAGE_P20, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_LGS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_LOW, INTERMEDIATE_GOOD, SUCCESS},
        new int[] {SET_IMAGE_P20, SET_IMAGE_P50, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_GGS() {
    testStreamedLoading(
        0,
        new int[] {INTERMEDIATE_GOOD, INTERMEDIATE_GOOD, SUCCESS},
        new int[] {SET_IMAGE_P50, SET_IMAGE_P50, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_FGS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_FAILURE, INTERMEDIATE_GOOD, SUCCESS},
        new int[] {IGNORE, SET_IMAGE_P50, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_LFS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_LOW, INTERMEDIATE_FAILURE, SUCCESS},
        new int[] {SET_IMAGE_P20, IGNORE, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_GFS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_GOOD, INTERMEDIATE_FAILURE, SUCCESS},
        new int[] {SET_IMAGE_P50, IGNORE, SET_IMAGE_P100});
  }

  @Test
  public void testLoadingS_FFS() {
    testStreamedLoading(
        new int[] {INTERMEDIATE_FAILURE, INTERMEDIATE_FAILURE, SUCCESS},
        new int[] {IGNORE, IGNORE, SET_IMAGE_P100});
  }

  /**
   * Tests a single loading scenario.
   *
   * @param isImmediate whether the result is immediate or not
   * @param outcome outcomes of the submitted request
   * @param dhInteraction expected interaction with drawee hierarchy after the request finishes
   */
  private void testLoading(boolean isImmediate, int outcome, int dhInteraction) {
    FakeDraweeController controller =
        new FakeDraweeController(
            mDeferredReleaser, mUiThreadExecutor, mDataSourceSupplier, "id2", mCallerContext);

    // create image and the corresponding data source
    FakeImage image = FakeImage.create(mock(Drawable.class), mock(FakeImageInfo.class));
    SimpleDataSource<FakeImage> dataSource = SimpleDataSource.create();
    when(mDataSourceSupplier.get()).thenReturn(dataSource);

    // finish immediate
    if (isImmediate) {
      finish(dataSource, image, outcome);
    }

    // attach
    controller.setHierarchy(mDraweeHierarchy);
    controller.onAttach();

    // finish delayed
    if (!isImmediate) {
      finish(dataSource, image, outcome);
    }

    // verify
    verify(mDataSourceSupplier).get();
    verifyDhInteraction(dhInteraction, image.getDrawable(), isImmediate);
    assertTrue(dataSource.isClosed());

    // detach
    controller.onDetach();

    // verify that all open images has been closed
    assertTrue(image.isOpened() == image.isClosed());

    verifyNoMoreInteractions(mDataSourceSupplier);
  }

  /**
   * Tests a suite of loading scenarios with streaming.
   *
   * @param outcomes outcomes of submitted requests
   * @param dhInteraction expected interaction with drawee hierarchy after each request finishes
   */
  private void testStreamedLoading(int[] outcomes, int[] dhInteraction) {
    for (int numImmediate = 0; numImmediate <= 1; numImmediate++) {
      reset(mDataSourceSupplier, mDraweeHierarchy);
      System.out.println("numImmediate: " + numImmediate);
      testStreamedLoading(numImmediate, outcomes, dhInteraction);
    }
  }

  /**
   * Tests a single loading scenario with streaming.
   *
   * @param numImmediate number of immediate results
   * @param outcomes outcomes of submitted requests
   * @param dhInteraction expected interaction with drawee hierarchy after each request finishes
   */
  private void testStreamedLoading(int numImmediate, int[] outcomes, int[] dhInteraction) {
    FakeDraweeController controller =
        new FakeDraweeController(
            mDeferredReleaser,
            mUiThreadExecutor,
            mDataSourceSupplier,
            "id_streamed",
            mCallerContext);

    int n = outcomes.length;

    // create data source and images
    SimpleDataSource<FakeImage> dataSource = SimpleDataSource.create();
    when(mDataSourceSupplier.get()).thenReturn(dataSource);
    List<FakeImage> images = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      images.add(FakeImage.create(mock(Drawable.class), mock(FakeImageInfo.class)));
    }

    // finish immediate
    for (int i = 0; i < numImmediate; i++) {
      finish(dataSource, images.get(i), outcomes[i]);
    }

    // attach
    controller.setHierarchy(mDraweeHierarchy);
    controller.onAttach();
    verify(mDraweeHierarchy).setProgress(0, true);

    // finish delayed
    for (int i = numImmediate; i < n; i++) {
      finish(dataSource, images.get(i), outcomes[i]);
    }

    // verify
    verify(mDataSourceSupplier).get();
    for (int i = 0; i < n; i++) {
      verifyDhInteraction(dhInteraction[i], images.get(i).getDrawable(), 0 < numImmediate);
    }
    assertTrue(dataSource.isClosed());

    // detach
    controller.onDetach();

    // verify that all open images has been closed
    for (int i = 0; i < n; i++) {
      assertTrue(images.get(i).isOpened() == images.get(i).isClosed());
    }

    verifyNoMoreInteractions(mDataSourceSupplier);
  }

  private static void finish(SimpleDataSource<FakeImage> dataSource, FakeImage image, int outcome) {
    switch (outcome) {
      case FAILURE:
        dataSource.setFailure(new RuntimeException());
        break;
      case SUCCESS:
        image.open();
        dataSource.setResult(image);
        break;
      case INTERMEDIATE_FAILURE:
        dataSource.setResult(createFaultyImage(), false);
        break;
      case INTERMEDIATE_LOW:
        image.open();
        dataSource.setProgress(0.2f);
        dataSource.setResult(image, false);
        break;
      case INTERMEDIATE_GOOD:
        image.open();
        dataSource.setProgress(0.5f);
        dataSource.setResult(image, false);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported outcome: " + outcome);
    }
  }

  private void verifyDhInteraction(int dhInteraction, Drawable drawable, boolean wasImmediate) {
    switch (dhInteraction) {
      case IGNORE:
        verify(mDraweeHierarchy, never()).setImage(eq(drawable), anyFloat(), anyBoolean());
        break;
      case SET_IMAGE_P20:
        verify(mDraweeHierarchy).setImage(eq(drawable), eq(0.2f), eq(wasImmediate));
        break;
      case SET_IMAGE_P50:
        verify(mDraweeHierarchy).setImage(eq(drawable), eq(0.5f), eq(wasImmediate));
        break;
      case SET_IMAGE_P100:
        verify(mDraweeHierarchy).setImage(eq(drawable), eq(1.0f), eq(wasImmediate));
        break;
      case SET_FAILURE:
        verify(mDraweeHierarchy).setFailure(any(Throwable.class));
        break;
      case SET_RETRY:
        verify(mDraweeHierarchy).setRetry(any(Throwable.class));
        break;
      default:
        fail();
        break;
    }
  }

  private static final int FAILURE = 0;
  private static final int SUCCESS = 1;
  private static final int INTERMEDIATE_FAILURE = 2;
  private static final int INTERMEDIATE_LOW = 3;
  private static final int INTERMEDIATE_GOOD = 4;

  private static final int IGNORE = 1000;
  private static final int SET_FAILURE = 1001;
  private static final int SET_RETRY = 1002;
  private static final int SET_IMAGE_P20 = 1003;
  private static final int SET_IMAGE_P50 = 1004;
  private static final int SET_IMAGE_P100 = 1005;

  private static FakeImage createFaultyImage() {
    return new FakeImage(null, null) {
      @Override
      public Drawable getDrawable() {
        throw new RuntimeException("Faulty intermediate image");
      }
    };
  }
}
