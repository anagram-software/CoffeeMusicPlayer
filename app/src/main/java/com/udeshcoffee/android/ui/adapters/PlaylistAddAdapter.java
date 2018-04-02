package com.udeshcoffee.android.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.udeshcoffee.android.R;
import com.udeshcoffee.android.extensions.ArtExtensionsKt;
import com.udeshcoffee.android.interfaces.OnItemSelectListener;
import com.udeshcoffee.android.model.Song;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * Created by Udathari on 5/30/2017.
 */

public class PlaylistAddAdapter extends RecyclerView.Adapter<PlaylistAddAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter,
        Consumer<List<Song>> {
    private List<Song> mDataset;
    private List<Integer> selected;
    private Context context;
    public OnItemSelectListener listener;
    private TextView count;

    @Override
    public void accept(List<Song> songs) {
        this.mDataset = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return Character.toString(mDataset.get(position).getTitle().charAt(0));
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener{
        // each data item is just a string in this case
        ImageView artView;
        TextView titleView;
        TextView artistView;
        ViewHolder(final View itemView) {
            super(itemView);
            artView = (ImageView) itemView.findViewById(R.id.song_art);
            titleView = (TextView) itemView.findViewById(R.id.song_title);
            artistView = (TextView) itemView.findViewById(R.id.song_subtitle);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                boolean isSelected = false;
                for (int i = 0; i < selected.size();i++){
                    if (position == selected.get(i)){
                        isSelected = true;
                    }
                }
                if (isSelected) {
                    listener.onDeselectItem(position);
                    selected.remove((Integer) position);
                    notifyItemChanged(position);
                }else {
                    listener.onSelectItem(position);
                    selected.add(position);
                    notifyItemChanged(position);
                }
            }
        }

    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PlaylistAddAdapter(TextView c) {
        mDataset = new ArrayList<>();
        selected = new ArrayList<>();
        count = c;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PlaylistAddAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View songView = inflater.inflate(R.layout.song, parent, false);

        // Return a new holder instance
        PlaylistAddAdapter.ViewHolder viewHolder = new PlaylistAddAdapter.ViewHolder(songView);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PlaylistAddAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position

        Song currentItem = mDataset.get(position);

        ArtExtensionsKt.loadArtwork(currentItem, context, holder.artView, null);
        holder.titleView.setText(currentItem.getTitle());
        holder.artistView.setText(currentItem.getArtistName());

        boolean isSelected = false;
        for (int i = 0; i < selected.size();i++){
            if (position == selected.get(i)){
                isSelected = true;
            }
        }
        if (isSelected) {
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.artBackground, typedValue, true))
                holder.itemView.setBackgroundColor(typedValue.data);
        } else {
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true))
                holder.itemView.setBackgroundColor(typedValue.data);
        }
        count.setText(String.format("%d selected", selected.size()));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public Song getItem(int pos){
        return mDataset.get(pos);
    }

}