/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

/**
 * Static methods for secure hashing.
 */
public class SecureHashUtil {

  public static String makeSHA1Hash(String text) {
    try {
      return makeSHA1Hash(text.getBytes("utf-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String makeSHA1Hash(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(bytes, 0, bytes.length);
      byte[] sha1hash = md.digest();
      return convertToHex(sha1hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String makeSHA1HashBase64(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update(bytes, 0, bytes.length);
      byte[] sha1hash = md.digest();
      return Base64.encodeToString(sha1hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String makeMD5Hash(String text) {
    try {
      return makeMD5Hash(text.getBytes("utf-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String makeMD5Hash(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(bytes, 0, bytes.length);
      byte[] sha1hash = md.digest();
      return convertToHex(sha1hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  static final byte[] HEX_CHAR_TABLE = {
      (byte) '0', (byte) '1', (byte) '2', (byte) '3',
      (byte) '4', (byte) '5', (byte) '6', (byte) '7',
      (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
      (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
  };

  private static String convertToHex(byte[] raw) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder(raw.length);
    for (byte b : raw) {
      int v = b & 0xFF;
      sb.append((char) HEX_CHAR_TABLE[v >>> 4]);
      sb.append((char) HEX_CHAR_TABLE[v & 0xF]);
    }
    return sb.toString();
  }
}
