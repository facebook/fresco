/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import com.facebook.fresco.ui.common.ControllerListener2.Extras
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForwardingControllerListener2Test {

  private lateinit var forwarder: ForwardingControllerListener2<String>
  private lateinit var listener1: RecordingListener
  private lateinit var listener2: RecordingListener

  @Before
  fun setUp() {
    forwarder = ForwardingControllerListener2()
    listener1 = RecordingListener()
    listener2 = RecordingListener()
  }

  @Test
  fun testOnSubmit_forwardsToAllListeners() {
    forwarder.addListener(listener1)
    forwarder.addListener(listener2)
    val extras = Extras()

    forwarder.onSubmit("id1", "callerCtx", extras)

    assertEquals(1, listener1.submitCalls.size)
    assertEquals("id1", listener1.submitCalls[0].id)
    assertEquals("callerCtx", listener1.submitCalls[0].callerContext)
    assertEquals(extras, listener1.submitCalls[0].extras)
    assertEquals(1, listener2.submitCalls.size)
    assertEquals("id1", listener2.submitCalls[0].id)
  }

  @Test
  fun testAllForwardingMethods_forwardToAllListeners() {
    forwarder.addListener(listener1)
    forwarder.addListener(listener2)
    val extras = Extras()
    val throwable = RuntimeException("err")

    forwarder.onSubmit("s1", "ctx", extras)
    forwarder.onFinalImageSet("f1", "info", extras)
    forwarder.onFailure("fail1", throwable, extras)
    forwarder.onRelease("r1", extras)
    forwarder.onIntermediateImageSet("i1", "interInfo")
    forwarder.onIntermediateImageFailed("if1")
    forwarder.onEmptyEvent("emptyCtx")

    // Verify listener1 received all calls with correct arguments
    assertEquals(1, listener1.submitCalls.size)
    assertEquals("s1", listener1.submitCalls[0].id)
    assertEquals(1, listener1.finalImageSetCalls.size)
    assertEquals("f1", listener1.finalImageSetCalls[0].id)
    assertEquals("info", listener1.finalImageSetCalls[0].imageInfo)
    assertEquals(1, listener1.failureCalls.size)
    assertEquals("fail1", listener1.failureCalls[0].id)
    assertEquals(throwable, listener1.failureCalls[0].throwable)
    assertEquals(1, listener1.releaseCalls.size)
    assertEquals("r1", listener1.releaseCalls[0].id)
    assertEquals(1, listener1.intermediateImageSetCalls.size)
    assertEquals("i1", listener1.intermediateImageSetCalls[0].id)
    assertEquals(1, listener1.intermediateImageFailedCalls.size)
    assertEquals("if1", listener1.intermediateImageFailedCalls[0])
    assertEquals(1, listener1.emptyEventCalls.size)
    assertEquals("emptyCtx", listener1.emptyEventCalls[0])

    // Verify listener2 also received all calls
    assertEquals(1, listener2.submitCalls.size)
    assertEquals(1, listener2.finalImageSetCalls.size)
    assertEquals(1, listener2.failureCalls.size)
    assertEquals(1, listener2.releaseCalls.size)
    assertEquals(1, listener2.intermediateImageSetCalls.size)
    assertEquals(1, listener2.intermediateImageFailedCalls.size)
    assertEquals(1, listener2.emptyEventCalls.size)
  }

  @Test
  fun testRemoveListener_stopsForwarding() {
    forwarder.addListener(listener1)
    forwarder.addListener(listener2)
    forwarder.removeListener(listener1)

    forwarder.onSubmit("id1", null, null)

    assertEquals(0, listener1.submitCalls.size)
    assertEquals(1, listener2.submitCalls.size)
  }

  @Test
  fun testRemoveAllListeners_stopsForwarding() {
    forwarder.addListener(listener1)
    forwarder.addListener(listener2)
    forwarder.removeAllListeners()

    forwarder.onSubmit("id1", null, null)

    assertEquals(0, listener1.submitCalls.size)
    assertEquals(0, listener2.submitCalls.size)
  }

  @Test
  fun testExceptionInListener_doesNotAffectOtherListeners() {
    val throwingListener = ThrowingListener()
    forwarder.addListener(throwingListener)
    forwarder.addListener(listener1)

    forwarder.onSubmit("id1", "ctx", null)

    assertEquals(1, listener1.submitCalls.size)
    assertEquals("id1", listener1.submitCalls[0].id)
    assertEquals("ctx", listener1.submitCalls[0].callerContext)
  }

  @Test
  fun testNoListeners_doesNotCorruptState() {
    forwarder.onSubmit("id1", null, null)
    forwarder.onFinalImageSet("id1", null, null)
    forwarder.onFailure("id1", null, null)
    forwarder.onRelease("id1", null)
    forwarder.onIntermediateImageSet("id1", null)
    forwarder.onIntermediateImageFailed("id1")
    forwarder.onEmptyEvent(null)

    // After calling all methods with no listeners, adding a listener should still work
    forwarder.addListener(listener1)
    forwarder.onSubmit("id2", null, null)
    assertEquals(1, listener1.submitCalls.size)
    assertEquals("id2", listener1.submitCalls[0].id)
  }

  @Test
  fun testMultipleCallsAccumulate() {
    forwarder.addListener(listener1)

    forwarder.onSubmit("id1", null, null)
    forwarder.onSubmit("id2", null, null)
    forwarder.onSubmit("id3", null, null)

    assertEquals(3, listener1.submitCalls.size)
    assertEquals("id1", listener1.submitCalls[0].id)
    assertEquals("id2", listener1.submitCalls[1].id)
    assertEquals("id3", listener1.submitCalls[2].id)
  }

  @Test
  fun testAddSameListenerTwice_receivesCallTwice() {
    forwarder.addListener(listener1)
    forwarder.addListener(listener1)

    forwarder.onSubmit("id1", null, null)

    assertEquals(2, listener1.submitCalls.size)
  }

  @Test
  fun testExceptionInMiddleListener_allOthersStillCalled() {
    val listener3 = RecordingListener()
    forwarder.addListener(listener1)
    forwarder.addListener(ThrowingListener())
    forwarder.addListener(listener3)

    forwarder.onRelease("rel1", null)

    assertEquals(1, listener1.releaseCalls.size)
    assertEquals(1, listener3.releaseCalls.size)
    assertEquals("rel1", listener3.releaseCalls[0].id)
  }

  // --- Fake implementations ---

  private data class SubmitCall(val id: String, val callerContext: Any?, val extras: Extras?)

  private data class FinalImageSetCall(val id: String, val imageInfo: String?, val extras: Extras?)

  private data class FailureCall(val id: String, val throwable: Throwable?, val extras: Extras?)

  private data class ReleaseCall(val id: String, val extras: Extras?)

  private data class IntermediateImageSetCall(val id: String, val imageInfo: String?)

  private class RecordingListener : ControllerListener2<String> {
    val submitCalls = mutableListOf<SubmitCall>()
    val finalImageSetCalls = mutableListOf<FinalImageSetCall>()
    val failureCalls = mutableListOf<FailureCall>()
    val releaseCalls = mutableListOf<ReleaseCall>()
    val intermediateImageSetCalls = mutableListOf<IntermediateImageSetCall>()
    val intermediateImageFailedCalls = mutableListOf<String>()
    val emptyEventCalls = mutableListOf<Any?>()

    override fun onSubmit(id: String, callerContext: Any?, extraData: Extras?) {
      submitCalls.add(SubmitCall(id, callerContext, extraData))
    }

    override fun onFinalImageSet(id: String, imageInfo: String?, extraData: Extras?) {
      finalImageSetCalls.add(FinalImageSetCall(id, imageInfo, extraData))
    }

    override fun onFailure(id: String, throwable: Throwable?, extraData: Extras?) {
      failureCalls.add(FailureCall(id, throwable, extraData))
    }

    override fun onRelease(id: String, extraData: Extras?) {
      releaseCalls.add(ReleaseCall(id, extraData))
    }

    override fun onIntermediateImageSet(id: String, imageInfo: String?) {
      intermediateImageSetCalls.add(IntermediateImageSetCall(id, imageInfo))
    }

    override fun onIntermediateImageFailed(id: String) {
      intermediateImageFailedCalls.add(id)
    }

    override fun onEmptyEvent(callerContext: Any?) {
      emptyEventCalls.add(callerContext)
    }
  }

  private class ThrowingListener : ControllerListener2<String> {
    override fun onSubmit(id: String, callerContext: Any?, extraData: Extras?) {
      throw RuntimeException("onSubmit exception")
    }

    override fun onFinalImageSet(id: String, imageInfo: String?, extraData: Extras?) {
      throw RuntimeException("onFinalImageSet exception")
    }

    override fun onFailure(id: String, throwable: Throwable?, extraData: Extras?) {
      throw RuntimeException("onFailure exception")
    }

    override fun onRelease(id: String, extraData: Extras?) {
      throw RuntimeException("onRelease exception")
    }

    override fun onIntermediateImageSet(id: String, imageInfo: String?) {
      throw RuntimeException("onIntermediateImageSet exception")
    }

    override fun onIntermediateImageFailed(id: String) {
      throw RuntimeException("onIntermediateImageFailed exception")
    }

    override fun onEmptyEvent(callerContext: Any?) {
      throw RuntimeException("onEmptyEvent exception")
    }
  }
}
