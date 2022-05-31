/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import com.facebook.fresco.vito.source.ImageSource
import kotlin.jvm.Throws

object ImageSourceParser {

  private const val HIERARCHY_TOKEN = "::"
  private const val OPEN_TOKEN = '{'
  private const val CLOSE_TOKEN = '}'
  private const val SEPARATOR_TOKEN = ','
  private const val ASSIGN_TOKEN = '='
  private const val WHITESPACE = ' '

  fun convertSourceToKeyValue(imageSourceStr: String?): List<Pair<String, String>> {
    if (imageSourceStr == null) {
      return listOf()
    }

    val keyValues = mutableListOf<Pair<String, String>>()

    try {
      getKeyValuesFromString(imageSourceStr, keyValues)
    } catch (exception: ImageSourceSyntaxException) {
      exception.printStackTrace()
      return emptyList()
    }

    return keyValues
  }

  @Throws(ImageSourceSyntaxException::class)
  private fun getKeyValuesFromString(source: String, keyValues: MutableList<Pair<String, String>>) {

    var index = 0
    val key = StringBuilder()
    val value = StringBuilder()
    var parentKey: String? = null

    val throwable =
        ImageSourceSyntaxException(
            "source is not a valid representation for instance ${ImageSource::class.java.name}")

    outer@ while (index < source.length) {

      when (source[index]) {
        OPEN_TOKEN,
        SEPARATOR_TOKEN -> {

          ++index

          if (key.isNotBlank() && value.isEmpty()) {
            parentKey = key.toString()
            addPairAndClearBuffer(keyValues, key, value)
            continue
          }
          while (index < source.length && source[index] != ASSIGN_TOKEN) {
            if (source[index].isWhitespace()) {
              ++index
            } else if (source[index] == OPEN_TOKEN) {
              val parentType = parentKey
              val parentField = key.toString().trimStart()

              parentKey = value.toString()

              if (parentType.isNullOrEmpty()) {
                throw throwable
              }

              key.clear()
              key.append("$parentType$HIERARCHY_TOKEN$parentField")

              addPairAndClearBuffer(keyValues, key, value)

              key.append('\n')

              continue@outer
            } else {
              key.append(source[index++])
            }
          }
        }
        ASSIGN_TOKEN -> {
          ++index
          while (index < source.length &&
              source[index] != SEPARATOR_TOKEN &&
              source[index] != CLOSE_TOKEN) {

            if (source[index] == WHITESPACE && value.isEmpty()) {
              ++index
            } else if (source[index] == OPEN_TOKEN) {

              val parentType = parentKey
              val parentField = key.toString().trimStart()

              parentKey = value.toString()

              if (parentType.isNullOrEmpty()) {
                throw throwable
              }

              key.clear()
              key.append("$parentType$HIERARCHY_TOKEN$parentField")

              addPairAndClearBuffer(keyValues, key, value)

              continue@outer
            } else {
              value.append(source[index++])
            }
          }
          addPairAndClearBuffer(keyValues, key, value)
        }
        WHITESPACE,
        CLOSE_TOKEN -> {
          ++index
        }
        else -> {
          key.append(source[index++])
        }
      }
    }
  }

  private fun addPairAndClearBuffer(
      keyValues: MutableList<Pair<String, String>>,
      key: StringBuilder,
      value: StringBuilder
  ) {
    if (key.isEmpty() && value.isEmpty()) {
      return
    }

    keyValues.add(Pair(key.toString(), value.toString()))

    key.clear()
    value.clear()
  }
}
