/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.os.SystemClock
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.JobScheduler.JobRunnable
import com.facebook.imagepipeline.producers.JobScheduler.JobStartExecutorSupplier
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import com.facebook.imagepipeline.testing.TestScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JobSchedulerTest {
  private class TestJobRunnable : JobRunnable {
    class Job(
        encodedImage: EncodedImage?,
        @field:Consumer.Status @param:Consumer.Status val status: Int
    ) {
      val encodedImage: EncodedImage? = EncodedImage.cloneOrNull(encodedImage)
    }

    val running: AtomicBoolean = AtomicBoolean()
    val wait: AtomicBoolean = AtomicBoolean()
    val fail: AtomicBoolean = AtomicBoolean()
    val jobs: ArrayList<Job?> = ArrayList()

    override fun run(encodedImage: EncodedImage?, @Consumer.Status status: Int) {
      running.set(true)
      try {
        waitForCondition(wait, false)
        if (fail.get()) {
          throw RuntimeException()
        } else {
          jobs.add(Job(encodedImage, status))
        }
      } finally {
        running.set(false)
      }
    }
  }

  private lateinit var fakeClockForTime: FakeClock
  private lateinit var fakeClockForWorker: FakeClock
  private lateinit var testExecutorService: TestExecutorService
  private lateinit var fakeClockForScheduled: FakeClock
  private lateinit var testScheduledExecutorService: TestScheduledExecutorService
  private lateinit var testJobRunnable: TestJobRunnable
  private lateinit var jobScheduler: JobScheduler
  private lateinit var mockedSystemClock: MockedStatic<SystemClock>
  private lateinit var mockedJobSchedulerJobStartExecutorSupplier:
      MockedStatic<JobStartExecutorSupplier>

  @Before
  fun setUp() {
    mockedJobSchedulerJobStartExecutorSupplier =
        Mockito.mockStatic(JobStartExecutorSupplier::class.java)
    mockedSystemClock = Mockito.mockStatic(SystemClock::class.java)
    MockitoAnnotations.initMocks(this)
    fakeClockForTime = FakeClock()
    fakeClockForWorker = FakeClock()
    fakeClockForScheduled = FakeClock()
    fakeClockForTime.incrementBy(1000)
    fakeClockForWorker.incrementBy(1000)
    fakeClockForScheduled.incrementBy(1000)
    mockedSystemClock
        .`when`<Any> { SystemClock.uptimeMillis() }
        .thenAnswer(
            object : Answer<Long> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock?): Long {
                return fakeClockForTime.now()
              }
            })

    testExecutorService = TestExecutorService(fakeClockForWorker)
    testScheduledExecutorService = TestScheduledExecutorService(fakeClockForScheduled)
    mockedJobSchedulerJobStartExecutorSupplier
        .`when`<Any> { JobStartExecutorSupplier.get() }
        .thenAnswer(Answer { testScheduledExecutorService })

    testJobRunnable = TestJobRunnable()
    jobScheduler = JobScheduler(testExecutorService, testJobRunnable, INTERVAL)
  }

  private fun fakeEncodedImage(): EncodedImage {
    val buf = Mockito.mock<PooledByteBuffer>(PooledByteBuffer::class.java)
    val ref = CloseableReference.of<PooledByteBuffer?>(buf)
    return EncodedImage(ref)
  }

  @After
  fun tearDownStaticMocks() {
    mockedSystemClock.close()
    mockedJobSchedulerJobStartExecutorSupplier.close()
  }

  @Test
  fun testUpdate_Intermediate() {
    val encodedImage = fakeEncodedImage()
    assertThat(jobScheduler.updateJob(encodedImage, Consumer.NO_FLAGS)).isTrue()
    assertThat(jobScheduler.mEncodedImage).isNotSameAs(encodedImage)
    assertReferencesEqual(encodedImage, jobScheduler.mEncodedImage)
    assertThat(jobScheduler.mStatus).isEqualTo(Consumer.NO_FLAGS)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  fun testUpdate_Intermediate_Invalid() {
    assertThat(jobScheduler.updateJob(fakeEncodedImage(), Consumer.NO_FLAGS)).isTrue()
    assertThat(jobScheduler.updateJob(null, Consumer.NO_FLAGS)).isFalse()
    assertThat(jobScheduler.mEncodedImage).isNotNull()
    assertThat(jobScheduler.mStatus).isEqualTo(Consumer.NO_FLAGS)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  fun testUpdate_Last() {
    val encodedImage = fakeEncodedImage()
    assertThat(jobScheduler.updateJob(encodedImage, Consumer.IS_LAST)).isTrue()
    assertThat(jobScheduler.mEncodedImage).isNotSameAs(encodedImage)
    assertReferencesEqual(encodedImage, jobScheduler.mEncodedImage)
    assertThat(jobScheduler.mStatus).isEqualTo(Consumer.IS_LAST)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  fun testUpdate_Placeholder() {
    val encodedImage = fakeEncodedImage()
    assertThat(jobScheduler.updateJob(encodedImage, Consumer.IS_PLACEHOLDER)).isTrue()
    assertThat(jobScheduler.mEncodedImage).isNotSameAs(encodedImage)
    assertReferencesEqual(encodedImage, jobScheduler.mEncodedImage)
    assertThat(jobScheduler.mStatus).isEqualTo(Consumer.IS_PLACEHOLDER)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  fun testUpdate_Last_Null() {
    assertThat(jobScheduler.updateJob(fakeEncodedImage(), Consumer.NO_FLAGS)).isTrue()
    assertThat(jobScheduler.updateJob(null, Consumer.IS_LAST)).isTrue()
    assertThat(jobScheduler.mEncodedImage).isEqualTo(null)
    assertThat(jobScheduler.mStatus).isEqualTo(Consumer.IS_LAST)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun testClear() {
    val encodedImage = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage, Consumer.IS_LAST)
    jobScheduler.clearJob()
    assertThat(jobScheduler.mEncodedImage).isEqualTo(null)
    encodedImage.close()
    assertThat(encodedImage.byteBufferRef).isNull()
  }

  @Test
  fun testSchedule_Intermediate() {
    val encodedImage = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage, Consumer.NO_FLAGS)

    assertThat(jobScheduler.scheduleJob()).isTrue()
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(1)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)

    fakeClockForTime.incrementBy(1234)
    fakeClockForWorker.incrementBy(1234)
    fakeClockForScheduled.incrementBy(1234)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)
    testJobRunnable.jobs[0]?.let { assertJobsEqual(it, encodedImage, Consumer.NO_FLAGS) }
  }

  @Test
  fun testSchedule_Intermediate_Invalid() {
    jobScheduler.updateJob(null, Consumer.NO_FLAGS)
    assertThat(jobScheduler.scheduleJob()).isFalse()
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  @Test
  fun testSchedule_Last_Null() {
    jobScheduler.updateJob(null, Consumer.IS_LAST)
    assertThat(jobScheduler.scheduleJob()).isTrue()
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(1)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)

    fakeClockForTime.incrementBy(1234)
    fakeClockForWorker.incrementBy(1234)
    fakeClockForScheduled.incrementBy(1234)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)
    testJobRunnable.jobs[0]?.let { assertJobsEqual(it, null, Consumer.IS_LAST) }
  }

  @Test
  @Throws(Exception::class)
  fun testSchedule_Last_Idle() {
    val encodedImage = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage, Consumer.IS_LAST)

    assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.IDLE)
    assertThat(jobScheduler.scheduleJob()).isTrue()
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(1)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)

    fakeClockForTime.incrementBy(1234)
    fakeClockForWorker.incrementBy(1234)
    fakeClockForScheduled.incrementBy(1234)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)
    testJobRunnable.jobs[0]?.let { assertJobsEqual(it, encodedImage, Consumer.IS_LAST) }

    testJobRunnable.jobs[0]?.encodedImage?.close()
    encodedImage.close()
    assertThat(encodedImage.byteBufferRef).isNull()
  }

  @Test
  fun testSchedule_Last_Queued() {
    jobScheduler.updateJob(fakeEncodedImage(), Consumer.IS_LAST)
    assertThat(jobScheduler.scheduleJob()).isTrue()

    val encodedImage2 = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage2, Consumer.IS_LAST)
    assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.QUEUED)
    assertThat(jobScheduler.scheduleJob()).isTrue()

    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(1)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)

    fakeClockForTime.incrementBy(1234)
    fakeClockForWorker.incrementBy(1234)
    fakeClockForScheduled.incrementBy(1234)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)
    testJobRunnable.jobs[0]?.let { assertJobsEqual(it, encodedImage2, Consumer.IS_LAST) }
  }

  @Test
  fun testSchedule_Last_Running_And_Pending() {
    val encodedImage1 = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage1, Consumer.IS_LAST)
    assertThat(jobScheduler.scheduleJob()).isTrue()

    val encodedImage2 = fakeEncodedImage()
    val encodedImage3 = fakeEncodedImage()

    Executors.newFixedThreadPool(1)
        .execute(
            object : Runnable {
              override fun run() {
                // wait until the job starts running
                waitForCondition(testJobRunnable.running, true)
                assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
                assertThat(testExecutorService.pendingCount).isEqualTo(0)
                assertThat(testJobRunnable.jobs.size).isEqualTo(0)

                jobScheduler.updateJob(encodedImage2, Consumer.IS_LAST)
                assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.RUNNING)
                assertThat(jobScheduler.scheduleJob()).isTrue()
                assertThat(jobScheduler.mJobState)
                    .isEqualTo(JobScheduler.JobState.RUNNING_AND_PENDING)

                jobScheduler.updateJob(encodedImage3, Consumer.IS_LAST)
                assertThat(jobScheduler.mJobState)
                    .isEqualTo(JobScheduler.JobState.RUNNING_AND_PENDING)
                assertThat(jobScheduler.scheduleJob()).isTrue()

                assertThat(jobScheduler.mJobState)
                    .isEqualTo(JobScheduler.JobState.RUNNING_AND_PENDING)
                testJobRunnable.wait.set(false)
              }
            })

    // block running until the above code executed on another thread finishes
    testJobRunnable.wait.set(true)

    fakeClockForTime.incrementBy(0)
    fakeClockForScheduled.incrementBy(0)
    fakeClockForWorker.incrementBy(0) // this line blocks
    assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.QUEUED)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(1)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)
    testJobRunnable.jobs[0]?.let { assertJobsEqual(it, encodedImage1, Consumer.IS_LAST) }

    fakeClockForTime.incrementBy(INTERVAL.toLong())
    fakeClockForScheduled.incrementBy(INTERVAL.toLong())
    fakeClockForWorker.incrementBy(INTERVAL.toLong())
    assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.IDLE)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(2)
    testJobRunnable.jobs[1]?.let { assertJobsEqual(it, encodedImage3, Consumer.IS_LAST) }
  }

  @Test
  fun testSchedule_TooSoon() {
    val encodedImage1 = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage1, Consumer.NO_FLAGS)
    jobScheduler.scheduleJob()
    fakeClockForTime.incrementBy(1234)
    fakeClockForWorker.incrementBy(1234)
    fakeClockForScheduled.incrementBy(1234)

    val encodedImage2 = fakeEncodedImage()
    jobScheduler.updateJob(encodedImage2, Consumer.IS_LAST)
    fakeClockForTime.incrementBy((INTERVAL - 5).toLong())
    fakeClockForWorker.incrementBy((INTERVAL - 5).toLong())
    fakeClockForScheduled.incrementBy((INTERVAL - 5).toLong())
    jobScheduler.scheduleJob()
    fakeClockForTime.incrementBy(0)
    fakeClockForWorker.incrementBy(0)
    fakeClockForScheduled.incrementBy(0)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(1)
    assertThat(testScheduledExecutorService.getScheduledQueue().getNextPendingCommandDelay())
        .isEqualTo(5)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)

    fakeClockForTime.incrementBy(5)
    fakeClockForWorker.incrementBy(5)
    fakeClockForScheduled.incrementBy(5)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(1)
    assertThat(testJobRunnable.jobs.size).isEqualTo(1)

    fakeClockForTime.incrementBy(0)
    fakeClockForWorker.incrementBy(0)
    fakeClockForScheduled.incrementBy(0)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(2)
    testJobRunnable.jobs[1]?.let { assertJobsEqual(it, encodedImage2, Consumer.IS_LAST) }
  }

  @Test
  fun testFailure() {
    jobScheduler.updateJob(fakeEncodedImage(), Consumer.NO_FLAGS)
    jobScheduler.scheduleJob()
    testJobRunnable.fail.set(true)
    try {
      fakeClockForTime.incrementBy(1234)
      fakeClockForWorker.incrementBy(0)
      throw RuntimeException("job should have failed, but it didn't.")
    } catch (e: Exception) {
      // expected
    }
    assertThat(jobScheduler.mJobState).isEqualTo(JobScheduler.JobState.IDLE)
    assertThat(testScheduledExecutorService.pendingCount).isEqualTo(0)
    assertThat(testExecutorService.pendingCount).isEqualTo(0)
    assertThat(testJobRunnable.jobs.size).isEqualTo(0)
  }

  companion object {
    private const val INTERVAL = 100

    private fun assertJobsEqual(
        job: TestJobRunnable.Job,
        encodedImage: EncodedImage?,
        @Consumer.Status status: Int
    ) {
      job.encodedImage?.let { assertReferencesEqual(encodedImage, it) }
      assertThat(job.status).isEqualTo(status)
    }

    private fun assertReferencesEqual(expected: EncodedImage?, actual: EncodedImage?) {
      if (expected == null) {
        assertThat(actual).isNull()
      } else {
        if (expected.getByteBufferRef() == null) {
          assertThat(actual?.byteBufferRef).isNull()
        } else {
          assertThat(actual?.byteBufferRef).isNotNull()
          assertThat(actual?.isValid).isEqualTo(expected.isValid)
          assertThat(actual?.byteBufferRef?.getUnderlyingReferenceTestOnly())
              .isEqualTo(expected.byteBufferRef.getUnderlyingReferenceTestOnly())
        }
      }
    }

    private fun waitForCondition(wait: AtomicBoolean, value: Boolean) {
      var cnt = 100
      while (wait.get() != value) {
        try {
          Thread.sleep(1)
        } catch (e: InterruptedException) {
          // ignore
        }
        if (--cnt < 0) {
          throw RuntimeException("Waited for too long!")
        }
      }
    }
  }
}
