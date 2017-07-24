package us.koller.cameraroll.data.provider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.provider.retriever.MediaStoreRetriever;
import us.koller.cameraroll.data.provider.retriever.StorageRetriever;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.util.SortUtil;

public class MediaProvider extends Provider {

    private static ArrayList<Album> albums;

    private static final int MODE_STORAGE = 1;
    private static final int MODE_MEDIASTORE = 2;

    public static final String FILE_TYPE_NO_MEDIA = ".nomedia";
    public static final int PERMISSION_REQUEST_CODE = 16;

    public abstract static class Callback implements Provider.Callback {
        public abstract void onMediaLoaded(ArrayList<Album> albums);
    }

    public MediaProvider(Context context) {
        super(context);
    }

    public static boolean checkPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (read != PackageManager.PERMISSION_GRANTED && write != PackageManager.PERMISSION_GRANTED) {
                String[] requestedPermissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(context, requestedPermissions, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    public void loadAlbums(final Activity context,
                           final boolean hiddenFolders,
                           Callback callback) {

        if (!MediaProvider.checkPermission(context)) {
            callback.needPermission();
            return;
        }

        int mode = getMode(context);

        switch (mode) {
            case MODE_STORAGE:
                retriever = new StorageRetriever();
                break;
            case MODE_MEDIASTORE:
                retriever = new MediaStoreRetriever();
                break;
        }

        if (retriever != null) {
            setCallback(callback);

            retriever.loadAlbums(context, hiddenFolders,
                    new Callback() {
                        @Override
                        public void onMediaLoaded(ArrayList<Album> albums) {
                            if (!hiddenFolders) {
                                //remove excluded albums
                                for (int i = albums.size() - 1; i >= 0; i--) {
                                    if (albums.get(i).excluded) {
                                        albums.remove(i);
                                    }
                                }
                            }

                            SortUtil.sortAlbums(context, albums);

                            setAlbums(albums);
                            Callback callback = getCallback();
                            if (callback != null) {
                                callback.onMediaLoaded(albums);
                            }
                        }

                        @Override
                        public void timeout() {
                            Callback callback = getCallback();
                            if (callback != null) {
                                callback.timeout();
                            }
                        }

                        @Override
                        public void needPermission() {
                            Callback callback = getCallback();
                            if (callback != null) {
                                callback.needPermission();
                            }
                        }
                    });
        } else {
            if (callback != null) {
                callback.onMediaLoaded(null);
            }
        }
    }

    private static void setAlbums(ArrayList<Album> albums) {
        MediaProvider.albums = albums;
    }

    public static ArrayList<Album> getAlbums() {
        return albums;
    }

    public static Album loadAlbum(String path) {
        if (albums == null) {
            return getErrorAlbum();
        }

        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getPath().equals(path)) {
                return albums.get(i);
            }
        }

        return getErrorAlbum();
    }

    public static Album getErrorAlbum() {
        //Error album
        Album album = new Album().setPath("ERROR");
        album.getAlbumItems().add(AlbumItem.getErrorItem());
        return album;
    }

    private static int getMode(Context context) {
        return Settings.getInstance(context).useStorageRetriever() ?
                MODE_STORAGE : MODE_MEDIASTORE;
    }
}