/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source

import com.facebook.common.internal.Supplier
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.image.CloseableImage

/** ImageSource that directly supplies a DataSource */
class DataSourceImageSource(
    val dataSourceSupplier: Supplier<DataSource<CloseableReference<CloseableImage>>>
) : ImageSource
