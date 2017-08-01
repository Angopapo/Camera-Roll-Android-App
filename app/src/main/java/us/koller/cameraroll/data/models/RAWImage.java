package us.koller.cameraroll.data.models;

import android.content.Context;
import android.os.Parcel;

import us.koller.cameraroll.util.Util;

public class RAWImage extends Photo {

    RAWImage() {

    }

    RAWImage(Parcel parcel) {
        super(parcel);
    }

    @Override
    public int[] retrieveImageDimens(Context context) {
        return Util.getImageDimensions(context, getUri(context));
    }

    @Override
    public String getType() {
        return "RAW Image";
    }
}
