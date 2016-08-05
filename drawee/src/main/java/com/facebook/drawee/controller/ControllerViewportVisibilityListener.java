/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.controller;

/**
 * A listener for {@link AbstractDraweeController} that listens to events regarding visibility of
 * the drawee in the viewport. As Android does not provide these events, the client must call
 * {@link AbstractDraweeController#onViewportVisibilityHint(boolean)} accordingly.
 */
public interface ControllerViewportVisibilityListener {

  /**
   * Called after a client has given the {@link AbstractDraweeController} a hint that the view
   * became visible in the viewport.
   *
   * @param id controller id
   */
  void onDraweeViewportEntry(String id);

  /**
   * Called after a client has given the {@link AbstractDraweeController} a hint that the view
   * is no longer visible in the viewport.
   *
   * @param id controller id
   */
  void onDraweeViewportExit(String id);
}
