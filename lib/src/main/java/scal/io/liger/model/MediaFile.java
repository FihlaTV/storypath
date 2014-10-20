package scal.io.liger.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import scal.io.liger.Constants;

/**
 * Created by mnbogner on 9/29/14.
 */
public class MediaFile {

    private String path;
    private String medium; // mime type?
    private String thumbnailFilePath;

    public MediaFile() {
      // required for JSON/GSON
    }

    public MediaFile(String path, String medium) {
        this.path = path;
        this.medium = medium;

        // check for file existance?
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getThumbnailFilePath() {
        return thumbnailFilePath;
    }

    public Bitmap getThumbnail() { // TODO: disk cache, multiple sizes
        Bitmap thumbnail = null;

        if (thumbnailFilePath == null) {
            if (medium.equals(Constants.VIDEO)) {
                try {
                    //Log.d(" *** TESTING *** ", "CREATING NEW THUMBNAIL FILE FOR " + path);

                    // FIXME should not be stored in the source location, but a cache dir in our app folder on the sd or internal cache if there is no SD
                    // FIXME need to check datestamp on original file to check if our thumbnail is up to date
                    // FIXME this should be run from a background thread as it does disk access  
                    File originalFile = new File(path);
                    String fileName = originalFile.getName();
                    String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
                    thumbnailFilePath = path.substring(0, path.lastIndexOf(File.separator) + 1) + tokens[0] + "_thumbnail.png";
                    File thumbnailFile = new File(thumbnailFilePath);
                    if (thumbnailFile.exists()) {
                        thumbnail = BitmapFactory.decodeFile(thumbnailFilePath);
                    } else {
                        FileOutputStream thumbnailStream = new FileOutputStream(thumbnailFile);

                        thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
                        thumbnail.compress(Bitmap.CompressFormat.PNG, 75, thumbnailStream); // FIXME make compression level configurable
                        thumbnailStream.flush();
                        thumbnailStream.close();
                    }

                    //Log.d(" *** TESTING *** ", "THUMBNAIL FILE SAVED AS " + thumbnailFilePath);
                } catch (IOException ioe) {
                    //Log.d(" *** TESTING *** ", "EXCEPTION: " + ioe.getMessage());
                    return null;
                }
            } else if (medium.equals(Constants.AUDIO)) {
                // TODO create audio thumbnails
            } else if (medium.equals(Constants.PHOTO)) {
                // TODO return the image directly
            } else {
                Log.e(this.getClass().getName(), "can't create thumbnail file for " + path + ", unsupported medium: " + medium);
            }
        } else {
            //Log.d(" *** TESTING *** ", "LOADING THUMBNAIL FILE FOR " + path);
            thumbnail = BitmapFactory.decodeFile(thumbnailFilePath);
            //Log.d(" *** TESTING *** ", "LOADED THUMBNAIL FROM FILE " + thumbnailFilePath);
        }

        return thumbnail;
    }
}
