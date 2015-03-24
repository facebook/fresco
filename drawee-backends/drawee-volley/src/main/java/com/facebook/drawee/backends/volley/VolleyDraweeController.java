/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.volley;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.AbstractDraweeController;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Drawee controller that bridges Volley with {@link SettableDraweeHierarchy}.
 * <p>
 * The hierarchy's actual image is set to the image(s) obtained by the provided data source.
 * The data source is automatically obtained and closed based on attach / detach calls.
 */
public class VolleyDraweeController
    extends AbstractDraweeController<Bitmap, Bitmap> {

  // Components
  private final Resources mResources;

  // Constant state (non-final because controllers can be reused)
  private Supplier<DataSource<Bitmap>> mDataSourceSupplier;

  public VolleyDraweeController(
      Resources resources,
      DeferredReleaser deferredReleaser,
      Executor uiThreadExecutor,
      Supplier<DataSource<Bitmap>> dataSourceSupplier,
      String id,
      Object callerContext) {
    super(deferredReleaser, uiThreadExecutor, id, callerContext);
    mResources = resources;
    init(dataSourceSupplier);
  }

  /**
   * Initializes this controller with the new data source supplier, id and caller context.
   * This allows for reusing of the existing controller instead of instantiating a new one.
   * This method should be called when the controller is in detached state.
   * @param dataSourceSupplier data source supplier
   * @param id unique id for this controller
   * @param callerContext tag and context for this controller
   */
  public void initialize(
      Supplier<DataSource<Bitmap>> dataSourceSupplier,
      String id,
      Object callerContext) {
    super.initialize(id, callerContext);
    init(dataSourceSupplier);
  }

  private void init(Supplier<DataSource<Bitmap>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;
  }

  protected Resources getResources() {
    return mResources;
  }

  @Override
  protected DataSource<Bitmap> getDataSource() {
    return mDataSourceSupplier.get();
  }

  @Override
  protected Drawable createDrawable(Bitmap image) {
    return new BitmapDrawable(mResources, Preconditions.checkNotNull(image));
  }

  @Override
  protected Bitmap getImageInfo(Bitmap image) {
    return image;
  }

  @Override
  protected int getImageHash(@Nullable Bitmap image) {
    return (image != null) ? image.hashCode() : 0;
  }

  @Override
  protected void releaseImage(@Nullable Bitmap image) {
    // no-op
  }

  @Override
  protected void releaseDrawable(@Nullable Drawable drawable) {
    // no-op
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("super", super.toString())
        .add("dataSourceSupplier", mDataSourceSupplier)
        .toString();
  }
}
