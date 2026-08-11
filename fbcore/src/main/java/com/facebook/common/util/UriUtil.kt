/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.facebook.infer.annotation.Assertions
import com.facebook.infer.annotation.PropagatesNullable
import java.io.File
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.net.URL

object UriUtil {

  /** http scheme for URIs */
  const val HTTP_SCHEME: String = "http"

  const val HTTPS_SCHEME: String = "https"

  /** File scheme for URIs */
  const val LOCAL_FILE_SCHEME: String = "file"

  /** Content URI scheme for URIs */
  const val LOCAL_CONTENT_SCHEME: String = "content"

  /** URI prefix (including scheme) for contact photos */
  private val LOCAL_CONTACT_IMAGE_URI: Uri =
      Uri.withAppendedPath(
          Assertions.assumeNotNull(ContactsContract.AUTHORITY_URI),
          "display_photo",
      )

  /** Asset scheme for URIs */
  const val LOCAL_ASSET_SCHEME: String = "asset"

  /** Resource scheme for URIs */
  const val LOCAL_RESOURCE_SCHEME: String = "res"

  /**
   * Resource scheme for fully qualified resources which might have a package name that is different
   * than the application one. This has the constant value of "android.resource".
   */
  const val QUALIFIED_RESOURCE_SCHEME: String = ContentResolver.SCHEME_ANDROID_RESOURCE

  /** Data scheme for URIs */
  const val DATA_SCHEME: String = "data"

  /**
   * Convert android.net.Uri to java.net.URL as necessary for some networking APIs.
   *
   * @param uri uri to convert
   * @return url pointing to the same resource as uri
   */
  @JvmStatic
  fun uriToUrl(@PropagatesNullable uri: Uri?): URL? {
    if (uri == null) {
      return null
    }

    try {
      return URL(uri.toString())
    } catch (e: MalformedURLException) {
      // This should never happen since we got a valid uri
      throw RuntimeException(e)
    }
  }

  /**
   * Check if uri represents network resource
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "http" or "https"
   */
  @JvmStatic
  fun isNetworkUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return HTTPS_SCHEME == scheme || HTTP_SCHEME == scheme
  }

  /**
   * Check if uri represents local file
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "file"
   */
  @JvmStatic
  fun isLocalFileUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return LOCAL_FILE_SCHEME == scheme
  }

  /**
   * Check if uri represents local content
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "content"
   */
  @JvmStatic
  fun isLocalContentUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return LOCAL_CONTENT_SCHEME == scheme
  }

  /**
   * Checks if the given URI is a general Contact URI, and not a specific display photo.
   *
   * @param uri the URI to check
   * @return true if the uri is a Contact URI, and is not already specifying a display photo.
   */
  @JvmStatic
  fun isLocalContactUri(uri: Uri): Boolean {
    if (uri.path == null) {
      return false
    }
    return isLocalContentUri(uri) &&
        ContactsContract.AUTHORITY == uri.authority &&
        !uri.path!!.startsWith(Assertions.assumeNotNull(LOCAL_CONTACT_IMAGE_URI.path))
  }

  /**
   * Checks if the given URI is for a photo from the device's local media store.
   *
   * @param uri the URI to check
   * @return true if the URI points to a media store photo
   */
  @JvmStatic
  fun isLocalCameraUri(uri: Uri): Boolean {
    val uriString = uri.toString()
    return uriString.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()) ||
        uriString.startsWith(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString())
  }

  /**
   * Check if uri represents local asset
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to "asset"
   */
  @JvmStatic
  fun isLocalAssetUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return LOCAL_ASSET_SCHEME == scheme
  }

  /**
   * Check if uri represents local resource
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to [LOCAL_RESOURCE_SCHEME]
   */
  @JvmStatic
  fun isLocalResourceUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return LOCAL_RESOURCE_SCHEME == scheme
  }

  /**
   * Check if uri represents fully qualified resource URI.
   *
   * @param uri uri to check
   * @return true if uri's scheme is equal to [QUALIFIED_RESOURCE_SCHEME]
   */
  @JvmStatic
  fun isQualifiedResourceUri(uri: Uri?): Boolean {
    val scheme = getSchemeOrNull(uri)
    return QUALIFIED_RESOURCE_SCHEME == scheme
  }

  /** Check if the uri is a data uri */
  @JvmStatic fun isDataUri(uri: Uri?): Boolean = DATA_SCHEME == getSchemeOrNull(uri)

  /**
   * @param uri uri to extract scheme from, possibly null
   * @return null if uri is null, result of uri.getScheme() otherwise
   */
  @JvmStatic fun getSchemeOrNull(uri: Uri?): String? = uri?.scheme

  /**
   * A wrapper around [Uri#parse] that returns null if the input is null.
   *
   * @param uriAsString the uri as a string
   * @return the parsed Uri or null if the input was null
   */
  @JvmStatic
  fun parseUriOrNull(uriAsString: String?): Uri? =
      if (uriAsString != null) Uri.parse(uriAsString) else null

  /**
   * Get the path of a file from the Uri.
   *
   * @param contentResolver the content resolver which will query for the source file
   * @param srcUri The source uri
   * @return The Path for the file or null if doesn't exists
   */
  @JvmStatic
  fun getRealPathFromUri(
      contentResolver: ContentResolver,
      srcUri: Uri,
  ): String? {
    var result: String? = null
    var uri = srcUri
    val mimeTypeString = contentResolver.getType(uri)
    if (isLocalContentUri(uri)) {
      val isVideo = mimeTypeString?.startsWith("video/") == true
      var selection: String? = null
      var selectionArgs: Array<String>? = null
      if ("com.android.providers.media.documents" == uri.authority) {
        val documentId = DocumentsContract.getDocumentId(uri)
        checkNotNull(documentId)
        uri = checkNotNull(getExternalContentUri(isVideo))
        selection = "${getMediaIdString(isVideo)}=?"
        selectionArgs =
            arrayOf(
                documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1],
            )
      }
      val cursor =
          contentResolver.query(
              uri,
              arrayOf(getDataPathString(isVideo)),
              selection,
              selectionArgs,
              null,
          )
      try {
        if (cursor?.moveToFirst() == true) {
          val idx = cursor.getColumnIndexOrThrow(getDataPathString(isVideo))
          if (idx != -1) {
            result = cursor.getString(idx)
          }
        }
      } finally {
        cursor?.close()
      }
    } else if (isLocalFileUri(uri)) {
      result = uri.path
    }
    return result
  }

  private fun getExternalContentUri(isVideo: Boolean): Uri =
      if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

  private fun getMediaIdString(isVideo: Boolean): String =
      if (isVideo) MediaStore.Video.Media._ID else MediaStore.Images.Media._ID

  private fun getDataPathString(isVideo: Boolean): String =
      if (isVideo) MediaStore.Video.Media.DATA else MediaStore.Images.Media.DATA

  /**
   * Gets the AssetFileDescriptor for a local file. This offers an alternative solution for opening
   * content:// scheme files
   *
   * @param contentResolver the content resolver which will query for the source file
   * @param srcUri The source uri
   * @return The AssetFileDescriptor for the file or null if it doesn't exist
   */
  @JvmStatic
  fun getAssetFileDescriptor(
      contentResolver: ContentResolver,
      srcUri: Uri,
  ): AssetFileDescriptor? {
    if (isLocalContentUri(srcUri)) {
      return try {
        contentResolver.openAssetFileDescriptor(srcUri, "r")
      } catch (e: FileNotFoundException) {
        null
      }
    }
    return null
  }

  /**
   * Returns a URI for a given file using [Uri#fromFile(File)].
   *
   * @param file a file with a valid path
   * @return the URI
   */
  @JvmStatic fun getUriForFile(file: File): Uri = Uri.fromFile(file)

  /**
   * Return a URI for the given resource ID. The returned URI consists of a [LOCAL_RESOURCE_SCHEME]
   * scheme and the resource ID as path.
   *
   * @param resourceId the resource ID to use
   * @return the URI
   */
  @JvmStatic
  fun getUriForResourceId(resourceId: Int): Uri =
      Uri.Builder().scheme(LOCAL_RESOURCE_SCHEME).path(resourceId.toString()).build()

  /**
   * Returns a URI for the given resource ID in the given package. Use this method only if you need
   * to specify a package name different to your application's main package.
   *
   * @param packageName a package name (e.g. com.facebook.myapp.plugin)
   * @param resourceId to resource ID to use
   * @return the URI
   */
  @JvmStatic
  fun getUriForQualifiedResource(packageName: String, resourceId: Int): Uri =
      Uri.Builder()
          .scheme(QUALIFIED_RESOURCE_SCHEME)
          .authority(packageName)
          .path(resourceId.toString())
          .build()
}

/**
 * Returns a copy of this URI containing exactly one query parameter named [name] with [value] as
 * its value. Existing values with the same name are removed. All other encoded query components
 * remain byte-for-byte unchanged and in their original order. Opaque URIs are returned unchanged.
 */
fun Uri.withQueryParameter(name: String, value: String): Uri {
  if (isOpaque) {
    return this
  }

  val preservedEncodedParameters =
      encodedQuery
          ?.takeIf { it.isNotEmpty() }
          ?.split('&')
          ?.filterNot { encodedParameter ->
            Uri.decode(encodedParameter.substringBefore('=')) == name
          }
  val builder = buildUpon().clearQuery()
  if (!preservedEncodedParameters.isNullOrEmpty()) {
    builder.encodedQuery(preservedEncodedParameters.joinToString("&"))
  }
  return builder.appendQueryParameter(name, value).build()
}
