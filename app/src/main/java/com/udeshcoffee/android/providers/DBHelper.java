package com.udeshcoffee.android.providers;

/**
 * Created by Udathari on 2/27/2017.
 */

//public class DBHelper extends SQLiteOpenHelper {
//    private static final int DATABASE_VERSION = 5;
//
//    // Change Log
//    // Version 4 - Added "KEY_TAGS" to "TABLE_ARTIST_BIO"
//
//    // DB Name
//    private static final String DATABASE_NAME = "CoffeePlayer";
//
//    public static String  SELECT_FAVS = "SELECT " + DBHelper.KEY_FAV +
//            " FROM " + DBHelper.TABLE_FAVS;
//
//
//    public static String IS_FAV = "SELECT  * FROM " + DBHelper.TABLE_FAVS + " WHERE " +
//            DBHelper.KEY_FAV + " = ";
//
//    // Tables
//    public static final String TABLE_PLAY_COUNT = "playcount";
//    public static final String TABLE_FAVS = "favorites";
//    private static final String TABLE_ARTIST_BIO = "bio";
//    public static final String TABLE_LYRICS = "lyrics";
//    private static final String TABLE_EQ = "equalizer";
//
//    // Legacy Tables
//    private static final String TABLE_SONGS = "songs";
//    private static final String TABLE_ALBUMS = "albums";
//    private static final String TABLE_ARTISTS = "artists";
//    private static final String TABLE_GENRES = "genres";
//    private static final String TABLE_PLAYLISTS = "playlists";
//    private static final String TABLE_LAST = "lasts";
//    private static final String TABLE_RECENT = "recents";
//    private static final String TABLE_RECENTLY_PLAYED = "rplayeds";
//
//    public static final String KEY_SONG_ID = "songid";
//    private static final String KEY_ARTIST_ID = "artistid";
//    private static final String KEY_BIO = "bio";
//    private static final String KEY_TAGS = "tags";
//    public static final String KEY_PLAY_COUNT = "rplayedid";
//    public static final String KEY_PLAY_TIME = "rplayed";
//    public static final String KEY_FAV = "fav";
//    public static final String KEY_LYRIC = "lyric";
//    public static final String KEY_EQ_ID = "eqid";
//    public static final String KEY_EQ_NAME = "eqname";
//
//    public DBHelper(Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        //3rd argument to be passed is CursorFactory instance
//    }
//
//    // Creating Tables
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        String CREATE_PLAY_COUNT_TABLE = "CREATE TABLE " + TABLE_PLAY_COUNT + "("
//                + KEY_SONG_ID + " INTEGER PRIMARY KEY, "
//                + KEY_PLAY_COUNT + " INTEGER, "
//                + KEY_PLAY_TIME + " INTEGER )";
//        db.execSQL(CREATE_PLAY_COUNT_TABLE);
//
//        String CREATE_FAV_TABLE = "CREATE TABLE " + TABLE_FAVS + "("
//                + KEY_FAV + " INTEGER PRIMARY KEY)";
//        db.execSQL(CREATE_FAV_TABLE);
//
//        String CREATE_ARTIST_BIO_TABLE = "CREATE TABLE " + TABLE_ARTIST_BIO + "("
//                + KEY_ARTIST_ID + " INTEGER PRIMARY KEY, "
//                + KEY_BIO + " TEXT, "
//                + KEY_TAGS + " TEXT )";
//        db.execSQL(CREATE_ARTIST_BIO_TABLE);
//
//        String CREATE_LYRIC_TABLE = "CREATE TABLE " + TABLE_LYRICS + "("
//                + KEY_SONG_ID + " INTEGER PRIMARY KEY, "
//                + KEY_LYRIC + " TEXT )";
//        db.execSQL(CREATE_LYRIC_TABLE);
//
//        String CREATE_EQ_TABLE = "CREATE TABLE " + TABLE_EQ + "("
//                + KEY_EQ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + KEY_EQ_NAME + " TEXT )";
//        db.execSQL(CREATE_EQ_TABLE);
//    }
//
//    // Upgrading database
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        // Drop older table if existed
//        if (oldVersion == 1 && newVersion == 2) {
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EQ);
//        }
//        if (newVersion == 3) {
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMS);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTISTS);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENRES);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENT);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENTLY_PLAYED);
//            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EQ);
//
//            String CREATE_PLAY_COUNT_TABLE = "CREATE TABLE " + TABLE_PLAY_COUNT + "("
//                    + KEY_SONG_ID + " INTEGER PRIMARY KEY, "
//                    + KEY_PLAY_COUNT + " INTEGER, "
//                    + KEY_PLAY_TIME + " INTEGER )";
//            db.execSQL(CREATE_PLAY_COUNT_TABLE);
//
//            String CREATE_ARTIST_BIO_TABLE = "CREATE TABLE " + TABLE_ARTIST_BIO + "("
//                    + KEY_ARTIST_ID + " INTEGER PRIMARY KEY, "
//                    + KEY_BIO + " TEXT )";
//            db.execSQL(CREATE_ARTIST_BIO_TABLE);
//
//            String CREATE_LYRIC_TABLE = "CREATE TABLE " + TABLE_LYRICS + "("
//                    + KEY_SONG_ID + " INTEGER PRIMARY KEY, "
//                    + KEY_LYRIC + " TEXT )";
//            db.execSQL(CREATE_LYRIC_TABLE);
//        } else if (oldVersion == 3 && newVersion == 4) {
//           db.execSQL("ALTER TABLE " + TABLE_ARTIST_BIO + " ADD COLUMN " + KEY_TAGS);
//        } else if (oldVersion < 4 && newVersion == 5) {
//            db.execSQL("ALTER TABLE " + TABLE_ARTIST_BIO + " ADD COLUMN " + KEY_TAGS);
//            String CREATE_EQ_TABLE = "CREATE TABLE " + TABLE_EQ + "("
//                    + KEY_EQ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                    + KEY_EQ_NAME + " TEXT )";
//            db.execSQL(CREATE_EQ_TABLE);
//        } else if (oldVersion == 4 && newVersion == 5) {
//            String CREATE_EQ_TABLE = "CREATE TABLE " + TABLE_EQ + "("
//                    + KEY_EQ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                    + KEY_EQ_NAME + " TEXT )";
//            db.execSQL(CREATE_EQ_TABLE);
//        }
//    }
//
//
//
//}