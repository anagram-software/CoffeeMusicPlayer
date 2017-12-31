/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.udeshcoffee.android.utils

import android.content.Context
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.local.LocalDataSource
import com.udeshcoffee.android.data.local.LocalDatabase
import com.udeshcoffee.android.data.remote.RemoteDataSource

object Injection {

    fun provideMediaRepository(context: Context): MediaRepository =
            MediaRepository.getInstance(context, provideLocalSource(context))

    fun provideDataRepository(context: Context): DataRepository =
            DataRepository.getInstance(provideLocalSource(context), RemoteDataSource)

    private fun provideLocalSource(context: Context): LocalDataSource {
        val database = LocalDatabase.getInstance(context)
        return LocalDataSource(database.dataDao())
    }
}
