package us.koller.cameraroll.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.SortUtil;
import us.koller.cameraroll.util.StorageUtil;

public abstract class AlbumItem
        implements Parcelable, SortUtil.Sortable {

    private static final int PHOTO = 1;
    private static final int GIF = 2;
    private static final int VIDEO = 3;
    private static final int RAW = 4;

    private String name;
    private String path;
    private long dateTaken;
    private int[] imageDimens;

    public boolean error = false;
    public boolean contentUri = false;
    public boolean isSharedElement = false;
    public boolean hasFadedIn = false;

    //factory method
    public static AlbumItem getInstance(final Context context, String path) {
        if (path == null) {
            return null;
        }

        AlbumItem albumItem = null;
        if (MediaType.isGif(context, path)) {
            albumItem = new Gif();
        } else if (MediaType.isRAWImage(context, path)) {
            albumItem = new RAWImage();
        } else if (MediaType.isImage(context, path)) {
            albumItem = new Photo();
        } else if (MediaType.isVideo(context, path)) {
            albumItem = new Video();
        }

        if (albumItem != null) {
            albumItem.setPath(path)
                    .setName(new File(path).getName());

            if (path.startsWith("content")) {
                albumItem.contentUri = true;
            }
        }

        return albumItem;
    }

    AlbumItem() {
        name = "";
        path = "";
        dateTaken = -1;
    }

    public AlbumItem setName(String name) {
        this.name = name;
        return this;
    }

    private AlbumItem setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setDate(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    @Override
    public long getDate() {
        if (dateTaken != -1) {
            return dateTaken;
        }

        return new File(getPath()).lastModified();
    }

    public long getDateTaken() {
        return dateTaken;
    }

    @Override
    public boolean pinned() {
        return false;
    }

    public Uri getUri(Context context) {
        if (!contentUri) {
            //my file provider isn't working with Google Photos ?!
            /*try {
                File file = new File(getPath());
                return FileProvider.getUriForFile(context,
                        context.getApplicationContext().getPackageName() + ".provider", file);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                //file is probably on removable storage
                return StorageUtil
                       .getContentUriFromFilePath(context, getPath());
            }*/

            return StorageUtil
                    .getContentUriFromFilePath(context, getPath());
        }
        return Uri.parse(getPath());
    }

    public int[] getImageDimens(Context context) {
        if (imageDimens == null) {
            imageDimens = retrieveImageDimens(context);
        }
        return new int[]{this.imageDimens[0], this.imageDimens[1]};
    }

    abstract int[] retrieveImageDimens(Context context);

    AlbumItem(Parcel parcel) {
        this.name = parcel.readString();
        this.path = parcel.readString();
        this.error = Boolean.parseBoolean(parcel.readString());
        this.contentUri = Boolean.parseBoolean(parcel.readString());
    }

    @Override
    public String toString() {
        return getName() + ", " + getPath();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        int k;
        if (this instanceof RAWImage) {
            k = RAW;
        } else if (this instanceof Gif) {
            k = GIF;
        } else if (this instanceof Video) {
            k = VIDEO;
        } else {
            k = PHOTO;
        }
        parcel.writeInt(k);
        parcel.writeString(name);
        parcel.writeString(path);
        parcel.writeString(String.valueOf(error));
        parcel.writeString(String.valueOf(contentUri));
    }

    @SuppressWarnings("WeakerAccess")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public AlbumItem createFromParcel(Parcel parcel) {
            switch (parcel.readInt()) {
                case VIDEO:
                    return new Video(parcel);
                case GIF:
                    return new Gif(parcel);
                case RAW:
                    return new RAWImage(parcel);
                default:
                    return new Photo(parcel);
            }
        }

        public AlbumItem[] newArray(int i) {
            return new AlbumItem[i];
        }
    };

    public static AlbumItem getErrorItem() {
        AlbumItem albumItem = new Photo();
        albumItem.setPath("ERROR").setName("ERROR");
        return albumItem;
    }

    public abstract String getType();
}
