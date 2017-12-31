package com.udeshcoffee.android.ui.main.detail.playlistdetail;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.udeshcoffee.android.R;
import com.udeshcoffee.android.data.MediaRepository;
import com.udeshcoffee.android.ui.adapters.PlaylistAddAdapter;
import com.udeshcoffee.android.utils.Injection;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Udathari on 2/23/2017.
 */

public class PlaylistItemAddActivity extends AppCompatActivity {

    private MediaRepository mediaRepository;
    private PlaylistAddAdapter addAdapter;
    private Long playlistId;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaRepository = Injection.INSTANCE.provideMediaRepository(getApplicationContext());
        setContentView(R.layout.activity_playlist_item_add);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        playlistId = intent.getLongExtra("id", 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.playlistitemadd_actionbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("Add to " + title);

        TextView count = (TextView) findViewById(R.id.playlistitemadd_count);

        FastScrollRecyclerView songView = (FastScrollRecyclerView) findViewById(R.id.playlistitemadd_list);
        //songView.setNestedScrollingEnabled(false);
        addAdapter = new PlaylistAddAdapter(count);

        songView.setLayoutManager(new LinearLayoutManager(this));
        songView.setAdapter(addAdapter);
        addAdapter.setOnSelectListener(new PlaylistAddAdapter.OnItemSelectListener() {
            @Override
            public void onSelectItem(int postion) {
                addToPlaylist(addAdapter.getItem(postion).getId());
            }

            @Override
            public void onDeselectItem(int postion) {
                deletePlaylistTracks(addAdapter.getItem(postion).getId());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        disposable = mediaRepository.getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        this.finish();
        return true;
    }

    private Toast addDeleteToast;

    public void addToPlaylist(long audioId) {
        ContentResolver resolver = this.getContentResolver();
        String[] cols = new String[] {
                "count(*)"
        };
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        int base = 0;
        if (cur != null) {
            cur.moveToFirst();
            base = cur.getInt(0);
            cur.close();
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base);
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
        resolver.insert(uri, values);
        if (addDeleteToast != null)
            addDeleteToast.cancel();
        addDeleteToast = Toast.makeText(this, "Added to playlist", Toast.LENGTH_SHORT);
        addDeleteToast.show();
    }

    public void deletePlaylistTracks(long audioId) {
        ContentResolver resolver = this.getContentResolver();
        try {
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external", playlistId);
            String where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?" ; // my mistake was I used .AUDIO_ID here

            String audioId1 = Long.toString(audioId);
            String[] whereVal = { audioId1 };
            resolver.delete(uri, where,whereVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (addDeleteToast != null)
            addDeleteToast.cancel();
        addDeleteToast = Toast.makeText(this, "Removed from playlist", Toast.LENGTH_SHORT);
        addDeleteToast.show();
    }
}
