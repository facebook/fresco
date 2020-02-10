/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object Deps {
    const val javaxAnnotation = "javax.annotation:javax.annotation-api:1.2"
    const val jsr305 = "com.google.code.findbugs:jsr305:3.0.2"
    
    object SoLoader {
        private const val version = "0.8.1"
        const val soloaderAnnotation = "com.facebook.soloader:annotation:$version"
        const val nativeloader = "com.facebook.soloader:nativeloader:$version"
        const val soloader = "com.facebook.soloader:soloader:$version"
    }

    object Tools {
        object Stetho {
            private const val version = "1.3.1"
            const val stetho = "com.facebook.stetho:stetho:$version"
            const val okhttp3 = "com.facebook.stetho:stetho-okhttp3:$version"
        }
    }
}