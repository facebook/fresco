/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc

import android.util.Log
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.producers.ProducerContext

class LogcatRequestListener2(val logExtraMap: Boolean = true, val tag : String = "LogcatRequestListener2") : RequestListener2 {

    override fun onRequestStart(producerContext: ProducerContext) {
        Log.d(tag, "onRequestStart ${toString(producerContext)}")
    }

    override fun onRequestSuccess(producerContext: ProducerContext) {
        Log.d(tag, "onRequestSuccess ${toString(producerContext)}")
    }

    override fun onRequestFailure(producerContext: ProducerContext, throwable: Throwable?) {
        Log.d(tag, "onRequestFailure ${toString(producerContext)}")
    }

    override fun onRequestCancellation(producerContext: ProducerContext) {
        Log.d(tag, "onRequestCancellation ${toString(producerContext)}")
    }

    override fun onProducerFinishWithCancellation(producerContext: ProducerContext, producerName: String, extraMap: MutableMap<String, String>?) {
        Log.d(tag, "onProducerFinishWithCancellation $producerContext, producerName=$producerName, extras=$extraMap")
    }

    override fun onProducerStart(producerContext: ProducerContext, producerName: String) {
        Log.d(tag, "onProducerStart ${toString(producerContext)}, producerName=$producerName")
    }

    override fun onProducerEvent(producerContext: ProducerContext, producerName: String, eventName: String) {
        Log.d(tag, "onProducerEvent ${toString(producerContext)}, producerName=$producerName, event=$eventName")
    }

    override fun onProducerFinishWithSuccess(producerContext: ProducerContext, producerName: String, extraMap: MutableMap<String, String>?) {
        Log.d(tag, "onProducerFinishWithSuccess ${toString(producerContext)}, producerName=$producerName, extras=$extraMap")
    }

    override fun onProducerFinishWithFailure(producerContext: ProducerContext, producerName: String?, t: Throwable?, extraMap: MutableMap<String, String>?) {
        Log.d(tag, "onProducerFinishWithFailure ${toString(producerContext)}, producerName=$producerName, extras=$extraMap")
    }

    override fun onUltimateProducerReached(producerContext: ProducerContext, producerName: String, successful: Boolean) {
        Log.d(tag, "onUltimateProducerReached ${toString(producerContext)}, producer=$producerName, successful=$successful")
    }

    override fun requiresExtraMap(producerContext: ProducerContext, producerName: String) = logExtraMap

    private fun toString(producerContext: ProducerContext) = producerContext.run { "id=$id, extras=$extras, request=$imageRequest" }
}