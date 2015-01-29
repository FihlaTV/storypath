package scal.io.liger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;
import android.widget.ImageView;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import scal.io.liger.view.AudioWaveform;

/**
 * Created by mnbogner on 7/14/14.
 */
public class MediaHelper {

    private static final String TAG = "MediaHelper";
    private static final String LIGER_DIR = "Liger";
    private static final boolean VERBOSE = false;
    private static File selectedFile = null;
    private static ArrayList<File> fileList = null;
    private static String sdLigerFilePath = null;

    /** Use @MediaType annotation to limit String argument to one of
     * Constants.VIDEO / AUDIO / PHOTO
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({Constants.VIDEO, Constants.AUDIO, Constants.PHOTO})
    public @interface MediaType {}

    public static interface ThumbnailCallback {
        public void newThumbnailGenerated(File thumbnail);
    }

    public static File loadFileFromPath(String filePath) {

        // assume initial / indicates a non-relative path
        // (a relative path starting with / will break the code anyway)
        if (filePath.startsWith("/")) {
            File mediaFile = new File(filePath);
            return mediaFile;
        }

        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            System.out.println("GOT FILE: " + sdLigerFilePath + filePath);
            File mediaFile = new File(sdLigerFilePath + filePath);
            return mediaFile;
        } else {
            System.err.println("SD CARD NOT FOUND");
        }

        return null;
    }

    public static File loadFile() {
        if(null == selectedFile) {
            return null;
        }

        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            return selectedFile;
        } else {
            System.err.println("SD CARD NOT FOUND");
        }

        return null;
    }

    public static void setupFileStructure(Context context) {
        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            String sdCardFolderPath = Environment.getExternalStorageDirectory().getPath();
            sdLigerFilePath = sdCardFolderPath + File.separator + LIGER_DIR + File.separator;
        } else {
            System.err.println("SD CARD NOT FOUND");
        }
    }

    public static String[] getMediaFileList() {
        //ensure path has been set
        if(null == sdLigerFilePath) {
            return null;
        }

        ArrayList<String> fileNamesList = new ArrayList<String>();
        fileList = new ArrayList<File>();

        // revisit this, perhaps take media type and return corresponding files?
        File ligerDir = new File(sdLigerFilePath);
        if (ligerDir != null) {
            for (File file : ligerDir.listFiles()) {
                if (file.getName().endsWith(".mp4")) {
                    fileNamesList.add(file.getName());
                    fileList.add(file);
                }
            }
        }

        File defaultLigerDir = new File(sdLigerFilePath + "/default/");
        if (defaultLigerDir != null) {
            for (File file : defaultLigerDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    fileNamesList.add(file.getName());
                    fileList.add(file);
                }
            }
        }

        return fileNamesList.toArray(new String[fileNamesList.size()]);
    }

    public static void setSelectedFile(int index) {
        selectedFile = fileList.get(index);
    }

    /**
     * Return a directory where audio recordings will be stored
     * @throws IOException
     * TODO Allow this to be configured by Liger client application
     */
    public static @Nullable File getAudioDirectory() throws IOException {
        File storageDirectory = new File(sdLigerFilePath, "audio");
        recursiveCreateDirectory(storageDirectory);

        return storageDirectory;
    }

    /**
     * Return a directory where media thumbnails will be stored
     * @throws IOException
     */
    public static @Nullable File getThumbnailDirectory() throws IOException {
        File thumbDirectory = new File(sdLigerFilePath, "thumbs");
        recursiveCreateDirectory(thumbDirectory);

        return thumbDirectory;
    }

    /**
     * Asynchronously load a a media file or content provider URI into an ImageView,
     * creating and saving the thumbnail if necessary.
     *
     * Development Note:
     * So long as Content Provider Uris are translatable to File addresses
     * let's maintain only the file-based thumnbail generation logic.
     * When the time comes that we have to deal with Streams, convert
     * {@link #displayFileThumbnail(String, java.io.File, android.widget.ImageView, scal.io.liger.MediaHelper.ThumbnailCallback)}
     * to take an InputStream, instead of File, argument.
     *
     * Safe to call from the UI thread.
     */
    public static void displayMediaThumbnail(@NonNull @MediaType final String mediaType,
                                             @NonNull String path,
                                             @NonNull ImageView target,
                                             @Nullable ThumbnailCallback callback) {

        String filePath = null;

        if (path.contains("://") && !path.contains("file://")) {
            // path is a ContentProvider URI
            Uri uri         = Uri.parse(path);
            String mimeType = FileUtils.getMimeType(target.getContext(), uri);
            filePath        = FileUtils.getPath(target.getContext(), uri);
            if (VERBOSE) Log.d(TAG, String.format("media uri mime type %s path %s", mimeType, filePath));
            // WARNING .mp4 audio files report mimetype video
            if (!mimeType.contains("image") &&
                !mimeType.contains("video") &&
                !mimeType.contains("audio"))
                    Log.w(TAG, "Cannot display thumbnail. Unknown content url type " + path);
        } else {
            // path is a file path
            filePath = path.replace("file://", "");
        }

        if (filePath != null) {
            File mediaFile = new File(filePath);
            if (mediaFile.exists())
                displayFileThumbnail(mediaType, mediaFile, target, callback);
            else
                Log.w(TAG, "path appears to be a file, but it cannot be found on disk " + filePath);
        }
    }

    /**
     * Asynchronously load a media file thumbnail into an ImageView, creating the thumbnail if necessary.
     * Safe to call from UI thread.
     */
    public static void displayFileThumbnail(@NonNull @MediaType final String mediaType,
                                            @NonNull final File media,
                                            @NonNull final ImageView target,
                                            @Nullable final ThumbnailCallback callback) {
        try {
            File thumbnailFile = getThumbnailFileForMediaFile(media);
            if (!thumbnailFile.exists()) {
                // If we're going to have to generate the thumbnail
                // set a loading indicator
                displayLoadingIndicator(mediaType, target);
            } else {
                // Thumbnail is already available. Show it
                Picasso.with(target.getContext()).load(thumbnailFile).into(target);
                return;
            }
        } catch (IOException e) {
            displayLoadingIndicator(mediaType, target);
            e.printStackTrace();
        }

        // Asynchronously generate thumbnail
        new AsyncTask<Void, Void, File>() {

            WeakReference<ImageView> weakView = new WeakReference<>(target);

            @Override
            protected File doInBackground(Void... params) {
                ImageView target = weakView.get();
                if (target != null) {
                    try {
                        return generateThumbnail(target.getContext(), media, mediaType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(File result) {
                ImageView target = weakView.get();
                if (result != null) {
                    if (target != null)
                        Picasso.with(target.getContext()).load(result).into(target);

                    if (callback != null)
                        callback.newThumbnailGenerated(result);
                }
            }
        }.execute();
    }

    /**
     * Get or create a waveform bitmap for the given audio file. Should be called on a background thread
     * @throws IOException
     */
    private static File getWaveformForAudioFile(@NonNull Context context, @NonNull File audio) throws IOException {
        File waveFormFile = getThumbnailFileForMediaFile(audio);
        if (!waveFormFile.exists()) {
            Bitmap waveform = AudioWaveform.createBitmap(context, audio.getAbsolutePath());

            if (waveform == null) {
                Log.e(TAG, "Failed to create audio waveform");
                return null;
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(waveFormFile);
                waveform.compress(Bitmap.CompressFormat.PNG, 100 /* No effect with PNG */, out);
                Log.d(TAG, "Generated waveform thumb " + waveFormFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return waveFormFile;
    }

    private static void recursiveCreateDirectory(@NonNull File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            // Directory does not exist and could not be created
            throw new IOException("Unable to create " + directory.getAbsolutePath());
        }
    }

    public static File getThumbnailFileForMediaFile(@NonNull File media) throws IOException {
        return new File(getThumbnailDirectory(), media.getName() + ".thumb");
    }

    public static File getThumbnailFileForMediaUri(@NonNull Uri media) throws IOException {
        // Convert last Uri path segmenet to file name, allowing only alphanumeric and underscores.
        return new File(getThumbnailDirectory(), media.getLastPathSegment().replaceAll("\\W+", "") + ".thumb");
    }

    /**
     * Display an appropriate loading image for the given media type
     */
    private static void displayLoadingIndicator(@NonNull @MediaType String mediaType,
                                                @NonNull ImageView target) {
        switch (mediaType) {
            case Constants.AUDIO:
                target.setImageResource(R.drawable.waveform_loading);
                break;
            case Constants.VIDEO:
            case Constants.PHOTO:
                target.setImageResource(R.drawable.media_loading);
                break;
        }
    }

    /**
     * Generate a thumbnanil for the given media file.
     * Must be called from a background thread.
     * TODO Allow custom sizes
     */
    private static @Nullable File generateThumbnail(@NonNull Context context,
                                                    @NonNull File media,
                                                    @NonNull @MediaType String mediaType)
                                                    throws IOException {

        Bitmap thumbnail = null;

        switch (mediaType) {
            case Constants.AUDIO:
                return getWaveformForAudioFile(context, media);
            case Constants.VIDEO:
                thumbnail = ThumbnailUtils.createVideoThumbnail(media.getAbsolutePath(),
                                                                MediaStore.Images.Thumbnails.MINI_KIND);
                break;
            case Constants.PHOTO:
                thumbnail = decodeSampledBitmapFromResource(media, 640, 480);
                break;
        }

        if (thumbnail != null) {
            File thumbnailFile = getThumbnailFileForMediaFile(media);
            FileOutputStream thumbnailStream = new FileOutputStream(thumbnailFile);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, thumbnailStream); // FIXME make compression level configurable
            thumbnailStream.flush();
            thumbnailStream.close();
            Log.d(TAG, "Generated thumbnail at " + thumbnailFile.getAbsolutePath());
            return thumbnailFile;
        } else {
            Log.w(TAG, "Unable to generate thumbnail for " + media.getAbsolutePath());
        }
        return null;
    }

    private static @Nullable Bitmap decodeSampledBitmapFromResource(File media,
                                                         int reqWidth,
                                                         int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        // this does not allocate any memory for image data
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(media.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(media.getAbsolutePath(), options);
    }

    /**
     * Calculating a scaling factor for loading a downsampled
     * Bitmap to be at least (reWidth x reqHeight)
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}