/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.VisibilityAware
import com.facebook.imagepipeline.image.ImageInfo

interface ImagePerfLoggingListener : ControllerListener2<ImageInfo>, VisibilityAware
