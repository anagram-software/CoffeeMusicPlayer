package com.udeshcoffee.android.model

import android.net.Uri

/**
 * Created by Udathari on 8/26/2017.
 */
data class Query(val uri: Uri, val projection: Array<String>?, val selection: String?, val args: Array<String>?, val sort: String?)