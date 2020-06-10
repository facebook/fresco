/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object SampleDeps {

    object Comparison {
        object Glide {
            private const val version = "4.9.0"
            const val glide = "com.github.bumptech.glide:glide:$version"
            const val compiler = "com.github.bumptech.glide:compiler:$version"
        }

        object Uil {
            private const val version = "1.9.5"
            const val uil = "com.nostra13.universalimageloader:universal-image-loader:$version"
        }

        object Picasso {
            private const val version = "2.71828"
            const val picasso = "com.squareup.picasso:picasso:$version"
        }

        object AndroidQuery {
            private const val version = "0.25.9"
            const val aquery = "com.googlecode.android-query:android-query:$version"
        }
    }

    object Showcase {

        const val caverockSvg = "com.caverock:androidsvg-aar:1.4"
    }
}
