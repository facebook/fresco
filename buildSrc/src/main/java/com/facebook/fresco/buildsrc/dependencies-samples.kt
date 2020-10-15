/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object SampleDeps {
    
    object AndroidX {
        const val appcompat = "androidx.appcompat:appcompat:1.0.2"
        const val cardview = "androidx.cardview:cardview:1.0.0"
        const val preference = "androidx.preference:preference:1.0.0"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.0.0"
    }
    
    object Google {
        const val material = "com.google.android.material:material:1.1.0-alpha03"
    }

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
            const val okhttpDownloader = "com.jakewharton.picasso:picasso2-okhttp3-downloader:1.0.2"
        }

        object AndroidQuery {
            private const val version = "0.25.9"
            const val aquery = "com.googlecode.android-query:android-query:$version"
        }
    }

    object Showcase {

        const val caverockSvg = "com.caverock:androidsvg-aar:1.4"
    }
    
    object Zoomable {
        const val legacyAndroidXSupportCoreUi = "androidx.legacy:legacy-support-core-ui:1.0.0"
    }
}
