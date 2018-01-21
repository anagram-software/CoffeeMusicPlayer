package com.udeshcoffee.android.data.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
* Created by Udathari on 9/26/2017.
*/
@Entity(tableName = "equalizer")
data class EQPreset(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "eqid") val id: Int,
        @ColumnInfo(name = "eqname") val name: String
)