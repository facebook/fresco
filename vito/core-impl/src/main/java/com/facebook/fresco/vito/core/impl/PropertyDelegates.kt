/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.closeables.AutoCleanupDelegate
import com.facebook.datasource.DataSource

class DataSourceCleanupDelegate :
    AutoCleanupDelegate<DataSource<out Any>>(null, dataSourceCleanupFunction)

private val dataSourceCleanupFunction: (DataSource<out Any>) -> Unit = { it.close() }
