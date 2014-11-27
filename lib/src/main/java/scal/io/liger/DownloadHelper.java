package scal.io.liger;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import scal.io.liger.model.ExpansionIndexItem;

/**
 * Created by mnbogner on 11/6/14.
 */
public class DownloadHelper {

    // TODO use HTTPS
    // TODO pickup Tor settings
    public static boolean checkExpansionFiles(Context context, String mainOrPatch, int version) {
        String expansionFilePath = ZipHelper.getExpansionFileFolder(context, mainOrPatch, version);

        if (expansionFilePath != null) {
            Log.d("DOWNLOAD", "EXPANSION FILE " + ZipHelper.getExpansionZipFilename(context, mainOrPatch, version) + " FOUND IN " + expansionFilePath);
            return true;
        } else {
            Log.d("DOWNLOAD", "EXPANSION FILE " + ZipHelper.getExpansionZipFilename(context, mainOrPatch, version) + " NOT FOUND");
            return false;
        }
    }

    // for additional expansion files, check files folder for specified file
    public static boolean checkExpansionFiles(Context context, String fileName) {
        String expansionFilePath = ZipHelper.getExpansionFileFolder(context, fileName);

        if (expansionFilePath != null) {
            Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " FOUND IN " + expansionFilePath);
            return true;
        } else {
            Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " NOT FOUND");
            return false;
        }
    }

    public static void checkAndDownload(Context context) {

        if (checkExpansionFiles(context, Constants.MAIN, Constants.MAIN_VERSION)) {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE FOUND (NO DOWNLOAD)");
        } else {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE NOT FOUND (DOWNLOADING)");

            final LigerDownloadManager mainDownload = new LigerDownloadManager(Constants.MAIN, Constants.MAIN_VERSION, context, true);
            Thread mainDownloadThread = new Thread(mainDownload);

            Toast.makeText(context, "Starting download of content pack.", Toast.LENGTH_LONG).show(); // FIXME move to strings

            mainDownloadThread.start();
        }

        if (Constants.PATCH_VERSION > 0) {
            if (checkExpansionFiles(context, Constants.PATCH, Constants.PATCH_VERSION)) {
                Log.d("DOWNLOAD", "PATCH EXPANSION FILE FOUND (NO DOWNLOAD)");
            } else {
                Log.d("DOWNLOAD", "PATCH EXPANSION FILE NOT FOUND (DOWNLOADING)");

                final LigerDownloadManager patchDownload = new LigerDownloadManager(Constants.PATCH, Constants.PATCH_VERSION, context, true);
                Thread patchDownloadThread = new Thread(patchDownload);

                Toast.makeText(context, "Starting download of patch for content pack.", Toast.LENGTH_LONG).show(); // FIXME move to strings

                patchDownloadThread.start();
            }
        }

        HashMap<String, ExpansionIndexItem> expansionIndex = IndexManager.loadInstalledFileIndex(context);

        for (String fileName : expansionIndex.keySet()) {
            if (checkExpansionFiles(context, fileName)) {
                Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " FOUND (NO DOWNLOAD)");
            } else {
                Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " NOT FOUND (DOWNLOADING)");

                final LigerAltDownloadManager expansionDownload = new LigerAltDownloadManager(fileName, context, true);
                Thread expansionDownloadThread = new Thread(expansionDownload);

                Toast.makeText(context, "Starting download of expansion file.", Toast.LENGTH_LONG).show(); // FIXME move to strings

                expansionDownloadThread.start();
            }
        }
    }
}
