package com.udeshcoffee.android.data.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by Udathari on 12/19/2017.
 */
@Entity(tableName = "favorites")
data class Favorite constructor(
        @PrimaryKey @ColumnInfo(name = "fav") var id: Long
)
