/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.net.Uri;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.multiuri.MultiUri;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public class LithoPrefetchUtils {

  public static void startPrefetch(
      final FrescoContext frescoContext,
      final @Nullable Uri uri,
      final @Nullable MultiUri multiUri,
      final @Nullable ImageOptions imageOptions,
      final @Nullable Object callerContext,
      final AtomicReference<DataSource<Void>> prefetchData) {
    cancelPrefetch(prefetchData);
    if (uri != null && frescoContext.getExperiments().enableWorkingRangePrefetching()) {
      prefetchData.set(
          frescoContext
              .getPrefetcher()
              .prefetch(
                  frescoContext.getExperiments().workingRangePrefetchTarget(),
                  uri,
                  imageOptions,
                  callerContext));
    }
  }

  public static void cancelPrefetch(final AtomicReference<DataSource<Void>> prefetchData) {
    DataSource<Void> dataSource = prefetchData.get();
    if (dataSource != null) {
      dataSource.close();
    }
    prefetchData.set(null);
  }
}
