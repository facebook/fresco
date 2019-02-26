/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.common;

import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_DATA;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_UNKNOWN;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/**
 * This is the interface we use in order to define different types of Uri an ImageRequest
 * can have.
 */
@Retention(SOURCE)
@IntDef({
    SOURCE_TYPE_UNKNOWN,
    SOURCE_TYPE_NETWORK,
    SOURCE_TYPE_LOCAL_FILE,
    SOURCE_TYPE_LOCAL_VIDEO_FILE,
    SOURCE_TYPE_LOCAL_IMAGE_FILE,
    SOURCE_TYPE_LOCAL_CONTENT,
    SOURCE_TYPE_LOCAL_ASSET,
    SOURCE_TYPE_LOCAL_RESOURCE,
    SOURCE_TYPE_DATA,
    SOURCE_TYPE_QUALIFIED_RESOURCE
})
public @interface SourceUriType {

  int SOURCE_TYPE_UNKNOWN = -1;
  int SOURCE_TYPE_NETWORK = 0;
  int SOURCE_TYPE_LOCAL_FILE = 1;
  int SOURCE_TYPE_LOCAL_VIDEO_FILE = 2;
  int SOURCE_TYPE_LOCAL_IMAGE_FILE = 3;
  int SOURCE_TYPE_LOCAL_CONTENT = 4;
  int SOURCE_TYPE_LOCAL_ASSET = 5;
  int SOURCE_TYPE_LOCAL_RESOURCE = 6;
  int SOURCE_TYPE_DATA = 7;
  int SOURCE_TYPE_QUALIFIED_RESOURCE = 8;
}
