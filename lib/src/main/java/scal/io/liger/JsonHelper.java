package scal.io.liger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import scal.io.liger.model.Card;
import scal.io.liger.model.Dependency;
import scal.io.liger.model.StoryPath;
import scal.io.liger.model.StoryPathLibrary;
import timber.log.Timber;

/**
 * @author Matt Bogner
 * @author Josh Steiner
 */
public class JsonHelper {

    public static final String KEY_USE_IOCIPHER = "p_use_iocipher_storage";

    private static final String TAG = "JsonHelper";
    private static final String LIGER_DIR = "Liger";
    private static File selectedJSONFile = null;
    private static String selectedJSONPath = null;
    private static ArrayList<String> jsonFileNamesList = null;
    private static ArrayList<File> jsonFileList = null;
    private static ArrayList<String> jsonPathList = null;
    private static HashMap<String, String> jsonKeyToPath = null;
    //private static String sdLigerFilePath = null;

    //private static String language = null; // TEMP

    // TEMP - for gathering insance files to test references
    @NonNull
    public static ArrayList<String> getInstancePaths(@NonNull Context context) {

        ArrayList<String> results = new ArrayList<String>();

        String path = getSdLigerFilePath(context);
        if (path != null) {
            File jsonFolder = new File(path);
            // check for nulls (uncertain as to cause of nulls)
            if ((jsonFolder.listFiles() != null)) {
                for (File jsonFile : jsonFolder.listFiles()) {
                    if (jsonFile.getName().contains("-instance") && !jsonFile.isDirectory()) {
                        Timber.d("FOUND INSTANCE PATH: " + jsonFile.getPath());
                        results.add(jsonFile.getPath());
                    }
                }
            } else {
                Timber.d(getSdLigerFilePath(context) + " WAS NULL OR listFiles() RETURNED NULL, CANNOT GATHER INSTANCE PATHS");
            }
        }

        return results;
    }

    /*
    public static ArrayList<String> getLibraryInstancePaths() {

        ArrayList<String> results = new ArrayList<String>();

        File jsonFolder = new File(getSdLigerFilePath());
        // check for nulls (uncertain as to cause of nulls)
        if ((jsonFolder != null) && (jsonFolder.listFiles() != null)) {
            for (File jsonFile : jsonFolder.listFiles()) {
                if (jsonFile.getName().contains("-library-instance") && !jsonFile.isDirectory()) {
                    Timber.d("FOUND LIBRARY INSTANCE PATH: " + jsonFile.getPath());
                    results.add(jsonFile.getPath());
                }
            }
        } else {
            Timber.d(getSdLigerFilePath() + " WAS NULL OR listFiles() RETURNED NULL, CANNOT GATHER INSTANCE PATHS");
        }

        return results;
    }
    */

    public static String loadJSONFromPath(@NonNull String jsonPath, @NonNull String language) {

        String jsonString = "";
        String sdCardState = Environment.getExternalStorageState();

        String localizedFilePath = jsonPath;

        // check language setting and insert country code if necessary
        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (jsonPath.lastIndexOf("-" + language + jsonPath.substring(jsonPath.lastIndexOf("."))) < 0) {
                localizedFilePath = jsonPath.substring(0, jsonPath.lastIndexOf(".")) + "-" + language + jsonPath.substring(jsonPath.lastIndexOf("."));
            }
            Timber.d("loadJSONFromPath() - LOCALIZED PATH: " + localizedFilePath);
        }

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File jsonFile = new File(jsonPath);
                InputStream jsonStream = new FileInputStream(jsonFile);

                File localizedFile = new File(localizedFilePath);
                // if there is a file at the localized path, use that instead
                if ((localizedFile.exists()) && (!jsonPath.equals(localizedFilePath))) {
                    Timber.d("loadJSONFromPath() - USING LOCALIZED FILE: " + localizedFilePath);
                    jsonStream = new FileInputStream(localizedFile);
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException e) {
                Timber.e("READING JSON FILE FROM SD CARD FAILED: " + e.getMessage());
            } catch (NullPointerException npe) {
                Timber.e("READING JSON FILE FROM SD CARD FAILED (STREAM WAS NULL): " + npe.getMessage());
            }
        } else {
            System.err.println("SD CARD NOT FOUND");
        }

        return jsonString;
    }

    public static String loadJSON(Context context, String language) {
        // check for file
        // paths to actual files should fully qualified
        // paths within zip files should be relative
        // (or at least not resolve to actual files)
        File checkFile = new File(selectedJSONPath);
        //if (selectedJSONFile.exists()) {
        //    return loadJSON(selectedJSONFile, language);
        //} else {
        if (checkFile.exists()) {
            return loadJSON(checkFile.getPath(), context, language);
        } else {
            return loadJSONFromZip(selectedJSONPath, context, language);
        }
    }

    @Nullable
    public static String loadJSON(String jsonFilePath, Context context, String language) {

        if(null == jsonFilePath) {
            return null;
        }

        String jsonString = null;
        String sdCardState = Environment.getExternalStorageState();

        // FIXME break out into localizing file path method to use in available index localizer
        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary
        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
            }
            Timber.d("loadJSON() - LOCALIZED PATH: " + localizedFilePath);
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); // hopefully context is valid?
        boolean useIOCipher = settings.getBoolean(KEY_USE_IOCIPHER, false);

        // use iocipher if specified, vfs should have been mounted when cacheword was unlocked
        if (useIOCipher) {

            Timber.d("LOADING JSON FROM FILE " + jsonFilePath);

            // info.guardianproject.iocipher.File
            // info.guardianproject.iocipher.FileInputStream

            try {
                info.guardianproject.iocipher.File jsonFile = new info.guardianproject.iocipher.File(jsonFilePath);
                info.guardianproject.iocipher.FileInputStream jsonStream = new info.guardianproject.iocipher.FileInputStream(jsonFile);

                info.guardianproject.iocipher.File localizedFile = new info.guardianproject.iocipher.File(localizedFilePath);
                // if there is a file at the localized path, use that instead
                if ((localizedFile.exists()) && (!jsonFilePath.equals(localizedFilePath))) {
                    Timber.d("loadJSON() - USING LOCALIZED VIRTUAL FILE: " + localizedFilePath);
                    jsonStream = new info.guardianproject.iocipher.FileInputStream(localizedFile);
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException e) {
                Timber.e("READING JSON FILE FRON VIRTUAL STORAGE FAILED: " + e.getMessage());
            } catch (NullPointerException npe) {
                Timber.e("READING JSON FILE FROM VIRTUAL STORAGE FAILED (STREAM WAS NULL): " + npe.getMessage());
            }
        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File jsonFile = new File(jsonFilePath);
                InputStream jsonStream = new FileInputStream(jsonFile);

                File localizedFile = new File(localizedFilePath);
                // if there is a file at the localized path, use that instead
                if ((localizedFile.exists()) && (!jsonFilePath.equals(localizedFilePath))) {
                    Timber.d("loadJSON() - USING LOCALIZED FILE: " + localizedFilePath);
                    jsonStream = new FileInputStream(localizedFile);
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException e) {
                Timber.e("READING JSON FILE FRON SD CARD FAILED: " + e.getMessage());
            } catch (NullPointerException npe) {
                Timber.e("READING JSON FILE FROM SD CARD FAILED (STREAM WAS NULL): " + npe.getMessage());
            }
        } else {
            Timber.e("SD CARD NOT FOUND");
        }

        return jsonString;
    }

    // NEW

    // merged with regular loadJSON method
    /*
    public static String loadJSONFromZip(Context context, String language) {
        return loadJSONFromZip(selectedJSONPath, context, language);
    }
    */
    @Nullable
    public static String loadJSONFromZip(String jsonFilePath, Context context, String language) {

        //Timber.d("NEW METHOD loadJSONFromZip CALLED FOR " + jsonFilePath);

        if(null == jsonFilePath) {
            return null;
        }

        String jsonString = null;

//        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary
//        if (language != null) {
            // just in case, check whether country code has already been inserted
//            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
//                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
//            }
//            Timber.d("loadJSONFromZip() - LOCALIZED PATH: " + localizedFilePath);
//        }

        // removed sd card check as expansion file should not be located on sd card
            try {
                InputStream jsonStream = ZipHelper.getFileInputStream(jsonFilePath, context, language);

                // if there is no result with the localized path, retry with default path
//                if ((jsonStream == null) && (!jsonFilePath.equals(localizedFilePath))) {
//                    jsonStream = ZipHelper.getFileInputStream(jsonFilePath, context);
//                } else {
//                    Timber.d("loadJSONFromZip() - USING LOCALIZED FILE: " + localizedFilePath);
//                }

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from ZIP file failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from ZIP file failed: " + ioe.getMessage());
            }

        return jsonString;
    }

    @Nullable
    public static String getSdLigerFilePath(@NonNull Context context) {
        // this only works until the class is garbage collected
        // return sdLigerFilePath;

        // String sdCardFolderPath = context.getExternalFilesDir(null).getPath();
        File storageDirectory = StorageHelper.getActualStorageDirectory(context);
        if (storageDirectory != null) {
            String sdCardFolderPath = storageDirectory.getPath();

            String sdLigerFilePath = sdCardFolderPath + File.separator;

            Timber.d("CONSTRUCTED LIGER FILE PATH: " + sdLigerFilePath);

            return sdLigerFilePath;
        } else {
            return null;
        }
    }

    private static void copyFilesToSdCard(@NonNull Context context, @NonNull String basePath) {
        copyFileOrDir(context, basePath, ""); // copy all files in assets folder in my project
    }

    private static void copyFileOrDir(@NonNull Context context, @NonNull String assetFromPath, @NonNull String baseToPath) {
        AssetManager assetManager = context.getAssets();
        String assets[] = null;
        try {
            Log.i("tag", "copyFileOrDir() "+assetFromPath);
            assets = assetManager.list(assetFromPath);
            if (assets.length == 0) {
                copyFile(context, assetFromPath, baseToPath);
            } else {
                String fullPath =  baseToPath + assetFromPath;
                Log.i("tag", "path="+fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !assetFromPath.startsWith("images") && !assetFromPath.startsWith("sounds") && !assetFromPath.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir "+fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (assetFromPath.equals(""))
                        p = "";
                    else
                        p = assetFromPath + "/";

                    if (!assetFromPath.startsWith("images") && !assetFromPath.startsWith("sounds") && !assetFromPath.startsWith("webkit"))
                        copyFileOrDir(context, p + assets[i], baseToPath);
                }
            }
        } catch (IOException ex) {
            Timber.e("I/O Exception", ex);
        }
    }

    private static void copyFile(@NonNull Context context, @NonNull String fromFilename, @NonNull String baseToPath) {
        AssetManager assetManager = context.getAssets();

        if (fromFilename.endsWith(".obb"))
            return;  // let's not copy .obb files, they get copied elseware

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() "+fromFilename);
            in = assetManager.open(fromFilename);
            if (fromFilename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = baseToPath + fromFilename.substring(0, fromFilename.length()-4);
            else
                newFileName = baseToPath + fromFilename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Timber.e("Exception in copyFile() of "+newFileName);
            Timber.e("Exception in copyFile() "+e.toString());
        }

    }

    private static void copyObbFile(@NonNull Context context, @NonNull String mainOrPatch, int version, @NonNull String toPath) {
        AssetManager assetManager = context.getAssets();

        String obbFileName = mainOrPatch + "." + version + ".obb";

        File file = new File(toPath);
        if (!(file.exists())) {
//            file.mkdirs();
            InputStream in = null;
            OutputStream out = null;
            try {
                Log.i("tag", "copyObbFile() " + obbFileName);
                in = assetManager.open(obbFileName);
                out = new FileOutputStream(toPath);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (Exception e) {
                Timber.e("Exception in copyObbFile() of " + toPath);
                Timber.e("Exception in copyObbFile() " + e.toString());
            }
        }
    }
    // FIXME this seems to be required to have been run to use any of the static methods like getLibraryInstanceFiles, we shouldn't have black magic like this
    public static void setupFileStructure(@NonNull Context context) {
        String sdCardState = Environment.getExternalStorageState();
        String sdLigerFilePath;

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            // FIXME we need to remove this, it seems like the popup stuff requires it even though we acutally read files from .obb not the Liger folder

            // String sdCardFolderPath = context.getExternalFilesDir(null).getPath();
            String sdCardFolderPath = StorageHelper.getActualStorageDirectory(context).getPath();

            sdLigerFilePath = sdCardFolderPath + File.separator;
            Timber.d("NEW EXTERNAL DIRECTORY: " + sdLigerFilePath);

            new File(sdLigerFilePath).mkdirs();
        } else {
            Timber.e("SD CARD NOT FOUND"); // FIXME don't bury errors in logs, we should let this crash
        }
    }

    private static void setupJSONFileList(Context context) {

        // HARD CODING LIST
        // rebuild list to pick up new save files
        // if (jsonFileNamesList == null) {
            File ligerFile_1 = new File(getSdLigerFilePath(context) + "/org.storymaker.app/default/default_library.json");
//            File ligerFile_2 = new File(sdLigerFilePath + "/default/learning_guide_1/learning_guide_1.json");
//            File ligerFile_3 = new File(sdLigerFilePath + "/default/learning_guide_2/learning_guide_2.json");
//            File ligerFile_4 = new File(sdLigerFilePath + "/default/learning_guide_3/learning_guide_3.json");
//            File ligerFile_5 = new File(sdLigerFilePath + "/default/learning_guide_library.json");
//            File ligerFile_6 = new File(sdLigerFilePath + "/default/learning_guide_library_SAVE.json");
//            File ligerFile_7 = new File(getSdLigerFilePath(context) + "/learning_guide/learning_guide_1/learning_guide_1_library.json");
//            File ligerFile_8 = new File(getSdLigerFilePath(context) + "/learning_guide/learning_guide_2/learning_guide_2_library.json");
//            File ligerFile_9= new File(getSdLigerFilePath(context) + "/learning_guide/learning_guide_3/learning_guide_3_library.json");

            jsonFileNamesList = new ArrayList<String>();
            jsonFileNamesList.add(ligerFile_1.getName());
//            jsonFileNamesList.add(ligerFile_2.getName());
//            jsonFileNamesList.add(ligerFile_3.getName());
//            jsonFileNamesList.add(ligerFile_4.getName());
//            jsonFileNamesList.add(ligerFile_5.getName());
//            jsonFileNamesList.add(ligerFile_6.getName());
//            jsonFileNamesList.add(ligerFile_7.getName());
//            jsonFileNamesList.add(ligerFile_8.getName());
//            jsonFileNamesList.add(ligerFile_9.getName());

            jsonFileList = new ArrayList<File>();
            jsonFileList.add(ligerFile_1);
//            jsonFileList.add(ligerFile_2);
//            jsonFileList.add(ligerFile_3);
//            jsonFileList.add(ligerFile_4);
//            jsonFileList.add(ligerFile_5);
//            jsonFileList.add(ligerFile_6);
//            jsonFileList.add(ligerFile_7);
//            jsonFileList.add(ligerFile_8);
//            jsonFileList.add(ligerFile_9);

            jsonPathList = new ArrayList<String>();
            jsonPathList.add("org.storymaker.app/default/default_library.json");
//            jsonPathList.add("default/learning_guide_1/learning_guide_1.json");
//            jsonPathList.add("default/learning_guide_2/learning_guide_2.json");
//            jsonPathList.add("default/learning_guide_3/learning_guide_3.json");
//            jsonPathList.add("default/learning_guide_library.json");
//            jsonPathList.add("default/learning_guide_library_SAVE.json");
//            jsonPathList.add("org.storymaker.app/learning_guide/learning_guide_1/learning_guide_1_library.json");
//            jsonPathList.add("org.storymaker.app/learning_guide/learning_guide_2/learning_guide_2_library.json");
//            jsonPathList.add("org.storymaker.app/learning_guide/learning_guide_3/learning_guide_3_library.json");

            jsonKeyToPath = new HashMap<String, String>();
            jsonKeyToPath.put("default_library", "org.storymaker.app/default/default_library.json");
//            jsonKeyToPath.put("learning_guide_1.json", "default/learning_guide_1/learning_guide_1.json");
//            jsonKeyToPath.put("learning_guide_2.json", "default/learning_guide_2/learning_guide_2.json");
//            jsonKeyToPath.put("learning_guide_3.json", "default/learning_guide_3/learning_guide_3.json");
//            jsonKeyToPath.put("learning_guide_library", "default/learning_guide_library.json");
//            jsonKeyToPath.put("learning_guide_library_SAVE", "default/learning_guide_library_SAVE.json");
//            jsonKeyToPath.put("learning_guide_1_library", "org.storymaker.app/learning_guide/learning_guide_1/learning_guide_1_library.json");
//            jsonKeyToPath.put("learning_guide_2_library", "org.storymaker.app/learning_guide/learning_guide_2/learning_guide_2_library.json");
//            jsonKeyToPath.put("learning_guide_3_library", "org.storymaker.app/learning_guide/learning_guide_3/learning_guide_3_library.json");


            for (File jsonFile : getLibraryInstanceFiles(context)) {
                jsonFileNamesList.add(jsonFile.getName());
                jsonFileList.add(jsonFile);
                jsonPathList.add(jsonFile.getPath());
                jsonKeyToPath.put(jsonFile.getName(), jsonFile.getPath());
            }
        // }
    }

    /*
    public static ArrayList<File> getInstanceFiles() {

        ArrayList<File> results = new ArrayList<File>();

        File jsonFolder = new File(getSdLigerFilePath());
        // check for nulls (uncertain as to cause of nulls)
        if ((jsonFolder != null) && (jsonFolder.listFiles() != null)) {
            for (File jsonFile : jsonFolder.listFiles()) {
                if (jsonFile.getName().contains("-instance") && !jsonFile.isDirectory()) {
                    Timber.d("FOUND INSTANCE FILE: " + jsonFile.getName());
                    File localFile = new File(jsonFile.getPath());
                    results.add(localFile);
                }
            }
        } else {
            Timber.d(getSdLigerFilePath() + " WAS NULL OR listFiles() RETURNED NULL, CANNOT GATHER INSTANCE FILES");
        }

        return results;
    }
    */

    @NonNull
    public static ArrayList<File> getLibraryInstalledFiles(Context context) {

        ArrayList<File> results = new ArrayList<File>();

        File obbFolder = new File(getSdLigerFilePath(context));
        // check for nulls (uncertain as to cause of nulls)
        if ((obbFolder != null) && (obbFolder.listFiles() != null)) {
            for (File obbFile : obbFolder.listFiles()) {
                if (obbFile.getName().endsWith(".obb") &&
                        !obbFile.isDirectory()) {
                    Timber.d("FOUND LIBRARY INSTALLED FILE: " + obbFile.getName());
                    File localFile = new File(obbFile.getPath());
                    results.add(localFile);
                }
            }
        } else {
            Timber.d(getSdLigerFilePath(context) + " WAS NULL OR listFiles() RETURNED NULL, CANNOT GATHER INSTANCE FILES");
        }

        return results;
    }


    @NonNull
    public static ArrayList<File> getLibraryInstanceFiles(Context context) {

        ArrayList<File> results = new ArrayList<File>();

        File jsonFolder = new File(getSdLigerFilePath(context));
        // check for nulls (uncertain as to cause of nulls)
        if ((jsonFolder != null) && (jsonFolder.listFiles() != null)) {
            for (File jsonFile : jsonFolder.listFiles()) {
                if (jsonFile.getName().contains("-library-instance") &&
                        jsonFile.getName().endsWith(".json") &&
                        !jsonFile.isDirectory()) {
                    Timber.d("FOUND LIBRARY INSTANCE FILE: " + jsonFile.getName());
                    File localFile = new File(jsonFile.getPath());
                    results.add(localFile);
                }
            }
        } else {
            Timber.d(getSdLigerFilePath(context) + " WAS NULL OR listFiles() RETURNED NULL, CANNOT GATHER INSTANCE FILES");
        }

        return results;
    }

    @Nullable
    public static String[] getJSONFileList(Context context) {
        //ensure path has been set
        if(null == getSdLigerFilePath(context)) {
            return null;
        }

        setupJSONFileList(context);

        /*
        File ligerDir = new File(sdLigerFilePath);
        if (ligerDir != null) {
            for (File file : ligerDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    jsonFileNamesList.add(file.getName());
                    jsonFileList.add(file);
                }
            }
        }

        File defaultLigerDir = new File(sdLigerFilePath + "/default/");
        if (defaultLigerDir != null) {
            for (File file : defaultLigerDir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    jsonFileNamesList.add(file.getName());
                    jsonFileList.add(file);
                }
            }
        }
        */

        return jsonFileNamesList.toArray(new String[jsonFileNamesList.size()]);
    }

    @Nullable
    public static String getJsonPathByKey(Context context, String key) {
        setupJSONFileList(context);
        if (jsonKeyToPath.containsKey(key)) {
            return jsonKeyToPath.get(key);
        } else {
            return null;
        }
    }

    public static File setSelectedJSONFile(int index) {
        selectedJSONFile = jsonFileList.get(index);
        return selectedJSONFile;
    }

    public static String setSelectedJSONPath(int index) {
        selectedJSONPath = jsonPathList.get(index);
        return selectedJSONPath;
    }

    /*
    private static void addFileToSDCard(InputStream jsonInputStream, String filePath) {
        OutputStream outputStream = null;

        try {
            // write the inputStream to a FileOutputStream
            outputStream = new FileOutputStream(new File(sdLigerFilePath + filePath));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = jsonInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jsonInputStream != null) {
                try {
                    jsonInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    */

    @Nullable
    public static StoryPathLibrary loadStoryPathLibrary(@NonNull String jsonFilePath, @NonNull ArrayList<String> referencedFiles, @NonNull Context context, @NonNull String language) {

        //Timber.d("NEW METHOD loadStoryPathLibrary CALLED FOR " + jsonFilePath);

        String storyPathLibraryJson = "";
        String sdCardState = Environment.getExternalStorageState();

        // templates are loaded from zips, instances will not have localized file names
        /*
        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary
        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
            }
            Timber.d("loadStoryPathLibrary() - LOCALIZED PATH: " + localizedFilePath);
        }

        File f = new File(localizedFilePath);
        if ((!f.exists()) && (!localizedFilePath.equals(jsonFilePath))) {
            f = new File(jsonFilePath);
        } else {
            Timber.d("loadStoryPathLibrary() - USING LOCALIZED FILE: " + localizedFilePath);
        }
        */

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); // hopefully context is valid?
        boolean useIOCipher = settings.getBoolean(KEY_USE_IOCIPHER, false);

        // use iocipher if specified, vfs should have been mounted when cacheword was unlocked
        if (useIOCipher) {

            Timber.d("LOADING STORY PATH LIBRARY " + jsonFilePath);

            // info.guardianproject.iocipher.File
            // info.guardianproject.iocipher.FileInputStream

            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(jsonFilePath);

            try {
                info.guardianproject.iocipher.FileInputStream jsonStream = new info.guardianproject.iocipher.FileInputStream(f);

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from from virtual failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathLibraryJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from virtual file failed: " + ioe.getMessage());
                return null;
            }
        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {

            File f = new File(jsonFilePath);

            try {
                InputStream jsonStream = new FileInputStream(f);

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from SD card failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathLibraryJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from SD card failed: " + ioe.getMessage());
                return null;
            }
        } else {
            Timber.e("SD card not found");
            return null;
        }

        return deserializeStoryPathLibrary(storyPathLibraryJson, jsonFilePath, referencedFiles, context, language);

    }

    // NEW

    @Nullable
    public static StoryPathLibrary loadStoryPathLibraryFromZip(@NonNull String jsonFilePath, @NonNull ArrayList<String> referencedFiles, @NonNull Context context, @NonNull String language) {

        //Timber.d("NEW METHOD loadStoryPathLibraryFromZip CALLED FOR " + jsonFilePath);

        String storyPathLibraryJson = "";

//        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary

//        if (language != null) {
            // just in case, check whether country code has already been inserted
//            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
//                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
//            }
//            Timber.d("loadStoryPathLibraryFromZip() - LOCALIZED PATH: " + localizedFilePath);
//        }

        // removed sd card check as expansion file should not be located on sd card
            try {
                InputStream jsonStream = ZipHelper.getFileInputStream(jsonFilePath, context, language);

                // if there is no result with the localized path, retry with default path
//                if ((jsonStream == null) && localizedFilePath.contains("-")) {
//                    localizedFilePath = localizedFilePath.substring(0, localizedFilePath.lastIndexOf("-")) + localizedFilePath.substring(localizedFilePath.lastIndexOf("."));
//                    jsonStream = ZipHelper.getFileInputStream(localizedFilePath, context);
//                    Timber.d("loadStoryPathLibraryFromZip() - USING DEFAULT FILE: " + localizedFilePath);
//                } else {
//                    Timber.d("loadStoryPathLibraryFromZip() - USING LOCALIZED FILE: " + localizedFilePath);
//                }

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from ZIP file failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathLibraryJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from ZIP file failed: " + ioe.getMessage());
                return null;
            }

        String localizedFilePath = jsonFilePath;

        // need to localize path for deserialization (this can probably be handled better)

        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
                // if not already appended, don't bother to append -en
                if (!"en".equals(language)) {
                    localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
                    Timber.d("loadStoryPathLibraryFromZip() - LOCALIZED PATH: " + localizedFilePath);
                } else {
                    Timber.d("loadStoryPathLibraryFromZip() - PATH: " + localizedFilePath);
                }
            } else {
                Timber.d("loadStoryPathLibraryFromZip() - LOCALIZED PATH: " + localizedFilePath);
            }
        } else {
            Timber.d("loadStoryPathLibraryFromZip() - PATH: " + localizedFilePath);
        }

        return deserializeStoryPathLibrary(storyPathLibraryJson, localizedFilePath, referencedFiles, context, language);
    }

    @Nullable
    public static StoryPathLibrary deserializeStoryPathLibrary(@NonNull String storyPathLibraryJson, @NonNull String jsonFilePath, @NonNull ArrayList<String> referencedFiles, @NonNull Context context, @NonNull String language) {

        //Timber.d("NEW METHOD deserializeStoryPathLibrary CALLED FOR " + jsonFilePath);

        GsonBuilder gBuild = new GsonBuilder();
        gBuild.registerTypeAdapter(StoryPathLibrary.class, new StoryPathLibraryDeserializer());
        Gson gson = gBuild.excludeFieldsWithoutExposeAnnotation().create();

        // fromJson has a bug where it will return null if storyPathLibraryJson is "", therefore this method is @Nullable
        // https://github.com/google/gson/issues/457
        StoryPathLibrary storyPathLibrary = gson.fromJson(storyPathLibraryJson, StoryPathLibrary.class);

         if (jsonFilePath.contains("instance")) {
            Timber.d("LOCALIZING AN INSTANCE: " + jsonFilePath);

            // if an instance has been loaded:
            // - jsonFilePath will not be localized
            // - compare language code in object to language code argument
            // - if different, load template, import translated strings
            // - update template location and language code
            // - version?
            if (storyPathLibrary.getLanguage() == null) {
                // default to en
                storyPathLibrary.setLanguage("en");
            }
            if (storyPathLibrary.getLanguage().equals(language)) {
                Timber.d("LANGUAGE MATCHES: " + storyPathLibrary.getLanguage() + "/" + language);
                // language matches, all is well
            } else {
                Timber.d("LANGUAGE DOESN'T MATCH: " + storyPathLibrary.getLanguage() + "/" + language);
                // language mis-match, need to load template
                String instanceTemplate = storyPathLibrary.getTemplatePath();
                if (instanceTemplate == null) {
                    Timber.d("NO TEMPLATE, TRYING SOMETHING ELSE");
                    // can't identify template, can't fix language

                    HashMap<String, String> templateMap = IndexManager.loadTempateIndex(context);

                    String templateString = jsonFilePath.substring(jsonFilePath.lastIndexOf(File.separator) + 1, jsonFilePath.indexOf("-")) + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));

                    Timber.d("TRYING TO LOOK UP TEMPLATE FOR " + templateString);

                    instanceTemplate = templateMap.get(templateString);

                    Timber.d("FOUND TEMPLATE: " + instanceTemplate);
                }

                if (instanceTemplate == null) {
                    Timber.d("STILL NO TEMPLATE, CAN'T UPDATE STRINGS");
                } else {
                    // re-construct template name
                    String newTemplate = instanceTemplate.substring(0, instanceTemplate.lastIndexOf('.'));
                    if (newTemplate.lastIndexOf('-') > 0) {
                        newTemplate = newTemplate.substring(0, newTemplate.lastIndexOf('-'));
                    }
                    // FIXME: language sometimes comes in as "english" rather than "en"
                    if (!"en".equals(language)) {
                        newTemplate = newTemplate + '-' + language;
                    }
                    newTemplate = newTemplate + instanceTemplate.substring(instanceTemplate.lastIndexOf('.'));
                    Timber.d("GETTING STRINGS FROM TEMPLATE: " + newTemplate);

                    StoryPathLibrary newStoryPathLibrary = loadStoryPathLibraryFromZip(newTemplate, referencedFiles, context, language);

                    // FIXME: method was updated to handle nulls, but we should probably find a better way to
                    // FIXME: skip the whole localization process if the selected language isn't available
                    updateStoryPathLibraryStrings(storyPathLibrary, newStoryPathLibrary);
                    storyPathLibrary.setLanguage(language);
                    storyPathLibrary.setTemplatePath(newTemplate);
                }
            }
        } else if (jsonFilePath.equals("SAVED_STATE")) {
            // this method gets called to de-serialize saved states, but no localization should be required in that context
        } else {
            Timber.d("LOCALIZING A TEMPLATE: " + jsonFilePath);

            // if a template has been loaded:
            // - jsonFilePath should include the localized file name
            // - need to set template location in object
            // - need to set language code in object
            // - no need to import translated strings
            // - version?
            storyPathLibrary.setLanguage(language);
            storyPathLibrary.setTemplatePath(jsonFilePath);
            Timber.d("SET LANGUAGE/TEMPLATE: " + language + ", " + jsonFilePath);
        }

        // a story path library model must have a file location to manage relative paths
        // if it is loaded from a saved state, the location should already be set
        if ((jsonFilePath == null) || (jsonFilePath.length() == 0) || (jsonFilePath.equals("SAVED_STATE"))) {
            if ((storyPathLibrary.getFileLocation() == null) || (storyPathLibrary.getFileLocation().length() == 0)) {
                Timber.e("file location for story path library " + storyPathLibrary.getId() + " could not be determined");
                return null;
            } else {
                Timber.d("incoming path is <" + jsonFilePath + ">, using existing file location: " + storyPathLibrary.getFileLocation());
            }
        } else {
            File checkFile = new File(jsonFilePath);
            if (!checkFile.exists()) {
                storyPathLibrary.setFileLocation(jsonFilePath); // FOR NOW, DON'T SAVE ACTIAL FILE LOCATIONS
            }
        }

        // construct and insert dependencies
        for (String referencedFile : referencedFiles) {
            Dependency dependency = new Dependency();
            dependency.setDependencyFile(referencedFile);

            // extract id from path/file name
            // assumes format <path/library id>-instance-<timestamp>.json
            // assumes path/library id doesn't not contain "-"
            String derivedId = referencedFile.substring(referencedFile.lastIndexOf(File.separator) + 1);
            derivedId = derivedId.substring(0, derivedId.indexOf("-"));
            dependency.setDependencyId(derivedId);
            storyPathLibrary.addDependency(dependency);
            Timber.d("DEPENDENCY: " + derivedId + " -> " + referencedFile);
        }

        storyPathLibrary.setCardReferences();
        storyPathLibrary.initializeObservers();
        storyPathLibrary.setContext(context);

        return storyPathLibrary;
    }

    public static void updateStoryPathLibraryStrings(StoryPathLibrary storyPathLibrary, StoryPathLibrary storyPathLibraryTemplate) {

        if (storyPathLibraryTemplate == null) {
            Timber.e("TEMPLATE WAS NULL, CAN'T UPDATE STRINGS");
            return;
        }

        for (Card card : storyPathLibraryTemplate.getCards()) {
            try {
                storyPathLibrary.getCardByIdOnly(card.getId()).copyText(card);
                // Timber.d("FOUND CARD " + card.getId() + " AND UPDATED STRINGS");
            } catch (NullPointerException npe) {
                Timber.e("COULD NOT FIND CARD " + card.getId() + " TO UPDATE STRINGS");
            }
        }
    }

    @NonNull
    public static String getStoryPathLibrarySaveFileName(StoryPathLibrary storyPathLibrary) {

        //Timber.d("NEW METHOD getStoryPathLibrarySaveFileName CALLED FOR " + storyPathLibrary.getId());

        Date timeStamp = new Date();
        //String jsonFilePath = storyPathLibrary.buildZipPath(storyPathLibrary.getId() + "_" + timeStamp.getTime() + ".json");
        //TEMP
        String jsonFilePath = storyPathLibrary.buildTargetPath(storyPathLibrary.getId() + "-library-instance-" + timeStamp.getTime() + ".json");

        return jsonFilePath;
    }

    public static boolean saveStoryPathLibrary(StoryPathLibrary storyPathLibrary, String jsonFilePath) {

        //Timber.d("NEW METHOD saveStoryPathLibrary CALLED FOR " + storyPathLibrary.getId() + " -> " + jsonFilePath);

        String sdCardState = Environment.getExternalStorageState();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(storyPathLibrary.getContext()); // hopefully context is valid?
        boolean useIOCipher = settings.getBoolean(KEY_USE_IOCIPHER, false);

        // use iocipher if specified, vfs should have been mounted when cacheword was unlocked
        if (useIOCipher) {

            Timber.d("SAVING STORY PATH LIBRARY " + jsonFilePath);

            // info.guardianproject.iocipher.File
            // info.guardianproject.iocipher.FileOutputStream

            try {
                // can't use .swap file, i don't think there's a way to do cp/mv on virtual files
                info.guardianproject.iocipher.File storyPathLibraryFile = new info.guardianproject.iocipher.File(jsonFilePath);
                if (storyPathLibraryFile.exists()) {
                    storyPathLibraryFile.delete();
                }
                storyPathLibraryFile.createNewFile();
                info.guardianproject.iocipher.FileOutputStream storyPathLibraryStream = new info.guardianproject.iocipher.FileOutputStream(storyPathLibraryFile);

                Timber.d("VIRTUAL FILE READY, STREAM OPENED");

                String storyPathLibraryJson = serializeStoryPathLibrary(storyPathLibrary);

                byte storyPathLibraryData[] = storyPathLibraryJson.getBytes();
                storyPathLibraryStream.write(storyPathLibraryData);
                storyPathLibraryStream.flush();
                storyPathLibraryStream.close();

                Timber.d("SUCCESS?");

            } catch (IOException ioe) {
                Timber.e("writing json file " + jsonFilePath + " to virtual file failed: " + ioe.getMessage());
                return false;
            }

        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File storyPathLibraryFile = new File(jsonFilePath + ".swap"); // NEED TO WRITE TO SWAP AND COPY
                if (storyPathLibraryFile.exists()) {
                    storyPathLibraryFile.delete();
                }
                storyPathLibraryFile.createNewFile();
                FileOutputStream storyPathLibraryStream = new FileOutputStream(storyPathLibraryFile);

                String storyPathLibraryJson = serializeStoryPathLibrary(storyPathLibrary);

                byte storyPathLibraryData[] = storyPathLibraryJson.getBytes();
                storyPathLibraryStream.write(storyPathLibraryData);
                storyPathLibraryStream.flush();
                storyPathLibraryStream.close();

                Process p = Runtime.getRuntime().exec("mv " + jsonFilePath + ".swap " + jsonFilePath);

            } catch (IOException ioe) {
                Timber.e("writing json file " + jsonFilePath + " to SD card failed: " + ioe.getMessage());
                return false;
            }
        } else {
            Timber.e("SD card not found");
            return false;
        }

        // update file location
        // TEMP - this will break references to content in zip file.  unsure what to do...
        // storyPathLibrary.setFileLocation(jsonFilePath);

        return true;
    }

    @Nullable
    public static String serializeStoryPathLibrary(StoryPathLibrary storyPathLibrary) {

        //Timber.d("NEW METHOD serializeStoryPathLibrary CALLED FOR " + storyPathLibrary.getId());

        GsonBuilder gBuild = new GsonBuilder();
        Gson gson = gBuild.excludeFieldsWithoutExposeAnnotation().create();

        String storyPathLibraryJson = gson.toJson(storyPathLibrary);

        return storyPathLibraryJson;
    }

    @Nullable
    public static StoryPath loadStoryPath(String jsonFilePath, StoryPathLibrary storyPathLibrary, ArrayList<String> referencedFiles, Context context, String language) {

        //Timber.d("NEW METHOD loadStoryPath CALLED FOR " + jsonFilePath);

        String storyPathJson = "";
        String sdCardState = Environment.getExternalStorageState();

        // templates are loaded from zips, instances will not have localized file names
        /*
        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary
        if (language != null) {
            // just in case, check whether country code has already been inserted
            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
            }
            Timber.d("loadStoryPath() - LOCALIZED PATH: " + localizedFilePath);
        }

        File f = new File(localizedFilePath);
        if ((!f.exists()) && (!localizedFilePath.equals(jsonFilePath))) {
            f = new File(jsonFilePath);
        } else {
            Timber.d("loadStoryPath() - USING LOCALIZED FILE: " + localizedFilePath);
        }
        */

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); // hopefully context is valid?
        boolean useIOCipher = settings.getBoolean(KEY_USE_IOCIPHER, false);

        // use iocipher if specified, vfs should have been mounted when cacheword was unlocked
        if (useIOCipher) {

            Timber.d("LOADING STORY PATH " + jsonFilePath);

            // info.guardianproject.iocipher.File
            // info.guardianproject.iocipher.FileInputStream

            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(jsonFilePath);

            try {
                info.guardianproject.iocipher.FileInputStream jsonStream = new info.guardianproject.iocipher.FileInputStream(f);

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from from virtual failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from virtual file failed: " + ioe.getMessage());
                return null;
            }
        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {

            File f = new File(jsonFilePath);

            try {
                InputStream jsonStream = new FileInputStream(f);

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from SD card failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from SD card failed: " + ioe.getMessage());
                return null;
            }
        } else {
            Timber.e("SD card not found");
            return null;
        }

        return deserializeStoryPath(storyPathJson, jsonFilePath, storyPathLibrary, referencedFiles, context, language);
    }

    // NEW

    @Nullable
    public static StoryPath loadStoryPathFromZip(String jsonFilePath, StoryPathLibrary storyPathLibrary, ArrayList<String> referencedFiles, Context context, String language) {

        //Timber.d("NEW METHOD loadStoryPathFromZip CALLED FOR " + jsonFilePath);

        String storyPathJson = "";

//        String localizedFilePath = jsonFilePath;

        // check language setting and insert country code if necessary

//        if (language != null) {
            // just in case, check whether country code has already been inserted
//            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
//                localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
//            }
//            Timber.d("loadStoryPathFromZip() - LOCALIZED PATH: " + localizedFilePath);
//        }

        // removed sd card check as expansion file should not be located on sd card
            try {
                InputStream jsonStream = ZipHelper.getFileInputStream(jsonFilePath, context, language);

                // if there is no result with the localized path, retry with default path
//                if ((jsonStream == null) && localizedFilePath.contains("-")) {
//                    localizedFilePath = localizedFilePath.substring(0, localizedFilePath.lastIndexOf("-")) + localizedFilePath.substring(localizedFilePath.lastIndexOf("."));
//                    jsonStream = ZipHelper.getFileInputStream(localizedFilePath, context);
//                    Timber.d("loadStoryPathFromZip() - USING DEFAULT FILE: " + localizedFilePath);
//                } else {
//                    Timber.d("loadStoryPathFromZip() - USING LOCALIZED FILE: " + localizedFilePath);
//                }

                if (jsonStream == null) {
                    Timber.e("reading json file " + jsonFilePath + " from ZIP file failed (stream was null)");
                    return null;
                }

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                storyPathJson = new String(buffer);
            } catch (IOException ioe) {
                Timber.e("reading json file " + jsonFilePath + " from ZIP file failed: " + ioe.getMessage());
                return null;
            }

        String localizedFilePath = jsonFilePath;

        // need to localize path for deserialization (this can probably be handled better)

        if (language != null) {
        // just in case, check whether country code has already been inserted
            if (jsonFilePath.lastIndexOf("-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."))) < 0) {
                if (!"en".equals(language)) {
                    localizedFilePath = jsonFilePath.substring(0, jsonFilePath.lastIndexOf(".")) + "-" + language + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));
                    Timber.d("loadStoryPathFromZip() - LOCALIZED PATH: " + localizedFilePath);
                } else {
                    Timber.d("loadStoryPathFromZip() - PATH: " + localizedFilePath);
                }
            } else {
                Timber.d("loadStoryPathFromZip() - LOCALIZED PATH: " + localizedFilePath);
            }
        } else {
            Timber.d("loadStoryPathFromZip() - PATH: " + localizedFilePath);
        }

        return deserializeStoryPath(storyPathJson, localizedFilePath, storyPathLibrary, referencedFiles, context, language);
    }

    public static StoryPath deserializeStoryPath(String storyPathJson, String jsonFilePath, StoryPathLibrary storyPathLibrary, ArrayList<String> referencedFiles, Context context, String language) {

        //Timber.d("NEW METHOD deserializeStoryPath CALLED FOR " + jsonFilePath);

        GsonBuilder gBuild = new GsonBuilder();
        gBuild.registerTypeAdapter(StoryPath.class, new StoryPathDeserializer());
        Gson gson = gBuild.excludeFieldsWithoutExposeAnnotation().create();

        StoryPath storyPath = gson.fromJson(storyPathJson, StoryPath.class);

        if (jsonFilePath.contains("instance")) {
            Timber.d("LOCALIZING AN INSTANCE: " + jsonFilePath);

            // if an instance has been loaded:
            // - jsonFilePath will not be localized
            // - compare language code in object to language code argument
            // - if different, load template, import translated strings
            // - update template location and language code
            // - version?
            if (storyPath.getLanguage() == null) {
                // default to en
                storyPath.setLanguage("en");
            }
            if (storyPath.getLanguage().equals(language)) {
                Timber.d("LANGUAGE MATCHES: " + storyPath.getLanguage() + "/" + language);
                // language matches, all is well
            } else {
                // language mis-match, need to load template
                Timber.d("LANGUAGE DOESN'T MATCH: " + storyPath.getLanguage() + "/" + language);
                String instanceTemplate = storyPath.getTemplatePath();
                if (instanceTemplate == null) {
                    Timber.d("NO TEMPLATE, TRYING SOMETHING ELSE");
                    // can't identify template, can't fix language

                    HashMap<String, String> templateMap = IndexManager.loadTempateIndex(context);

                    String templateString = jsonFilePath.substring(jsonFilePath.lastIndexOf(File.separator) + 1, jsonFilePath.indexOf("-")) + jsonFilePath.substring(jsonFilePath.lastIndexOf("."));

                    Timber.d("TRYING TO LOOK UP TEMPLATE FOR " + templateString);

                    instanceTemplate = templateMap.get(templateString);

                    Timber.d("FOUND TEMPLATE: " + instanceTemplate);
                }

                if (instanceTemplate == null) {
                    Timber.d("STILL NO TEMPLATE, CAN'T UPDATE STRINGS");
                } else {
                    // re-construct template name
                    String newTemplate = instanceTemplate.substring(0, instanceTemplate.lastIndexOf('.'));
                    if (newTemplate.lastIndexOf('-') > 0) {
                        newTemplate = newTemplate.substring(0, newTemplate.lastIndexOf('-'));
                    }
                    if (!"en".equals(language)) {
                        newTemplate = newTemplate + '-' + language;
                    }
                    newTemplate = newTemplate + instanceTemplate.substring(instanceTemplate.lastIndexOf('.'));

                    StoryPath newStoryPath = loadStoryPathFromZip(newTemplate, storyPathLibrary, referencedFiles, context, language);

                    if (newStoryPath == null) {
                        Timber.d("TEMPLATE " + newTemplate + " FAILED TO LOAD, CAN'T UPDATE STRINGS");
                        // can't load template, can't fix language
                    } else {
                        Timber.d("GETTING STRINGS FROM TEMPLATE: " + newTemplate);
                        updateStoryPathStrings(storyPath, newStoryPath);
                        storyPath.setLanguage(language);
                        storyPath.setTemplatePath(newTemplate);
                    }
                }
            }
        }  else {
            Timber.d("LOCALIZING A TEMPLATE: " + jsonFilePath);

            // if a template has been loaded:
            // - jsonFilePath should include the localized file name
            // - need to set template location in object
            // - need to set language code in object
            // - no need to import translated strings
            // - version?
            storyPath.setLanguage(language);
            storyPath.setTemplatePath(jsonFilePath);
            Timber.d("SET LANGUAGE/TEMPLATE: " + language + ", " + jsonFilePath);
        }

        // a story path model must have a file location to manage relative paths
        // if it is loaded from a saved state, the location should already be set
        if ((jsonFilePath == null) || (jsonFilePath.length() == 0)) {
            if ((storyPath.getFileLocation() == null) || (storyPath.getFileLocation().length() == 0)) {
                Timber.e("file location for story path " + storyPath.getId() + " could not be determined");
            }
        } else {
            File checkFile = new File(jsonFilePath);
            if (!checkFile.exists()) {
                storyPath.setFileLocation(jsonFilePath); // FOR NOW, DON'T SAVE ACTIAL FILE LOCATIONS
            }
        }

        storyPath.setCardReferences();
        storyPath.initializeObservers();
        storyPath.setStoryPathLibrary(storyPathLibrary);
        // THIS MAY HAVE UNINTENDED CONSEQUENCES...
        if (storyPath.getStoryPathLibraryFile() == null) {
            storyPath.setStoryPathLibraryFile(storyPathLibrary.getFileLocation());

        }

        // construct and insert dependencies
        for (String referencedFile : referencedFiles) {
            Dependency dependency = new Dependency();
            dependency.setDependencyFile(referencedFile);

            // extract id from path/file name
            // assumes format <path/library id>-instance-<timestamp>.json
            // assumes path/library id doesn't not contain "-"
            String derivedId = referencedFile.substring(referencedFile.lastIndexOf(File.separator) + 1);
            derivedId = derivedId.substring(0, derivedId.indexOf("-"));
            dependency.setDependencyId(derivedId);
            storyPath.addDependency(dependency);
            Timber.d("DEPENDENCY: " + derivedId + " -> " + referencedFile);
        }

        storyPath.setContext(context);

        return storyPath;
    }

    public static void updateStoryPathStrings(StoryPath storyPath, StoryPath storyPathTemplate) {
        for (Card card : storyPathTemplate.getCards()) {
            try {
                storyPath.getCardByIdOnly(card.getId()).copyText(card);
                Timber.d("FOUND CARD " + card.getId() + " AND UPDATED STRINGS");
            } catch (NullPointerException npe) {
                Timber.e("COULD NOT FIND CARD " + card.getId() + " TO UPDATE STRINGS");
            }
        }
    }

    public static String getStoryPathSaveFileName(StoryPath storyPath) {

        //Timber.d("NEW METHOD getStoryPathSaveFileName CALLED FOR " + storyPath.getId());

        Date timeStamp = new Date();
        //String jsonFilePath = storyPath.buildZipPath(storyPath.getId() + "_" + timeStamp.getTime() + ".json");
        //TEMP
        String jsonFilePath = storyPath.buildTargetPath(storyPath.getId() + "-instance-" + timeStamp.getTime() + ".json");

        return jsonFilePath;
    }

    public static boolean saveStoryPath(StoryPath storyPath, String jsonFilePath) {

        //Timber.d("NEW METHOD getStoryPathSaveFileName CALLED FOR " + storyPath.getId() + " -> " + jsonFilePath);

        String sdCardState = Environment.getExternalStorageState();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(storyPath.getContext()); // hopefully context is valid?
        boolean useIOCipher = settings.getBoolean(KEY_USE_IOCIPHER, false);

        // use iocipher if specified, vfs should have been mounted when cacheword was unlocked
        if (useIOCipher) {

            Timber.d("SAVING STORY PATH " + jsonFilePath);

            // info.guardianproject.iocipher.File
            // info.guardianproject.iocipher.FileOutputStream

            try {
                // can't use .swap file, i don't think there's a way to do cp/mv on virtual files
                info.guardianproject.iocipher.File storyPathFile = new info.guardianproject.iocipher.File(jsonFilePath);
                if (storyPathFile.exists()) {
                    storyPathFile.delete();
                }
                storyPathFile.createNewFile();
                info.guardianproject.iocipher.FileOutputStream storyPathStream = new info.guardianproject.iocipher.FileOutputStream(storyPathFile);

                Timber.d("VIRTUAL FILE READY, STREAM OPENED");

                String storyPathJson = serializeStoryPath(storyPath);

                byte storyPathData[] = storyPathJson.getBytes();
                storyPathStream.write(storyPathData);
                storyPathStream.flush();
                storyPathStream.close();

                Timber.d("SUCCESS?");

            } catch (IOException ioe) {
                Timber.e("writing json file " + jsonFilePath + " to virtual file failed: " + ioe.getMessage());
                return false;
            }

        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File storyPathFile = new File(jsonFilePath + ".swap"); // NEED TO WRITE TO SWAP AND COPY
                if (storyPathFile.exists()) {
                    storyPathFile.delete();
                }
                storyPathFile.createNewFile();
                FileOutputStream storyPathStream = new FileOutputStream(storyPathFile);

                String storyPathJson = serializeStoryPath(storyPath);

                byte storyPathData[] = storyPathJson.getBytes();
                storyPathStream.write(storyPathData);
                storyPathStream.flush();
                storyPathStream.close();

                Process p = Runtime.getRuntime().exec("mv " + jsonFilePath + ".swap " + jsonFilePath);

            } catch (IOException ioe) {
                Timber.e("writing json file " + jsonFilePath + " to SD card failed: " + ioe.getMessage());
                return false;
            }
        } else {
            Timber.e("SD card not found");
            return false;
        }

        // update file location
        // TEMP - this will break references to content in zip file.  unsure what to do...
        // storyPath.setFileLocation(jsonFilePath);

        return true;
    }

    @Nullable
    public static String serializeStoryPath(StoryPath storyPath) {

        //Timber.d("NEW METHOD serializeStoryPath CALLED FOR " + storyPath.getId());

        GsonBuilder gBuild = new GsonBuilder();
        Gson gson = gBuild.excludeFieldsWithoutExposeAnnotation().create();

        // set aside references to prevent circular dependencies when serializing
        /*
        Context tempContext = storyPath.getContext();

        StoryPathLibrary tempStoryPathLibrary = storyPath.getStoryPathLibrary();
        storyPath.setContext(null);
        storyPath.setStoryPathLibrary(null);
        storyPath.clearObservers();
        storyPath.clearCardReferences();
        */

        String storyPathJson = gson.toJson(storyPath);

        // restore references
        /*
        storyPath.setCardReferences();
        storyPath.initializeObservers();
        storyPath.setStoryPathLibrary(tempStoryPathLibrary);
        storyPath.setContext(tempContext);
        */

        return storyPathJson;
    }

    public static void cleanup (String swapFilePath) {
        // delete lingering .swap files from failed saves
        String swapFilter = "*.swap";

        Timber.d("CLEANUP: DELETING " + swapFilter + " FROM " + swapFilePath);

        WildcardFileFilter swapFileFilter = new WildcardFileFilter(swapFilter);
        File swapDirectory = new File(swapFilePath);
        for (File swapFile : FileUtils.listFiles(swapDirectory, swapFileFilter, null)) {
            Timber.d("CLEANUP: FOUND " + swapFile.getPath() + ", DELETING");
            FileUtils.deleteQuietly(swapFile);
        }
    }

}
