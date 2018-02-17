/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import android.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    return makeHash(bytes, "SHA-1");
  }

  public static String makeSHA256Hash(byte[] bytes) {
    return makeHash(bytes, "SHA-256");
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
    return makeHash(bytes, "MD5");
  }

  public static String makeMD5Hash(InputStream stream) throws IOException {
    return makeHash(stream, "MD5");
  }

  static final byte[] HEX_CHAR_TABLE = {
      (byte) '0', (byte) '1', (byte) '2', (byte) '3',
      (byte) '4', (byte) '5', (byte) '6', (byte) '7',
      (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
      (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
  };

  public static String convertToHex(byte[] raw) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder(raw.length);
    for (byte b : raw) {
      int v = b & 0xFF;
      sb.append((char) HEX_CHAR_TABLE[v >>> 4]);
      sb.append((char) HEX_CHAR_TABLE[v & 0xF]);
    }
    return sb.toString();
  }

  private static String makeHash(byte[] bytes, String algorithm) {
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      md.update(bytes, 0, bytes.length);
      byte[] hash = md.digest();
      return convertToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int BUFFER_SIZE = 4096;

  private static String makeHash(InputStream stream, String algorithm) throws IOException {
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      byte[] buffer = new byte[BUFFER_SIZE];
      int read;
      while ((read = stream.read(buffer)) > 0) {
        md.update(buffer, 0, read);
      }
      byte[] hash = md.digest();
      return convertToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
