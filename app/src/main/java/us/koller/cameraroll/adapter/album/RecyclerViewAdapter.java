package us.koller.cameraroll.adapter.album;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.adapter.album.ViewHolder.AlbumItemHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.GifViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.PhotoViewHolder;
import us.koller.cameraroll.adapter.album.ViewHolder.VideoViewHolder;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Gif;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.ui.ItemActivity;
import us.koller.cameraroll.ui.MainActivity;

public class RecyclerViewAdapter extends RecyclerView.Adapter {

    @SuppressWarnings("FieldCanBeLocal")
    private final int VIEW_TYPE_PHOTO = 1;
    private final int VIEW_TYPE_GIF = 2;
    private final int VIEW_TYPE_VIDEO = 3;

    private Album album;

    private boolean pick_photos;

    private SelectorModeManager manager;

    private DragSelectTouchListener dragSelectTouchListener;

    public RecyclerViewAdapter(SelectorModeManager.Callback callback, final RecyclerView recyclerView,
                               final Album album, boolean pick_photos) {
        this.album = album;
        this.pick_photos = pick_photos;

        manager = new SelectorModeManager();
        manager.addCallback(callback);

        if (pick_photos) {
            setSelectorMode(true);
            if (callback != null) {
                callback.onSelectorModeEnter();
            }
        }

        //disable default change animation
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        if (callback != null && dragSelectEnabled()) {
            dragSelectTouchListener = new DragSelectTouchListener()
                    .withSelectListener(new DragSelectTouchListener.OnDragSelectListener() {
                        @Override
                        public void onSelectChange(int start, int end, boolean isSelected) {
                            for (int i = start; i <= end; i++) {
                                manager.onItemSelect(album.getAlbumItems().get(i).getPath());
                                //update ViewHolder
                                notifyItemChanged(i);
                            }
                        }
                    });
            recyclerView.addOnItemTouchListener(dragSelectTouchListener);
        }
    }

    public void setSelectorModeManager(SelectorModeManager manager) {
        this.manager = manager;
    }

    @Override
    public int getItemViewType(int position) {
        AlbumItem albumItem = album.getAlbumItems().get(position);
        if (albumItem instanceof Photo) {
            return VIEW_TYPE_PHOTO;
        } else if (albumItem instanceof Gif) {
            return VIEW_TYPE_GIF;
        } else {
            return VIEW_TYPE_VIDEO;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.albumitem_cover, parent, false);
        switch (viewType) {
            case VIEW_TYPE_VIDEO:
                return new VideoViewHolder(v);
            case VIEW_TYPE_GIF:
                return new GifViewHolder(v);
            default:
                return new PhotoViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final AlbumItem albumItem = album.getAlbumItems().get(position);

        if (!albumItem.equals(((AlbumItemHolder) holder).getAlbumItem())) {
            ((AlbumItemHolder) holder).setAlbumItem(albumItem);
        }

        boolean selected = manager.isItemSelected(albumItem.getPath());

        ((AlbumItemHolder) holder).setSelected(selected);

        holder.itemView.setTag(albumItem.getPath());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSelectorMode()) {
                    onItemSelected((AlbumItemHolder) holder);
                } else {
                    Intent intent = new Intent(holder.itemView.getContext(), ItemActivity.class);
                    intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
                    //intent.putExtra(ItemActivity.ALBUM, getAlbum());
                    intent.putExtra(ItemActivity.ALBUM_PATH, album.getPath());
                    intent.putExtra(ItemActivity.ITEM_POSITION,
                            album.getAlbumItems().indexOf(albumItem));

                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    (Activity) holder.itemView.getContext(),
                                    holder.itemView.findViewById(R.id.image),
                                    albumItem.getPath());
                    ((Activity) holder.itemView.getContext())
                            .startActivityForResult(intent,
                                    MainActivity.REFRESH_PHOTOS_REQUEST_CODE, options.toBundle());
                }
            }
        });

        if (getManager().callbacksAttached()) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!getSelectorMode()) {
                        setSelectorMode(true);
                        clearSelectedItemsList();
                    }

                    onItemSelected((AlbumItemHolder) holder);

                    if (dragSelectEnabled()) {
                        //notify DragSelectTouchListener
                        boolean selected = manager.isItemSelected(albumItem.getPath());
                        if (selected) {
                            int position = getAlbum().getAlbumItems().indexOf(albumItem);
                            dragSelectTouchListener.startDragSelection(position);
                        }
                    }
                    return true;
                }
            });
        }
    }

    public boolean isSelectorModeActive() {
        return getSelectorMode() && !pick_photos;
    }

    public void restoreSelectedItems() {
        //notify AlbumActivity
        getManager().onSelectorModeEnter();

        for (int i = 0; i < this.album.getAlbumItems().size(); i++) {
            if (manager.isItemSelected(album.getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }

        getManager().onItemSelected(manager.getSelectedItemCount());
    }

    private void checkForNoSelectedItems() {
        if (getSelectedItemCount() == 0 && !pick_photos) {
            cancelSelectorMode(null);
        }
    }

    private int getSelectedItemCount() {
        return manager.getSelectedItemCount();
    }

    private void onItemSelected(AlbumItemHolder holder) {
        boolean selected = manager.onItemSelect(holder.albumItem.getPath());
        holder.setSelected(selected);
        checkForNoSelectedItems();
    }

    public String[] cancelSelectorMode(Activity context) {
        setSelectorMode(false);
        //update ui
        for (int i = 0; i < this.album.getAlbumItems().size(); i++) {
            if (manager.isItemSelected(album.getAlbumItems().get(i).getPath())) {
                notifyItemChanged(i);
            }
        }
        //generate paths array
        String[] paths;
        if (context != null) {
            paths = manager.createStringArray(context);
        } else {
            paths = null;
        }
        //clear manager list
        clearSelectedItemsList();
        return paths;
    }

    public boolean onBackPressed() {
        if (getSelectorMode() && !pick_photos) {
            cancelSelectorMode(null);
            return true;
        }
        return false;
    }

    private boolean getSelectorMode() {
        return manager.isSelectorModeActive();
    }

    private void setSelectorMode(boolean activate) {
        manager.setSelectorMode(activate);
    }

    public boolean dragSelectEnabled() {
        return true;
    }

    private void clearSelectedItemsList() {
        manager.clearList();
    }

    @Override
    public int getItemCount() {
        return getAlbum().getAlbumItems().size();
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    private SelectorModeManager getManager() {
        return manager;
    }

    public void saveInstanceState(Bundle state) {
        manager.saveInstanceState(state);
    }
}
