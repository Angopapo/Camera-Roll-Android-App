package us.koller.cameraroll.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class MediaType {

    public static boolean isMedia(String path) {
        return checkImageExtension(path) ||
                checkRAWExtension(path) ||
                checkGifExtension(path) ||
                checkVideoExtension(path);
    }

    public static boolean isMedia_MimeType(Context context, String path) {
        return isImage(context, path) || isVideo(context, path) || isGif(context, path);
    }

    public static String getMimeType(Context context, String path) {
        Uri uri = StorageUtil.getContentUriFromFilePath(context, path);
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            return mimeType;
        }
        //try fileExtension
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        return mimeType;
    }

    //trying to check via mimeType
    public static boolean isImage(Context context, String path) {
        if (path != null) {
            if (path.startsWith("content")) {
                //performance
                String mimeType = getMimeType(context, path);
                if (mimeType != null) {
                    return mimeType.contains("image");
                }
            }
            return checkImageExtension(path);
        }
        return false;
    }

    public static boolean isVideo(Context context, String path) {
        if (path != null) {
            if (path.startsWith("content")) {
                //performance
                String mimeType = getMimeType(context, path);
                if (mimeType != null) {
                    return mimeType.contains("video");
                }
            }
            return checkVideoExtension(path);
        }
        return false;
    }

    public static boolean isGif(Context context, String path) {
        if (path != null) {
            if (path.startsWith("content")) {
                //performance
                String mimeType = getMimeType(context, path);
                if (mimeType != null) {
                    return mimeType.contains("gif");
                }
            }
            return checkGifExtension(path);
        }
        return false;
    }

    public static boolean isRAWImage(Context context, String path) {
        if (path != null) {
            if (path.startsWith("content")) {
                //performance
                String mimeType = getMimeType(context, path);
                if (mimeType != null) {
                    return checkRAWExtension(mimeType);
                }
            }
            return checkRAWExtension(path);
        }
        return false;
    }


    //checking via extension
    private static String[] imageExtensions = {"jpg", "png", "jpe", "jpeg", "bmp"};
    private static String[] videoExtensions = {"mp4", "mkv", "webm", "avi"};
    private static String[] gifExtension = {"gif"};
    private static String[] rawExtension = {"dng"};
    private static String[] exifExtensions = {"jpg", "jpe", "jpeg", "bmp", "dng"};

    public static boolean doesSupportExif(String path) {
        return checkExtension(path, exifExtensions);
    }

    private static boolean checkImageExtension(String path) {
        return checkExtension(path, imageExtensions);
    }

    private static boolean checkVideoExtension(String path) {
        return checkExtension(path, videoExtensions);
    }

    private static boolean checkGifExtension(String path) {
        return checkExtension(path, gifExtension);
    }

    private static boolean checkRAWExtension(String path) {
        return checkExtension(path, rawExtension);
    }

    private static boolean checkExtension(String path, String[] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (path.toLowerCase().endsWith(extensions[i])) {
                return true;
            }
        }
        return false;
    }
}
