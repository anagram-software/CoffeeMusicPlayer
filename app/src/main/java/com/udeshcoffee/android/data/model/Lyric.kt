package com.udeshcoffee.android.data.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
* Created by Udathari on 12/19/2017.
*/
@Entity(tableName = "lyrics")
data class Lyric constructor(
        @PrimaryKey @ColumnInfo(name = "songid") var id: Long,
        @ColumnInfo(name = "lyric") var lyric: String
)
