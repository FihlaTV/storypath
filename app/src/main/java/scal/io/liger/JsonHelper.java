package scal.io.liger;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mnbogner on 7/14/14.
 */
public class JsonHelper {

    private static final String LIGER_DIR = "Liger";
    private static File selectedJSONFile = null;
    private static ArrayList<File> jsonFileList = null;
    private static String sdLigerFilePath = null;

    public static String loadJSONFromPath(String jsonPath) {
        String jsonString = "";
        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File jsonFile = new File(jsonPath);
                InputStream jsonStream = new FileInputStream(jsonFile);

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException e) {
                System.err.println("READING JSON FILE FRON SD CARD FAILED: " + e.getMessage());
            }
        } else {
            System.err.println("SD CARD NOT FOUND");
        }

        return jsonString;
    }

    public static String loadJSON() {
        if(null == selectedJSONFile) {
            return null;
        }

        String jsonString = "";
        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            try {
                InputStream jsonStream = new FileInputStream(selectedJSONFile);

                int size = jsonStream.available();
                byte[] buffer = new byte[size];
                jsonStream.read(buffer);
                jsonStream.close();
                jsonString = new String(buffer);
            } catch (IOException e) {
                System.err.println("READING JSON FILE FRON SD CARD FAILED: " + e.getMessage());
            }
        } else {
            System.err.println("SD CARD NOT FOUND");
        }

        return jsonString;
    }

    public static void setupFileStructure(Context context, boolean isFirstStart) {
        String sdCardState = Environment.getExternalStorageState();

        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
            String sdCardFolderPath = Environment.getExternalStorageDirectory().getPath();
            sdLigerFilePath = sdCardFolderPath + File.separator + LIGER_DIR + File.separator;

            //create folder if first app launch
            if(isFirstStart) {
                File file = new File(sdLigerFilePath);
                file.mkdirs();

                //add one choice from assets
                try {
                    InputStream jsonStream = context.getAssets().open("learning_guide_v1.json");
                    addFileToSDCard(jsonStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("SD CARD NOT FOUND");
        }
    }

    public static String[] getJSONFileList() {
        //ensure path has been set
        if(null == sdLigerFilePath) {
            return null;
        }

        File ligerDir = new File(sdLigerFilePath);
        ArrayList<String> jsonFileNamesList = new ArrayList<String>();
        jsonFileList = new ArrayList<File>();

        for (File file : ligerDir.listFiles()) {
            if (file.getName().endsWith(".json")) {
                jsonFileNamesList.add(file.getName());
                jsonFileList.add(file);
            }
        }

        return jsonFileNamesList.toArray(new String[jsonFileNamesList.size()]);
    }

    public static void setSelectedJSONFile(int index) {
        selectedJSONFile = jsonFileList.get(index);
    }

    private static void addFileToSDCard(InputStream jsonInputStream) {
        OutputStream outputStream = null;

        try {
            // write the inputStream to a FileOutputStream
            outputStream = new FileOutputStream(new File(sdLigerFilePath + "learning_guide_v1.json"));

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
}