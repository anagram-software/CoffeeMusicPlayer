package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
* Created by Udathari on 12/19/2017.
*/
@Entity(tableName = "lyrics")
data class Lyric constructor(
        @PrimaryKey @ColumnInfo(name = "songid") var id: Long,
        @ColumnInfo(name = "lyric") var lyric: String
)
