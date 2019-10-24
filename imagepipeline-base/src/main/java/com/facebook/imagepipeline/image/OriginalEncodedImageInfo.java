/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

public class OriginalEncodedImageInfo {
    private static final int UNSET = -1;
    private int mWidth = UNSET;
    private int mHeight = UNSET;
    private int mSize = UNSET;

    public OriginalEncodedImageInfo(int width, int height, int size) {
        mWidth = width;
        mHeight = height;
        mSize = size;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getSize() {
        return mSize;
    }
}
