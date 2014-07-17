package scal.io.liger;

import android.app.Activity;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mnbogner on 7/14/14.
 */
public class JsonHelper {
    public static String loadJSON(Context context, String jsonFile) {
        String jsonString = "";

        try {
            InputStream jsonStream = context.getAssets().open(jsonFile);

            int size = jsonStream.available();
            byte[] buffer = new byte[size];
            jsonStream.read(buffer);
            jsonStream.close();
            jsonString = new String(buffer);
        } catch (IOException e) {
            System.err.println("READING JSON FILE FAILED: " + e.getMessage());
        }

        return jsonString;
    }
}
