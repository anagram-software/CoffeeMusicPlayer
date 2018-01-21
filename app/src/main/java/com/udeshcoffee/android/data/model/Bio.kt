package com.udeshcoffee.android.data.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
* Created by Udathari on 12/19/2017.
*/
@Entity(tableName = "bio")
data class Bio constructor(
        @PrimaryKey @ColumnInfo(name = "artistid") var id: Long,
        @ColumnInfo(name = "bio") var bio: String?,
        @ColumnInfo(name = "tags") var tags: String?
)
