package com.udeshcoffee.android.recyclerview;


import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private int startPosition = -1;
    private int endPosition = -1;

    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }

    public interface OnDropListener {
        void onDrop(int fromPosition, int toPosition);
    }

    public interface OnSwipeListener {
        void onSwipe(int position);
    }

    private OnItemMoveListener mItemMoveListener;
    private OnDropListener mOnDropListener;

    @Nullable
    private OnSwipeListener mOnSwipeListener;

    public ItemTouchHelperCallback(OnItemMoveListener onMoveListener, OnDropListener onDropListener, @Nullable OnSwipeListener onSwipeListener) {
        mItemMoveListener = onMoveListener;
        mOnDropListener = onDropListener;
        mOnSwipeListener = onSwipeListener;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return mOnSwipeListener != null;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

        if (startPosition == -1) {
            startPosition = viewHolder.getAdapterPosition();
        }
        endPosition = target.getAdapterPosition();

        mItemMoveListener.onItemMove(viewHolder.getAdapterPosition(), endPosition);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (mOnSwipeListener != null) {
            mOnSwipeListener.onSwipe(viewHolder.getAdapterPosition());
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (startPosition != -1 && endPosition != -1) {
            mOnDropListener.onDrop(startPosition, endPosition);
        }

        startPosition = -1;
        endPosition = -1;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }
}
