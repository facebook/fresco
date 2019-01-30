/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.imagepipeline.systrace;

import javax.annotation.Nullable;

/**
 * This is intended as a hook into {@code android.os.Trace}, but allows you to provide your own
 * functionality. Use it as
 *
 * <p>{@code FrescoSystrace.beginSection("tag"); ... FrescoSystrace.endSection(); } As a default, it
 * simply calls {@code android.os.Trace} (see {@link DefaultFrescoSystrace}). You may supply your
 * own with {@link FrescoSystrace#provide(Systrace)}.
 */
public class FrescoSystrace {

  /** Convenience implementation of ArgsBuilder to use when we aren't tracing. */
  public static final ArgsBuilder NO_OP_ARGS_BUILDER = new NoOpArgsBuilder();

  private static volatile @Nullable Systrace sInstance = null;

  public interface Systrace {
    void beginSection(String name);

    ArgsBuilder beginSectionWithArgs(String name);

    void endSection();

    boolean isTracing();
  }

  /** Object that accumulates arguments. */
  public interface ArgsBuilder {

    /**
     * Write the full message to the Systrace buffer.
     *
     * <p>You must call this to log the trace message.
     */
    void flush();

    /**
     * Logs an argument whose value is any object. It will be stringified with {@link
     * String#valueOf(Object)}.
     */
    ArgsBuilder arg(String key, Object value);

    /**
     * Logs an argument whose value is an int. It will be stringified with {@link
     * String#valueOf(int)}.
     */
    ArgsBuilder arg(String key, int value);

    /**
     * Logs an argument whose value is a long. It will be stringified with {@link
     * String#valueOf(long)}.
     */
    ArgsBuilder arg(String key, long value);

    /**
     * Logs an argument whose value is a double. It will be stringified with {@link
     * String#valueOf(double)}.
     */
    ArgsBuilder arg(String key, double value);
  }

  private FrescoSystrace() {}

  public static void provide(Systrace instance) {
    sInstance = instance;
  }

  public static void beginSection(String name) {
    getInstance().beginSection(name);
  }

  public static ArgsBuilder beginSectionWithArgs(String name) {
    return getInstance().beginSectionWithArgs(name);
  }

  public static void endSection() {
    getInstance().endSection();
  }

  public static boolean isTracing() {
    return getInstance().isTracing();
  }

  private static Systrace getInstance() {
    if (sInstance == null) {
      synchronized (FrescoSystrace.class) {
        if (sInstance == null) {
          sInstance = new DefaultFrescoSystrace();
        }
      }
    }
    return sInstance;
  }

  private static final class NoOpArgsBuilder implements ArgsBuilder {

    @Override
    public void flush() {}

    @Override
    public ArgsBuilder arg(String key, Object value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, int value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, long value) {
      return this;
    }

    @Override
    public ArgsBuilder arg(String key, double value) {
      return this;
    }
  }
}
