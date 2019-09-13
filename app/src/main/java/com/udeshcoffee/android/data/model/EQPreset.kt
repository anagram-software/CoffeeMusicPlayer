package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
* Created by Udathari on 9/26/2017.
*/
@Entity(tableName = "equalizer")
data class EQPreset(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "eqid") val id: Int,
        @ColumnInfo(name = "eqname") val name: String
)