package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
* Created by Udathari on 12/19/2017.
*/
@Entity(tableName = "favorites")
data class Favorite constructor(
        @PrimaryKey @ColumnInfo(name = "fav") var id: Long
)
