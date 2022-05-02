/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel

fun ImageDataModel?.maybeGetDrawable(): Drawable? = (this as? DrawableImageDataModel)?.drawable
