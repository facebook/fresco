/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.soloader;

/**
 * A shim for loading shared libraries that the app can override.
 */
public class SoLoaderShim {

  /**
   * Handler that can be overridden by the application.
   */
  public interface Handler {

    void loadLibrary(String libraryName);
  }

  /**
   * Default handler for loading libraries.
   */
  public static class DefaultHandler implements Handler {

    @Override
    public void loadLibrary(String libraryName) {
      System.loadLibrary(libraryName);
    }
  }

  private static volatile Handler sHandler = new DefaultHandler();

  /**
   * Sets the handler.
   *
   * @param handler the new handler
   */
  public static void setHandler(Handler handler) {
    if (handler == null) {
      throw new NullPointerException("Handler cannot be null");
    }
    sHandler = handler;
  }

  /**
   * See {@link Runtime#loadLibrary}.
   *
   * @param libraryName the library to load
   */
  public static void loadLibrary(String libraryName) {
    sHandler.loadLibrary(libraryName);
  }

  public static void setInTestMode() {
    setHandler(
        new Handler() {
          @Override
          public void loadLibrary(String libraryName) {
          }
        });
  }
}
