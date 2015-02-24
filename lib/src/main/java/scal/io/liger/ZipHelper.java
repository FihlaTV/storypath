package scal.io.liger;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import scal.io.liger.model.ExpansionIndexItem;

/**
 * @author Matt Bogner
 * @author Josh Steiner
 */
public class ZipHelper {

    public static String getExpansionZipFilename(Context ctx, String mainOrPatch, int version) {
        String packageName = ctx.getPackageName();
        String filename = mainOrPatch + "." + version + "." + packageName + ".obb";
        return filename;
    }

    public static String getObbFolderName(Context ctx) {
        String packageName = ctx.getPackageName();
        File root = Environment.getExternalStorageDirectory();
        return root.toString() + "/Android/obb/" + packageName + "/";
    }

    public static String getFileFolderName(Context ctx) {
        String packageName = ctx.getPackageName();
        File root = Environment.getExternalStorageDirectory();
        return root.toString() + "/Android/data/" + packageName + "/files/";
    }

    public static String getFileFolderName(Context context, String fileName) {

        ExpansionIndexItem expansionIndexItem = IndexManager.loadInstalledFileIndex(context).get(fileName);

        if (expansionIndexItem == null) {
            Log.e("DIRECTORIES", "FAILED TO LOCATE EXPANSION INDEX ITEM FOR " + fileName);
            return null;
        }

        File root = Environment.getExternalStorageDirectory();
        return root.toString() + File.separator + expansionIndexItem.getExpansionFilePath();
    }

    public static String getExpansionFileFolder(Context ctx, String mainOrPatch, int version) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // check and/or attempt to create obb folder
            String checkPath = getObbFolderName(ctx);
            File checkDir = new File(checkPath);
            if (checkDir.isDirectory() || checkDir.mkdirs()) {
                File checkFile = new File(checkPath + getExpansionZipFilename(ctx, mainOrPatch, version));
                if (checkFile.exists()) {
                    Log.d("DIRECTORIES", "FOUND OBB IN OBB DIRECTORY: " + checkFile.getPath());
                    return checkPath;
                }
            }

            // check and/or attempt to create files folder
            checkPath = getFileFolderName(ctx);
            checkDir = new File(checkPath);
            if (checkDir.isDirectory() || checkDir.mkdirs()) {
                File checkFile = new File(checkPath + getExpansionZipFilename(ctx, mainOrPatch, version));
                if (checkFile.exists()) {
                    Log.d("DIRECTORIES", "FOUND OBB IN FILES DIRECTORY: " + checkFile.getPath());
                    return checkPath;
                }
            }
        }

        Log.e("DIRECTORIES", "FILE NOT FOUND IN OBB DIRECTORY OR FILES DIRECTORY");
        return null;
    }

    // for additional expansion files, check files folder for specified file
    public static String getExpansionFileFolder(Context ctx, String fileName) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // check and/or attempt to create files folder
            String checkPath = getFileFolderName(ctx, fileName);

            if (checkPath == null) {
                return null;
            }

            File checkDir = new File(checkPath);
            if (checkDir.isDirectory() || checkDir.mkdirs()) {
                File checkFile = new File(checkPath + fileName);
                if (checkFile.exists()) {
                    Log.d("DIRECTORIES", "FOUND OBB IN DIRECTORY: " + checkFile.getPath());
                    return checkPath;
                }
            }
        }

        Log.e("DIRECTORIES", "FILE NOT FOUND");
        return null;
    }

    public static ZipResourceFile getResourceFile(Context context) {
        try {
            // resource file contains main file and patch file

            ArrayList<String> paths = new ArrayList<String>();
            paths.add(getExpansionFileFolder(context, Constants.MAIN, Constants.MAIN_VERSION) + getExpansionZipFilename(context, Constants.MAIN, Constants.MAIN_VERSION));

            if (Constants.PATCH_VERSION > 0) {

                // if the main file is newer than the patch file, do not apply a patch file
                if (Constants.PATCH_VERSION < Constants.MAIN_VERSION) {
                    Log.d("ZIP", "PATCH VERSION " + Constants.PATCH_VERSION + " IS OUT OF DATE (MAIN VERSION IS " + Constants.MAIN_VERSION + ")");
                } else {
                    Log.d("ZIP", "APPLYING PATCH VERSION " + Constants.PATCH_VERSION + " (MAIN VERSION IS " + Constants.MAIN_VERSION + ")");
                    paths.add(getExpansionFileFolder(context, Constants.PATCH, Constants.PATCH_VERSION) + getExpansionZipFilename(context, Constants.PATCH, Constants.PATCH_VERSION));
                }

            }

            // add 3rd party stuff
            HashMap<String, ExpansionIndexItem> expansionIndex = IndexManager.loadInstalledOrderIndex(context);

            // need to sort patch order keys, numbers may not be consecutive
            ArrayList<String> orderNumbers = new ArrayList<String>(expansionIndex.keySet());
            Collections.sort(orderNumbers);

            for (String orderNumber : orderNumbers) {
                ExpansionIndexItem item = expansionIndex.get(orderNumber);
                if (item == null) {
                    Log.d("ZIP", "EXPANSION FILE ENTRY MISSING AT PATCH ORDER NUMBER " + orderNumber);
                } else {
                    String fileName = item.getExpansionFileName();
                    if (DownloadHelper.checkExpansionFiles(context, fileName)) {
                        // Log.d("ZIP", "EXPANSION FILE " + getExpansionFileFolder(context, fileName) + fileName + " FOUND, ADDING TO ZIP");
                        paths.add(getExpansionFileFolder(context, fileName) + fileName);
                    } else {
                        Log.e("ZIP", "EXPANSION FILE " + fileName + " NOT FOUND, CANNOT ADD TO ZIP");
                    }
                }
            }

            ZipResourceFile resourceFile = APKExpansionSupport.getResourceZipFile(paths.toArray(new String[paths.size()]));

            return resourceFile;
        } catch (IOException ioe) {
            Log.e(" *** TESTING *** ", "Could not open resource file (main version " + Constants.MAIN_VERSION + ", patch version " + Constants.PATCH_VERSION + ")");
            return null;
        }
    }

    public static InputStream getFileInputStream(String path, Context context, String language) {

        String localizedFilePath = path;

        // check language setting and insert country code if necessary

        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (path.lastIndexOf("-" + language + path.substring(path.lastIndexOf("."))) < 0) {
                localizedFilePath = path.substring(0, path.lastIndexOf(".")) + "-" + language + path.substring(path.lastIndexOf("."));
            }
            Log.d("LANGUAGE", "getFileInputStream() - TRYING LOCALIZED PATH: " + localizedFilePath);
        }

        InputStream fileStream = getFileInputStream(localizedFilePath, context);

        // if there is no result with the localized path, retry with default path
        if (fileStream == null) {
            if (localizedFilePath.contains("-")) {
                localizedFilePath = localizedFilePath.substring(0, localizedFilePath.lastIndexOf("-")) + localizedFilePath.substring(localizedFilePath.lastIndexOf("."));
                Log.d("LANGUAGE", "getFileInputStream() - NO RESULT WITH LOCALIZED PATH, TRYING DEFAULT PATH: " + localizedFilePath);
                fileStream = ZipHelper.getFileInputStream(localizedFilePath, context);
            }
        } else {
            return fileStream;
        }

        if (fileStream == null) {
            Log.d("LANGUAGE", "getFileInputStream() - NO RESULT WITH DEFAULT PATH: " + localizedFilePath);
        } else {
            return fileStream;
        }

        return null;
    }


    public static InputStream getFileInputStream(String path, Context context) {

        // resource file contains main file and patch file

        ArrayList<String> paths = new ArrayList<String>();
        paths.add(getExpansionFileFolder(context, Constants.MAIN, Constants.MAIN_VERSION) + getExpansionZipFilename(context, Constants.MAIN, Constants.MAIN_VERSION));
        if (Constants.PATCH_VERSION > 0) {

            // if the main file is newer than the patch file, do not apply a patch file
            if (Constants.PATCH_VERSION < Constants.MAIN_VERSION) {
                Log.d("ZIP", "PATCH VERSION " + Constants.PATCH_VERSION + " IS OUT OF DATE (MAIN VERSION IS " + Constants.MAIN_VERSION + ")");
            } else {
                Log.d("ZIP", "APPLYING PATCH VERSION " + Constants.PATCH_VERSION + " (MAIN VERSION IS " + Constants.MAIN_VERSION + ")");
                paths.add(getExpansionFileFolder(context, Constants.PATCH, Constants.PATCH_VERSION) + getExpansionZipFilename(context, Constants.PATCH, Constants.PATCH_VERSION));
            }

        }

        // add 3rd party stuff
        HashMap<String, ExpansionIndexItem> expansionIndex = IndexManager.loadInstalledOrderIndex(context);

        // need to sort patch order keys, numbers may not be consecutive
        ArrayList<String> orderNumbers = new ArrayList<String>(expansionIndex.keySet());
        Collections.sort(orderNumbers);

        for (String orderNumber : orderNumbers) {
            ExpansionIndexItem item = expansionIndex.get(orderNumber);
            if (item == null) {
                Log.d("ZIP", "EXPANSION FILE ENTRY MISSING AT PATCH ORDER NUMBER " + orderNumber);
            } else {
                String fileName = item.getExpansionFileName();
                if (DownloadHelper.checkExpansionFiles(context, fileName)) {
                    // Log.d("ZIP", "EXPANSION FILE " + getExpansionFileFolder(context, fileName) + fileName + " FOUND, ADDING TO ZIP");
                    paths.add(getExpansionFileFolder(context, fileName) + fileName);
                } else {
                    Log.e("ZIP", "EXPANSION FILE " + fileName + " NOT FOUND, CANNOT ADD TO ZIP");
                }
            }
        }

        return getFileInputStreamFromFiles(paths, path, context);
    }

    public static InputStream getFileInputStreamFromFile(String zipPath, String filePath, Context context) {

        ArrayList<String> zipPaths = new ArrayList<String>();
        zipPaths.add(zipPath);

        return getFileInputStreamFromFiles(zipPaths, filePath, context);
    }

    public static InputStream getFileInputStreamFromFiles(ArrayList<String> zipPaths, String filePath, Context context) {
        try {
            ZipResourceFile resourceFile = APKExpansionSupport.getResourceZipFile(zipPaths.toArray(new String[zipPaths.size()]));

            if (resourceFile == null) {
                return null;
            }

            // file path must be relative to the root of the resource file
            InputStream resourceStream = resourceFile.getInputStream(filePath);

            if (resourceStream == null) {
                Log.d(" *** TESTING *** ", "Could not find file " + filePath + " within resource file (main version " + Constants.MAIN_VERSION + ", patch version " + Constants.PATCH_VERSION + ")");
            } else {
                Log.d(" *** TESTING *** ", "Found file " + filePath + " within resource file (main version " + Constants.MAIN_VERSION + ", patch version " + Constants.PATCH_VERSION + ")");
            }
            return resourceStream;
        } catch (IOException ioe) {
            Log.e(" *** TESTING *** ", "Could not find file " + filePath + " within resource file (main version " + Constants.MAIN_VERSION + ", patch version " + Constants.PATCH_VERSION + ")");
            return null;
        }
    }

    public static File getTempFile(String path, String tempPath, Context context) {

        String extension = path.substring(path.lastIndexOf("."));

        File tempFile = new File(tempPath + File.separator + "TEMP" + extension);

        try {
            if (tempFile.exists()) {
                tempFile.delete();
                Log.d(" *** TESTING *** ", "Deleted temp file " + tempFile.getPath());
            }
            tempFile.createNewFile();
            Log.d(" *** TESTING *** ", "Made temp file " + tempFile.getPath());
        } catch (IOException ioe) {
            Log.e(" *** TESTING *** ", "Failed to clean up existing temp file " + tempFile.getPath() + ", " + ioe.getMessage());
            return null;
        }

        InputStream zipInput = getFileInputStream(path, context);

        if (zipInput == null) {
            Log.e(" *** TESTING *** ", "Failed to open input stream for " + path + " in .zip file");
            return null;
        }

        try {
            FileOutputStream tempOutput = new FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int i;
            while((i = zipInput.read(buf)) > 0) {
                tempOutput.write(buf, 0, i);
            }
            tempOutput.close();
            zipInput.close();
            Log.d(" *** TESTING *** ", "Wrote temp file " + tempFile.getPath());
        } catch (IOException ioe) {
            Log.e(" *** TESTING *** ", "Failed to write to temp file " + tempFile.getPath() + ", " + ioe.getMessage());
            return null;
        }

        Log.e(" *** TESTING *** ", "Created temp file " + tempFile.getPath());

        return tempFile;
    }
}
