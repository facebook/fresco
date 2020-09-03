/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include <android/log.h>
#include <pthread.h>
#include <string.h>


class RWLock {
 public:
  RWLock() : mutex_(PTHREAD_RWLOCK_INITIALIZER){};
  // No copying allowed
  RWLock(const RWLock&) = delete;
  void operator=(const RWLock&) = delete;

  ~RWLock() {
    pthread_rwlock_destroy(&mutex_);
  };

  inline int ReadLock() {
    return pthread_rwlock_rdlock(&mutex_);
  };

  inline int ReadUnlock() {
    return pthread_rwlock_unlock(&mutex_);
  };

  inline int WriteLock() {
    return pthread_rwlock_wrlock(&mutex_);
  }

  inline int WriteUnlock() {
    return pthread_rwlock_unlock(&mutex_);
  }

 private:
  pthread_rwlock_t mutex_;
};

class ReaderLock {
 private:
  RWLock* rlock_;

 public:
  inline explicit ReaderLock(RWLock* rlock) : rlock_(rlock) {
    int ret = rlock_->ReadLock();
    if (ret != 0) {
      __android_log_print(
          ANDROID_LOG_ERROR,
          LOG_TAG,
          "pthread_rwlock_rdlock returned %s",
          strerror(ret));
    }
  }

  inline ~ReaderLock() {
    int ret = rlock_->ReadUnlock();
    if (ret != 0) {
      __android_log_print(
          ANDROID_LOG_ERROR,
          LOG_TAG,
          "pthread_rwlock_unlock read returned %s",
          strerror(ret));
    }
  }
};

class WriterLock {
 private:
  RWLock* wlock_;

 public:
  inline explicit WriterLock(RWLock* wlock) : wlock_(wlock) {
    int ret = wlock_->WriteLock();
    if (ret != 0) {
      __android_log_print(
          ANDROID_LOG_ERROR,
          LOG_TAG,
          "pthread_rwlock_wrlock returned %s",
          strerror(ret));
    }
  }

  inline ~WriterLock() {
    int ret = wlock_->WriteUnlock();
    if (ret != 0) {
      __android_log_print(
          ANDROID_LOG_ERROR,
          LOG_TAG,
          "pthread_rwlock_unlock write returned %s",
          strerror(ret));
    }
  }
};
