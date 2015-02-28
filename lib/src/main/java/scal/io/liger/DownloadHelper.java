package scal.io.liger;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import scal.io.liger.model.ContentPackMetadata;
import scal.io.liger.model.ExpansionIndexItem;

/**
 * Created by mnbogner on 11/6/14.
 */
public class DownloadHelper {

    private static final Object waitObj = new Object();

    public static boolean checkAllFiles(Context context) {

        ArrayList<String> missingFiles = new ArrayList<String>();

        if (ZipHelper.getExpansionFileFolder(context, Constants.MAIN, Constants.MAIN_VERSION) == null) {
            missingFiles.add(ZipHelper.getExpansionZipFilename(context, Constants.MAIN, Constants.MAIN_VERSION));
        }

        // only check for patch file if version is newer than main file version
        if ((Constants.PATCH_VERSION > 0) && (Constants.PATCH_VERSION >= Constants.MAIN_VERSION)) {
            if (ZipHelper.getExpansionFileFolder(context, Constants.PATCH, Constants.PATCH_VERSION) == null) {
                missingFiles.add(ZipHelper.getExpansionZipFilename(context, Constants.PATCH, Constants.PATCH_VERSION));
            }
        }

        HashMap<String, ExpansionIndexItem> installedPacksMap = IndexManager.loadInstalledIdIndex(context);
        HashMap<String, ExpansionIndexItem> availablePacksMap = IndexManager.loadAvailableIdIndex(context);

        HashMap<String, ExpansionIndexItem> updatedPacksMap = new HashMap<String, ExpansionIndexItem>();
        ExpansionIndexItem tempContentPack = null;
        boolean updateFlag = false;

        for (ExpansionIndexItem contentPack : installedPacksMap.values()) {

            // while checking, update values
            tempContentPack = fixStats(contentPack, availablePacksMap.get(contentPack.getExpansionId()));
            if (tempContentPack == null) {
                updatedPacksMap.put(contentPack.getExpansionId(), contentPack);
            } else {
                updatedPacksMap.put(tempContentPack.getExpansionId(), tempContentPack);
                updateFlag = true;
            }

            File contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.MAIN));

            if (!contentPackFile.exists()) {
                missingFiles.add(IndexManager.buildFileName(contentPack, Constants.MAIN));
            }

            if (!IndexManager.buildFileName(contentPack, Constants.PATCH).equals(IndexManager.noPatchFile)) {
                contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.PATCH));

                if (!contentPackFile.exists()) {
                    missingFiles.add(IndexManager.buildFileName(contentPack, Constants.PATCH));
                }
            }
        }

        if (updateFlag) {

            // persist updated index
            IndexManager.saveInstalledIndex(context, updatedPacksMap);

            // need a better solution
            try {
                synchronized (waitObj) {
                    Log.d("WAITING", "PERSISTING INDEX WITH UPDATED VALUES");
                    waitObj.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (missingFiles.isEmpty()) {
            return true;
        } else {
            Log.e("CHECKING FILES", "THE FOLLOWING EXPECTED EXPANSION FILES ARE MISSING: " + missingFiles.toString());
            return false;
        }
    }

    public static float getDownloadProgress(Context context) {

        long totalExpectedSize = 0;
        long totalCurrentSize = 0;
        boolean sizeUndefined = false;

        // omitting main and patch files for now

        HashMap<String, ExpansionIndexItem> contentPacksMap = IndexManager.loadInstalledIdIndex(context);

        for (ExpansionIndexItem contentPack : contentPacksMap.values()) {
            File contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.MAIN));

            if (contentPack.getExpansionFileSize() == 0) {
                // no size defined, can't evaluate
                sizeUndefined = true;
            } else {
                totalExpectedSize = totalExpectedSize + contentPack.getExpansionFileSize();

                if (!contentPackFile.exists()) {
                    // actual file doesn't exist, check for temp file
                    contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.MAIN) + ".tmp");

                    if (!contentPackFile.exists()) {
                        // still no file, add nothing to current size
                    } else {
                        totalCurrentSize = totalCurrentSize + contentPackFile.length();
                    }
                } else {
                    totalCurrentSize = totalCurrentSize + contentPackFile.length();
                }

                if (!IndexManager.buildFileName(contentPack, Constants.PATCH).equals(IndexManager.noPatchFile)) {
                    contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.PATCH));

                    if (contentPack.getPatchFileSize() == 0) {
                        // no size defined, can't evaluate
                        sizeUndefined = true;
                    } else {
                        totalExpectedSize = totalExpectedSize + contentPack.getPatchFileSize();

                        if (!contentPackFile.exists()) {
                            // actual file doesn't exist, check for temp file
                            contentPackFile = new File(IndexManager.buildFilePath(contentPack) + IndexManager.buildFileName(contentPack, Constants.PATCH) + ".tmp");

                            if (!contentPackFile.exists()) {
                                // still no file, add nothing to current size
                            } else {
                                totalCurrentSize = totalCurrentSize + contentPackFile.length();
                            }
                        } else {
                            totalCurrentSize = totalCurrentSize + contentPackFile.length();
                        }
                    }
                }
            }
        }

        if (sizeUndefined) {
            return -1;
        } else if (totalExpectedSize == 0) {
            Log.e("CHECKING FILES", "TOTAL EXPECTED SIZE IS 0 BYTES (NO CURRENT DOWNLOADS?)");
            return -1;
        } else {
            Log.e("CHECKING FILES", "CURRENT DOWNLOAD PROGRESS: " + totalCurrentSize + " BYTES OUT OF " + totalExpectedSize + " BYTES");
            return (float)totalCurrentSize / (float)totalExpectedSize;
        }
    }

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

    public static boolean checkExpansionFiles(Context context, String fileName, ExpansionIndexItem item) {
        String expansionFilePath = ZipHelper.getExpansionFileFolder(context, fileName, item);

        if (expansionFilePath != null) {
            Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " FOUND IN " + expansionFilePath);
            return true;
        } else {
            Log.d("DOWNLOAD", "EXPANSION FILE " + fileName + " NOT FOUND");
            return false;
        }
    }

    public static void checkAndDownload(Context context) {

        // needs to be revised to deal with queue file

        if (checkExpansionFiles(context, Constants.MAIN, Constants.MAIN_VERSION)) {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE FOUND (NO DOWNLOAD)");
        } else {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE NOT FOUND (DOWNLOADING)");

            final LigerDownloadManager mainDownload = new LigerDownloadManager(Constants.MAIN, Constants.MAIN_VERSION, context, true);
            Thread mainDownloadThread = new Thread(mainDownload);

            Toast.makeText(context, "Starting download of content pack.", Toast.LENGTH_LONG).show(); // FIXME move to strings

            mainDownloadThread.start();

            // need a better solution
            try {
                synchronized (waitObj) {
                    Log.d("WAITING", Constants.MAIN + " "  + Constants.MAIN_VERSION);
                    waitObj.wait(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (Constants.PATCH_VERSION > 0) {

            // if the main file is newer than the patch file, remove the patch file rather than downloading
            if (Constants.PATCH_VERSION < Constants.MAIN_VERSION) {
                File obbDirectory = new File(ZipHelper.getObbFolderName(context));
                File fileDirectory = new File(ZipHelper.getFileFolderName(context));

                String nameFilter = Constants.PATCH + ".*." + context.getPackageName() + ".obb";

                Log.d("DOWNLOAD", "CLEANUP: DELETING " + nameFilter + " FROM " + obbDirectory.getPath());

                WildcardFileFilter obbFileFilter = new WildcardFileFilter(nameFilter);
                for (File obbFile : FileUtils.listFiles(obbDirectory, obbFileFilter, null)) {
                    Log.d("DOWNLOAD", "CLEANUP: FOUND " + obbFile.getPath() + ", DELETING");
                    FileUtils.deleteQuietly(obbFile);
                }

                Log.d("DOWNLOAD", "CLEANUP: DELETING " + nameFilter + " FROM " + fileDirectory.getPath());

                WildcardFileFilter fileFileFilter = new WildcardFileFilter(nameFilter);
                for (File fileFile : FileUtils.listFiles(fileDirectory, fileFileFilter, null)) {
                    Log.d("DOWNLOAD", "CLEANUP: FOUND " + fileFile.getPath() + ", DELETING");
                    FileUtils.deleteQuietly(fileFile);
                }
            } else {
                if (checkExpansionFiles(context, Constants.PATCH, Constants.PATCH_VERSION)) {
                    Log.d("DOWNLOAD", "PATCH EXPANSION FILE FOUND (NO DOWNLOAD)");
                } else {
                    Log.d("DOWNLOAD", "PATCH EXPANSION FILE NOT FOUND (DOWNLOADING)");

                    final LigerDownloadManager patchDownload = new LigerDownloadManager(Constants.PATCH, Constants.PATCH_VERSION, context, true);
                    Thread patchDownloadThread = new Thread(patchDownload);

                    Toast.makeText(context, "Starting download of patch for content pack.", Toast.LENGTH_LONG).show(); // FIXME move to strings

                    patchDownloadThread.start();

                    // need a better solution
                    try {
                        synchronized (waitObj) {
                            Log.d("WAITING", Constants.PATCH + " " + Constants.PATCH_VERSION);
                            waitObj.wait(5000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        HashMap<String, ExpansionIndexItem> installedIndex = IndexManager.loadInstalledIdIndex(context);
        HashMap<String, ExpansionIndexItem> availableIndex = IndexManager.loadAvailableIdIndex(context);

        for (String id : installedIndex.keySet()) {

            ExpansionIndexItem installedItem = installedIndex.get(id);
            ExpansionIndexItem availableItem = availableIndex.get(id);

            checkAndDownload(context, installedItem, availableItem);
        }
    }

    // need to be able to check/download a single file (currently only supports content packs)

    public static void checkAndDownload(Context context, ExpansionIndexItem installedItem, ExpansionIndexItem availableItem) {

        // need to compare main and patch versions
        // update installed index for consistency and to minimize code impact
        if ((installedItem.getExpansionFileVersion() != null) &&
            (availableItem.getExpansionFileVersion() != null) &&
            (Integer.parseInt(availableItem.getExpansionFileVersion()) > Integer.parseInt(installedItem.getExpansionFileVersion()))) {
            Log.d("DOWNLOAD", "FOUND NEWER VERSION OF MAIN EXPANSION ITEM " + installedItem.getExpansionId() + " (" + availableItem.getExpansionFileVersion() + " vs. " + installedItem.getExpansionFileVersion() + ") UPDATING");
            installedItem.setExpansionFileVersion(availableItem.getExpansionFileVersion());
            IndexManager.registerInstalledIndexItem(context, installedItem);
        }

        // need to account for case where installed item has no defined patch version
        if (availableItem.getPatchFileVersion() != null) {
            if (installedItem.getPatchFileVersion() != null) {
                if (Integer.parseInt(availableItem.getPatchFileVersion()) > Integer.parseInt(installedItem.getPatchFileVersion())) {
                    Log.d("DOWNLOAD", "FOUND NEWER VERSION OF PATCH EXPANSION ITEM " + installedItem.getExpansionId() + " (" + availableItem.getPatchFileVersion() + " vs. " + installedItem.getPatchFileVersion() + ") UPDATING");
                    installedItem.setPatchFileVersion(availableItem.getPatchFileVersion());
                    IndexManager.registerInstalledIndexItem(context, installedItem);
                }
            } else {
                Log.d("DOWNLOAD", "FOUND NEWER VERSION OF PATCH EXPANSION ITEM " + installedItem.getExpansionId() + " (" + availableItem.getPatchFileVersion() + " vs. " + installedItem.getPatchFileVersion() + ") UPDATING");
                installedItem.setPatchFileVersion(availableItem.getPatchFileVersion());
                IndexManager.registerInstalledIndexItem(context, installedItem);
            }
        }

        checkAndDownload(context, installedItem);
    }

    public static void checkAndDownload(Context context, ExpansionIndexItem installedItem) {

        String filePath = IndexManager.buildFilePath(installedItem);
        String fileName = IndexManager.buildFileName(installedItem, Constants.MAIN);

        File expansionFile = new File(filePath + fileName);

        // should be able to check this locally
        // if (checkExpansionFiles(context, fileName, installedItem)) {
        if (expansionFile.exists()) {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE " + fileName + " FOUND (NO DOWNLOAD)");
        } else {
            Log.d("DOWNLOAD", "MAIN EXPANSION FILE " + fileName + " NOT FOUND (DOWNLOADING)");

            final LigerAltDownloadManager expansionDownload = new LigerAltDownloadManager(fileName, context, true);
            Thread expansionDownloadThread = new Thread(expansionDownload);

            Toast.makeText(context, "Starting download of expansion file.", Toast.LENGTH_LONG).show(); // FIXME move to strings

            expansionDownloadThread.start();

            // need a better solution
            try {
                synchronized (waitObj) {
                    Log.d("WAITING", fileName);
                    waitObj.wait(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // if the main file is newer than the patch file, remove the patch file rather than downloading
        if (installedItem.getPatchFileVersion() != null) {
            if ((installedItem.getExpansionFileVersion() != null) &&
                (Integer.parseInt(installedItem.getPatchFileVersion()) < Integer.parseInt(installedItem.getExpansionFileVersion()))) {

                File obbDirectory = new File(ZipHelper.getObbFolderName(context));
                File fileDirectory = new File(ZipHelper.getFileFolderName(context));

                String nameFilter = installedItem.getExpansionId() + "." + Constants.PATCH + "*" + ".obb";

                Log.d("DOWNLOAD", "CLEANUP: DELETING " + nameFilter + " FROM " + obbDirectory.getPath());

                WildcardFileFilter obbFileFilter = new WildcardFileFilter(nameFilter);
                for (File obbFile : FileUtils.listFiles(obbDirectory, obbFileFilter, null)) {
                    Log.d("DOWNLOAD", "CLEANUP: FOUND " + obbFile.getPath() + ", DELETING");
                    FileUtils.deleteQuietly(obbFile);
                }

                Log.d("DOWNLOAD", "CLEANUP: DELETING " + nameFilter + " FROM " + fileDirectory.getPath());

                WildcardFileFilter fileFileFilter = new WildcardFileFilter(nameFilter);
                for (File fileFile : FileUtils.listFiles(fileDirectory, fileFileFilter, null)) {
                    Log.d("DOWNLOAD", "CLEANUP: FOUND " + fileFile.getPath() + ", DELETING");
                    FileUtils.deleteQuietly(fileFile);
                }
            } else {

                String patchName = IndexManager.buildFileName(installedItem, Constants.PATCH);

                expansionFile = new File(filePath + patchName);

                // should be able to check this locally
                // if (checkExpansionFiles(context, patchName, installedItem)) {
                if (expansionFile.exists()) {
                    Log.d("DOWNLOAD", "EXPANSION FILE PATCH " + patchName + " FOUND (NO DOWNLOAD)");
                } else {
                    Log.d("DOWNLOAD", "EXPANSION FILE PATCH " + patchName + " NOT FOUND (DOWNLOADING)");

                    final LigerAltDownloadManager expansionDownload = new LigerAltDownloadManager(patchName, context, true);
                    Thread expansionDownloadThread = new Thread(expansionDownload);

                    Toast.makeText(context, "Starting download of expansion file patch.", Toast.LENGTH_LONG).show(); // FIXME move to strings

                    expansionDownloadThread.start();

                    // need a better solution
                    try {
                        synchronized (waitObj) {
                            Log.d("WAITING", patchName);
                            waitObj.wait(5000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static ExpansionIndexItem fixStats (ExpansionIndexItem installedItem, ExpansionIndexItem availableItem) {

        ArrayList<String> updatedStats = new ArrayList<String>();

        // if size/hash don't match, defer to available index

        if ((availableItem.getExpansionFileSize() > 0) &&
            (installedItem.getExpansionFileSize() != availableItem.getExpansionFileSize())) {
            installedItem.setExpansionFileSize(availableItem.getExpansionFileSize());
            updatedStats.add("expansionFileSize");
        }

        if ((availableItem.getPatchFileSize() > 0) &&
            (installedItem.getPatchFileSize() != availableItem.getPatchFileSize())) {
            installedItem.setPatchFileSize(availableItem.getPatchFileSize());
            updatedStats.add("patchFileSize");
        }

        if (availableItem.getExpansionFileChecksum() != null) {
            if (installedItem.getExpansionFileChecksum() != null) {
                if (!installedItem.getExpansionFileChecksum().equals(availableItem.getExpansionFileChecksum())) {
                    installedItem.setExpansionFileChecksum(availableItem.getExpansionFileChecksum());
                    updatedStats.add("expansionFileChecksum");
                }
            } else {
                installedItem.setExpansionFileChecksum(availableItem.getExpansionFileChecksum());
                updatedStats.add("expansionFileChecksum");
            }
        }

        if (availableItem.getPatchFileChecksum() != null) {
            if (installedItem.getPatchFileChecksum() != null) {
                if (!installedItem.getPatchFileChecksum().equals(availableItem.getPatchFileChecksum())) {
                    installedItem.setPatchFileChecksum(availableItem.getPatchFileChecksum());
                    updatedStats.add("patchFileChecksum");
                }
            } else {
                installedItem.setPatchFileChecksum(availableItem.getPatchFileChecksum());
                updatedStats.add("patchFileChecksum");
            }
        }

        if (!updatedStats.isEmpty()) {
            Log.d("INDEX", "UPDATED STATS FOR " + installedItem.getExpansionId() + ": " + updatedStats.toString());
            return installedItem;
        } else {
            return null;
        }

    }
}
