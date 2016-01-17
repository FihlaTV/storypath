package scal.io.liger;

import timber.log.Timber;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.APKExpansionPolicy;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import java.util.Queue;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpRequestRetryHandler;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.conn.ConnectTimeoutException;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

import ch.boye.httpclientandroidlib.impl.client.DefaultHttpRequestRetryHandler;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import scal.io.liger.model.QueueItem;

/**
 * Created by mnbogner on 11/7/14.
 */
public class LigerDownloadManager implements Runnable {
    private final static String TAG = "LigerDownloadManager";

    // TODO use HTTPS
    // TODO pickup Tor settings

    private String mainOrPatch;
    private int version;
    private Context context;

    private DownloadManager dManager;
    private NotificationManager nManager;
    private long lastDownload = -1L;

    StrongHttpsClient mClient = null;

    //boolean useManager = true;
    //boolean useTor = true; // CURRENTLY SET TO TRUE, WILL USE TOR IF ORBOT IS RUNNING

    private static final String ligerId = "scal.io.liger";
    private static final String ligerDevice = Build.MODEL;

    AESObfuscator ligerObfuscator = null;
    APKExpansionPolicy ligerPolicy = null;
    LicenseChecker ligerChecker = null;

    private String mAppTitle;

    public LigerDownloadManager (String mainOrPatch, int version, Context context) {
        this.mainOrPatch = mainOrPatch;
        this.version = version;
        this.context = context;
        //this.useManager = useManager;

        this.mAppTitle = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE).getString(Constants.PREFS_APP_TITLE, "StoryPath");
    }

    public String getMainOrPatch() {
        return mainOrPatch;
    }

    public void setMainOrPatch(String mainOrPatch) {
        this.mainOrPatch = mainOrPatch;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    //public boolean isUseManager() {
    //    return useManager;
    //}

    //public void setUseManager(boolean useManager) {
    //    this.useManager = useManager;
    //}

    @Override
    public void run() {

        boolean downloadRequired = false;

        // NOTE: if whatever process was waiting for the download has died, but the download is still underway
        //       it may require a second click  or restart to get to this point.  if we end up here, with a
        //       finished download and no visible file progress, we'll manage the file and return without
        //       starting another download.

        String fileName = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);
        String filePath = ZipHelper.getExpansionZipDirectory(context, mainOrPatch, version);

        if (checkQueue()) {
            Timber.d("ANOTHER PROCESS IS ALREADY DOWNLOADING " + fileName + ", WILL NOT START DOWNLOAD");
        } else {
            Timber.d("NO OTHER PROCESS IS DOWNLOADING " + fileName + ", CHECKING FOR FILES");

            // force subsequent file check
            downloadRequired = true;

            File tempFile = new File(filePath, fileName + ".tmp");

            if (tempFile.exists()) {

                // TODO - not supporting partial files at this time due to small file size (<1MB)

                File actualFile = new File(filePath, fileName);

                try {
                    FileUtils.moveFile(tempFile, actualFile);
                    FileUtils.deleteQuietly(tempFile);
                    Timber.d("MOVED TEMP FILE " + tempFile.getPath() + " TO " + actualFile.getPath());
                } catch (IOException ioe) {
                    Timber.e("FAILED TO MOVE TEMP FILE " + tempFile.getPath() + " TO " + actualFile.getPath());
                    ioe.printStackTrace();
                    FileUtils.deleteQuietly(tempFile); // cleanup
                }
            } else {
                Timber.d(tempFile.getPath() + " DOES NOT EXIST");
            }
        }

        // final sanity check (file may be so small that it downloaded before check was initiated)
        // NOTE: version update or clearing data required to re-download
        if (downloadRequired) {
            File actualFile = new File(filePath, fileName);

            // if file exists, check file size
            if (actualFile.exists() && (actualFile.length() > 0)) {

                if ((Constants.MAIN.equals(mainOrPatch)) && (Constants.MAIN_SIZE > 0) && (Constants.MAIN_SIZE > actualFile.length())) {
                    Timber.e(actualFile.getPath() + " FOUND, BUT IS TOO SMALL (" + actualFile.length() + "/" + Constants.MAIN_SIZE + ")");
                    // delete incomplete/corrupt file
                    FileUtils.deleteQuietly(actualFile);
                } else if ((Constants.PATCH.equals(mainOrPatch)) && (Constants.PATCH_SIZE > 0) && (Constants.PATCH_SIZE > actualFile.length())) {
                    Timber.e(actualFile.getPath() + " FOUND, BUT IS TOO SMALL (" + actualFile.length() + "/" + Constants.PATCH_SIZE + ")");
                    // delete incomplete/corrupt file
                    FileUtils.deleteQuietly(actualFile);
                } else {
                    Timber.d(actualFile.getPath() + " FOUND, DO NOT DOWNLOAD AGAIN");
                    downloadRequired = false;
                }
            }
        }

        if (downloadRequired) {
            Timber.d(fileName + " MUST BE DOWNLOADED");
        } else {
            Timber.d(fileName + " WILL NOT BE DOWNLOADED");
            return;
        }

        // SHOULD BE ABLE TO ATTEMPT TO GET URL FROM GOOGLE LICENSING AND FALL BACK ON OUR SERVER

        byte[] ligerSALT = context.getResources().getString(R.string.liger_salt).getBytes();

        ligerObfuscator = new AESObfuscator(ligerSALT, ligerId, ligerDevice);
        ligerPolicy = new APKExpansionPolicy(context, ligerObfuscator);
        try {
            ligerChecker = new LicenseChecker(context, ligerPolicy, context.getResources().getString(R.string.base64_public_key));
        } catch (Exception e) {
            // need to catch exception thrown if publisher key is invalid
            // default to downloading from our servers
            Timber.d("LICENSE CHECK EXCEPTION THROWN: " + e.getClass().getName() + ", DOWNLOADING FROM LIGER SERVER");
            downloadFromLigerServer();
            return;
        }

        // callback will download from our servers if licence check fails
        LigerCallback ligerCallback = new LigerCallback();

        String deviceVersion = Build.VERSION.RELEASE;
        int major = Integer.parseInt(deviceVersion.split("\\.")[0]);

        // not sure what the best way to compare versions is (too many decimal points to convert to a number)
        if (major < 5) {
            Timber.d("ABOUT TO CHECK ACCESS ON ANDROID VERSION " + deviceVersion);
            ligerChecker.checkAccess(ligerCallback);
            Timber.d("ACCESS CHECK WAS INITIATED");
        } else {
            Timber.d("CANNOT CHECK ACCESS ON ANDROID VERSION " + deviceVersion + ", DOWNLOADING FROM LIGER SERVER");
            downloadFromLigerServer();
            return;
        }
    }

    public boolean checkQueue() {

        String fileName = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);
        String filePath = ZipHelper.getExpansionZipDirectory(context, mainOrPatch, version);

        File checkFile = new File(filePath, fileName + ".tmp");
        boolean foundInQueue = false;

        // need to check if a download has already been queued for this file
        //HashMap<Long, QueueItem> queueMap = QueueManager.loadQueue(context);

        //for (Long queueId : queueMap.keySet()) {

            //Timber.d("QUEUE ITEM IS " + queueMap.get(queueId).getQueueFile() + " LOOKING FOR " + checkFile.getName());

            //if (checkFile.getName().equals(queueMap.get(queueId).getQueueFile())) {

                Long queueId = QueueManager.checkQueue(context, checkFile);

                if (queueId == null) {

                    // not found
                    foundInQueue = false;

                } else if (queueId.equals(QueueManager.DUPLICATE_QUERY)) {

                    // not exactly in queue, but someone is already looking for this item, so avoid collision
                    foundInQueue = true;

                } else if (queueId < 0) {
                    // use negative numbers to flag non-manager downloads

                    if (checkFileProgress()) {

                        Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " AND DOWNLOAD PROGRESS OBSERVED, LEAVING " + queueId.toString() + " IN QUEUE ");
                        foundInQueue = true;

                    } else {

                        Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " BUT NO DOWNLOAD PROGRESS OBSERVED, REMOVING " + queueId.toString() + " FROM QUEUE ");
                        QueueManager.removeFromQueue(context, Long.valueOf(queueId));

                    }

                } else {
                    // use download manager ids to flag manager downloads

                    // need to init download manager to check queue
                    initDownloadManager();

                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(queueId.longValue());
                    Cursor c = dManager.query(query);
                    try {
                        if (c.moveToFirst()) {

                            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)) {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " BUT DOWNLOAD STATUS IS FAILED, REMOVING " + queueId.toString() + " FROM QUEUE ");
                                QueueManager.removeFromQueue(context, Long.valueOf(queueId));

                            } else if (DownloadManager.STATUS_PAUSED == c.getInt(columnIndex)) {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " AND DOWNLOAD STATUS IS PAUSED, LEAVING " + queueId.toString() + " IN QUEUE ");
                                foundInQueue = true;

                            } else if (DownloadManager.STATUS_PENDING == c.getInt(columnIndex)) {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " AND DOWNLOAD STATUS IS PENDING, LEAVING " + queueId.toString() + " IN QUEUE ");
                                foundInQueue = true;

                            } else if (DownloadManager.STATUS_RUNNING == c.getInt(columnIndex)) {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " AND DOWNLOAD STATUS IS RUNNING, LEAVING " + queueId.toString() + " IN QUEUE ");
                                foundInQueue = true;

                            } else if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " BUT DOWNLOAD STATUS IS SUCCESSFUL, REMOVING " + queueId.toString() + " FROM QUEUE ");
                                QueueManager.removeFromQueue(context, Long.valueOf(queueId));

                            } else {

                                Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " BUT DOWNLOAD STATUS IS UNKNOWN, REMOVING " + queueId.toString() + " FROM QUEUE ");
                                QueueManager.removeFromQueue(context, Long.valueOf(queueId));

                            }
                        } else {

                            Timber.d("QUEUE ITEM FOUND FOR " + checkFile.getName() + " BUT NOTHING FOUND IN DOWNLOAD MANAGER, REMOVING " + queueId.toString() + " FROM QUEUE ");
                            QueueManager.removeFromQueue(context, Long.valueOf(queueId));

                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // cleanup
                    c.close();
                }
            //}

            // skipping timeout check for now, timeout duration undecided

            /*
            if (foundInQueue) {
                Date currentTime = new Date();
                long queuedTime = queueMap.get(queueId).getQueueTime();
                if ((currentTime.getTime() - queueMap.get(queueId).getQueueTime()) > QueueManager.queueTimeout) {

                    Timber.d("TIMEOUT EXCEEDED, REMOVING " + queueId.toString() + " FROM DOWNLOAD MANAGER.");
                    int numberRemoved = manager.remove(queueId);

                    if (numberRemoved == 1) {
                        Timber.d("REMOVED FROM DOWNLOAD MANAGER, RE-QUEUEING: " + queueId.toString() + " -> " + uriFile.toString());
                        QueueManager.removeFromQueue(context, Long.valueOf(queueId));
                        foundInQueue = false;
                    } else {
                        Timber.d("FAILED TO REMOVE FROM DOWNLOAD MANAGER, NOT QUEUEING: " + queueId.toString() + " -> " + uriFile.toString());
                    }
                }
            }
            */
        //}

        return foundInQueue;
    }

    public boolean checkFileProgress() {

        // not a great solution, but should indicate if file is being actively downloaded
        // only .tmp files should be download targets

        String fileName = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);
        String filePath = ZipHelper.getExpansionZipDirectory(context, mainOrPatch, version);

        File checkFile = new File(filePath, fileName + ".tmp");
        if (checkFile.exists()) {
            long firstSize = checkFile.length();

            // wait for download progress
            try {
                synchronized (this) {
                    wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long secondSize = checkFile.length();

            if (secondSize > firstSize) {
                Timber.d("DOWNLOAD IN PROGRESS FOR " + checkFile.getPath() + "(" + firstSize + " -> " + secondSize + ")");

                return true;
            } else {
                Timber.d("NO DOWNLOAD PROGRESS FOR " + checkFile.getPath() + "(" + firstSize + " -> " + secondSize + ")");

                return false;
            }
        } else {
            Timber.d("NO FILE FOUND FOR " + checkFile.getPath());

            return false;
        }
    }

    private void downloadFromLigerServer() {

        String ligerUrl = Constants.LIGER_URL;
        String ligerObb = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);

        try {
            // if we're managing the download, download only to the files folder
            // if we're using the google play api, download only to the obb folder
            File ligerPath = new File(ZipHelper.getFileFolderName(context));

            Timber.d("DOWNLOADING " + ligerObb + " FROM " + ligerUrl + " TO " + ligerPath);

            String nameFilter = "";

            if (ligerObb.startsWith(Constants.MAIN)) {
                nameFilter = nameFilter + Constants.MAIN + ".*." + context.getPackageName() + ".*.tmp";
            }
            if (ligerObb.startsWith(Constants.PATCH)) {
                nameFilter = nameFilter + Constants.PATCH + ".*." + context.getPackageName() + ".*.tmp";
            }

            if (nameFilter.length() == 0) {
                Timber.d("CLEANUP: DON'T KNOW HOW TO BUILD WILDCARD FILTER BASED ON " + ligerObb);
            } else {
                Timber.d("CLEANUP: DELETING " + nameFilter + " FROM " + ligerPath.getPath());
            }

            WildcardFileFilter oldFileFilter = new WildcardFileFilter(nameFilter);
            for (File oldFile : FileUtils.listFiles(ligerPath, oldFileFilter, null)) {
                Timber.d("CLEANUP: FOUND " + oldFile.getPath() + ", DELETING");
                FileUtils.deleteQuietly(oldFile);
            }

            File targetFile = new File(ligerPath, ligerObb + ".tmp");

            // if there is no connectivity, do not queue item (no longer seems to pause if connection is unavailable)
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if ((ni != null) && (ni.isConnectedOrConnecting())) {

                if (context instanceof Activity) {
                    Utility.toastOnUiThread((Activity) context, "Starting download of " + mainOrPatch + " expansion file.", false); // FIXME move to strings
                }

                // check preferences.  will also need to check whether tor is active within method
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                boolean useTor = settings.getBoolean("pusetor", false);
                boolean useManager = settings.getBoolean("pusedownloadmanager", false);

                //if (checkTor(useTor, context)) {
                if (useTor && useManager) {
                    Timber.e("ANDROID DOWNLOAD MANAGER IS NOT COMPATABLE WITH TOR");

                    if (context instanceof Activity) {
                        Utility.toastOnUiThread((Activity) context, "Check settings, can't use download manager and tor", true); // FIXME move to strings
                    }

                    QueueManager.checkQueueFinished(context, targetFile.getName());

                } else if (useTor || !useManager) {
                    downloadWithTor(useTor, Uri.parse(ligerUrl + ligerObb), mAppTitle + " " + mainOrPatch + " file download", ligerObb, targetFile);
                }else {
                    downloadWithManager(Uri.parse(ligerUrl + ligerObb), mAppTitle + " " + mainOrPatch + " file download", ligerObb, Uri.fromFile(targetFile));
                }

            } else {
                Timber.d("NO CONNECTION, NOT QUEUEING DOWNLOAD: " + ligerUrl + ligerObb + " -> " + targetFile.getPath());

                if (context instanceof Activity) {
                    Utility.toastOnUiThread((Activity) context, "Check settings, no connection, can't start download", true); // FIXME move to strings
                }

                QueueManager.checkQueueFinished(context, targetFile.getName());

            }

        } catch (Exception e) {
            Timber.e("DOWNLOAD ERROR: " + ligerUrl + ligerObb + " -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean checkTor(Context mContext) {

        if(OrbotHelper.isOrbotRunning(mContext)) {
            Timber.d("ORBOT RUNNING, USE TOR");
            return true;
        } else {
            Timber.d("ORBOT NOT RUNNING, DON'T USE TOR");
            return false;
        }
    }

    private void downloadWithTor(boolean useTor, Uri uri, String title, String desc, File targetFile) {

        String fileName = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);
        String filePath = ZipHelper.getExpansionZipDirectory(context, mainOrPatch, version);

        initNotificationManager();

        // generate id/tag for notification
        String nTag = mainOrPatch;
        int nId = version;

        StrongHttpsClient httpClient = getHttpClientInstance();

        // we're now using this method to support non-tor downloads as well, so settings must be checked
        if (useTor) {
            if (checkTor(context)) {

                Timber.d("DOWNLOAD WITH TOR PROXY: " + Constants.TOR_PROXY_HOST + "/" + Constants.TOR_PROXY_PORT);

                httpClient.useProxy(true, "http", Constants.TOR_PROXY_HOST, Constants.TOR_PROXY_PORT); // CLASS DOES NOT APPEAR TO REGISTER A SCHEME FOR SOCKS, ORBOT DOES NOT APPEAR TO HAVE AN HTTPS PORT
            } else {
                Timber.e("CANNOT DOWNLOAD WITH TOR, TOR IS NOT ACTIVE");

                if (context instanceof Activity) {
                    Utility.toastOnUiThread((Activity) context, "Check settings, can't use tor if orbot isn't running", true); // FIXME move to strings
                }

                QueueManager.checkQueueFinished(context, targetFile.getName());

                return;
            }
        }

        // disable attempts to retry (more retries ties up connection and prevents failure handling)
        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(1, false);
        httpClient.setHttpRequestRetryHandler(retryHandler);

        // set modest timeout (longer timeout ties up connection and prevents failure handling)
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 3000);
        HttpConnectionParams.setSoTimeout(params, 3000);

        httpClient.setParams(params);

        Timber.d("CHECKING URI: " + uri.toString());

        try {

            HttpGet request = new HttpGet(uri.toString());

            // TODO - can't support partial files until file size is known

            HttpResponse response = httpClient.execute(request);

            HttpEntity entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {

                Timber.d("DOWNLOAD SUCCEEDED, STATUS CODE: " + statusCode);

                // queue item here, "download" doesn't start until after we get a status code

                // queue item, use date to get a unique long, subtract to get a negative number (to distinguish from download manager items)
                Date startTime = new Date();
                long queueId = 0 - startTime.getTime();
                QueueManager.addToQueue(context, queueId, targetFile.getName());

                targetFile.getParentFile().mkdirs();

                Timber.d("DOWNLOAD SUCCEEDED, GETTING ENTITY...");

                BufferedInputStream responseInput = new BufferedInputStream(response.getEntity().getContent());

                try {
                    FileOutputStream targetOutput = new FileOutputStream(targetFile);
                    byte[] buf = new byte[1024];
                    int i;

                    // create status bar notification
                    // TODO - can't support % complete until file size is known
                    Notification nProgress = new Notification.Builder(context)
                            .setContentTitle(mAppTitle + " content download")
                            .setContentText(fileName)
                            .setSmallIcon(android.R.drawable.arrow_down_float)
                            .setWhen(startTime.getTime())
                            .build();
                    nManager.notify(nTag, nId, nProgress);

                    while ((i = responseInput.read(buf)) > 0) {
                        targetOutput.write(buf, 0, i);
                    }
                    targetOutput.close();
                    responseInput.close();

                    /*
                    if (!handleFile(targetFile)) {
                        Timber.d("ERROR DURING FILE PROCESSING");
                        return;
                    }
                    */

                    Timber.d("SAVED DOWNLOAD TO " + targetFile);
                } catch (ConnectTimeoutException cte) {
                    Timber.e("FAILED TO SAVE DOWNLOAD TO " + fileName + " (CONNECTION EXCEPTION)");
                    cte.printStackTrace();
                } catch (SocketTimeoutException ste) {
                    Timber.e("FAILED TO SAVE DOWNLOAD TO " + fileName + " (SOCKET EXCEPTION)");
                    ste.printStackTrace();
                } catch (IOException ioe) {
                    Timber.e("FAILED TO SAVE DOWNLOAD TO " + fileName + " (IO EXCEPTION)");
                    ioe.printStackTrace();
                }

                // remove from queue here, regardless of success
                QueueManager.removeFromQueue(context, queueId);

                // remove notification, regardless of success
                nManager.cancel(nTag, nId);

                // handle file here, regardless of success
                // (assumes .tmp file will exist if download is interrupted)
                if (!handleFile(targetFile)) {
                    Timber.e("ERROR DURING FILE PROCESSING FOR " + fileName);
                }
            } else {
                Timber.e("DOWNLOAD FAILED FOR " + fileName + ", STATUS CODE: " + statusCode);

                QueueManager.checkQueueFinished(context, targetFile.getName());
            }

            // clean up connection
            EntityUtils.consume(entity);
            request.abort();
            request.releaseConnection();

        } catch (IOException ioe) {
            Timber.e("DOWNLOAD FAILED FOR " + fileName + ", EXCEPTION THROWN");
            ioe.printStackTrace();

            QueueManager.checkQueueFinished(context, targetFile.getName());
        }
    }

    private synchronized StrongHttpsClient getHttpClientInstance() {
        if (mClient == null) {
            try {

                mClient = new StrongHttpsClient(context,R.raw.debiancacerts,null);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return mClient;
    }

    private void downloadWithManager(Uri uri, String title, String desc, Uri uriFile) {
        initDownloadManager();

        Timber.d("QUEUEING DOWNLOAD: " + uri.toString() + " -> " + uriFile.toString());

        initReceivers();

        DownloadManager.Request request = new DownloadManager.Request(uri)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(title)
                .setDescription(desc)
                .setVisibleInDownloadsUi(false)
                .setDestinationUri(uriFile);

        lastDownload = dManager.enqueue(request);

        // have to enqueue first to get manager id
        String uriString = uriFile.toString();
        QueueManager.addToQueue(context, Long.valueOf(lastDownload), uriString.substring(uriString.lastIndexOf("/") + 1));
    }

    private synchronized void initDownloadManager() {
        if (dManager == null) {
            dManager = (DownloadManager)context.getSystemService(context.DOWNLOAD_SERVICE);
        }
    }

    private synchronized void initNotificationManager() {
        if (nManager == null) {
            nManager = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
        }
    }

    private synchronized void initReceivers() {

        String fileName = ZipHelper.getExpansionZipFilename(context, mainOrPatch, version);

        FilteredBroadcastReceiver onComplete = new FilteredBroadcastReceiver(fileName);
        BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                // ???
            }
        };

        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        context.registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    private class FilteredBroadcastReceiver extends BroadcastReceiver {

        public String fileFilter;
        public boolean fileReceived = false;

        public FilteredBroadcastReceiver(String fileFilter) {
            this.fileFilter = fileFilter;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor c = dManager.query(query);
                try {
                    if (c.moveToFirst()) {

                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                            File savedFile = new File(Uri.parse(uriString).getPath());
                            Timber.d("PROCESSING DOWNLOADED FILE " + savedFile.getPath());

                            File fileCheck = new File(savedFile.getPath().substring(0, savedFile.getPath().lastIndexOf(".")));

                            if (fileReceived) {
                                Timber.d("GOT FILE " + fileCheck.getName() + " BUT THIS RECEIVER HAS ALREADY PROCESSED A FILE");
                                return;
                            } else if (!fileCheck.getName().equals(fileFilter)) {
                                Timber.d("GOT FILE " + fileCheck.getName() + " BUT THIS RECEIVER IS FOR " + fileFilter);
                                return;
                            } else {
                                Timber.d("GOT FILE " + fileCheck.getName() + " AND THIS RECEIVER IS FOR " + fileFilter + ", PROCESSING...");
                                fileReceived = true;
                            }

                            QueueManager.removeFromQueue(context, Long.valueOf(downloadId));

                            Timber.d("DOWNLOAD COMPLETE, REMOVING FROM QUEUE: " + downloadId);

                            if (!handleFile(savedFile)) {
                                Timber.e("ERROR DURING FILE PROCESSING FOR " + fileCheck.getName());

                            } else {
                                Timber.e("FILE PROCESSING COMPLETE FOR " + fileCheck.getName());
                            }
                        } else {

                            // COLUMN_LOCAL_URI seems to be null if download fails
                            // COLUMN_URI is the download url, not the .tmp file path
                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
                            String uriName = uriString.substring(uriString.lastIndexOf("/"));

                            String filePath = ZipHelper.getExpansionZipDirectory(context, mainOrPatch, version);

                            File savedFile = new File(filePath, uriName + ".tmp");
                            Timber.d("PROCESSING DOWNLOADED FILE " + savedFile.getPath());

                            File fileCheck = new File(savedFile.getPath().substring(0, savedFile.getPath().lastIndexOf(".")));

                            if (fileReceived) {
                                Timber.d("GOT FILE " + fileCheck.getName() + " BUT THIS RECEIVER HAS ALREADY PROCESSED A FILE");
                                return;
                            } else if (!fileCheck.getName().equals(fileFilter)) {
                                Timber.d("GOT FILE " + fileCheck.getName() + " BUT THIS RECEIVER IS FOR " + fileFilter);
                                return;
                            } else {
                                Timber.d("GOT FILE " + fileCheck.getName() + " AND THIS RECEIVER IS FOR " + fileFilter + ", PROCESSING...");
                                fileReceived = true;
                            }

                            String status;
                            boolean willResume = true;

                            // improve feedback
                            if (DownloadManager.STATUS_RUNNING == c.getInt(columnIndex)) {
                                status = "RUNNING";
                            } else if (DownloadManager.STATUS_PENDING == c.getInt(columnIndex)) {
                                status = "PENDING";
                            } else if (DownloadManager.STATUS_PAUSED == c.getInt(columnIndex)) {
                                status = "PAUSED";
                            } else if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)) {
                                status = "FAILED";
                                willResume = false;
                            } else {
                                status = "UNKNOWN";
                                willResume = false;
                            }

                            Timber.e("MANAGER FAILED AT STATUS CHECK, STATUS IS " + status);

                            if (willResume) {
                                Timber.e("STATUS IS " + status + ", LEAVING QUEUE/FILES AS-IS FOR MANAGER TO HANDLE");
                            } else {
                                Timber.e("STATUS IS " + status + ", CLEANING UP QUEUE/FILES, MANAGER WILL NOT RESUME");

                                Timber.d("DOWNLOAD STOPPED, REMOVING FROM QUEUE: " + downloadId);

                                QueueManager.removeFromQueue(context, Long.valueOf(downloadId));

                                if (!handleFile(savedFile)) {
                                    Timber.e("ERROR DURING FILE PROCESSING FOR " + fileCheck.getName());
                                } else {
                                    Timber.e("FILE PROCESSING COMPLETE FOR " + fileCheck.getName());
                                }
                            }
                        }
                    } else {
                        Timber.e("MANAGER FAILED AT QUERY");
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            } else {
                Timber.e("MANAGER FAILED AT COMPLETION CHECK");
            }

            // once this has done its job, make it go away
            context.unregisterReceiver(this);
        }
    }

    private boolean handleFile (File tempFile) {

        File actualFile = new File(tempFile.getPath().substring(0, tempFile.getPath().lastIndexOf(".")));
        Timber.d("ACTUAL FILE: " + actualFile.getAbsolutePath());

        // additional error checking
        if (tempFile.exists()) {
            if (tempFile.length() == 0) {
                Timber.e("FINISHED DOWNLOAD OF " + tempFile.getPath() + " BUT IT IS A ZERO BYTE FILE");
                return false;
            } else {
                Timber.d("FINISHED DOWNLOAD OF " + tempFile.getPath() + " AND FILE LOOKS OK");
            }
        } else {
            Timber.e("FINISHED DOWNLOAD OF " + tempFile.getPath() + " BUT IT DOES NOT EXIST");
            return false;
        }

        try {
            // clean up old obbs before renaming new file
            File directory = new File(actualFile.getParent());

            String nameFilter = "";
            if (actualFile.getName().startsWith(Constants.MAIN)) {
                nameFilter = nameFilter + Constants.MAIN + ".*." + context.getPackageName() + ".obb";
            }
            if (actualFile.getName().startsWith(Constants.PATCH)) {
                nameFilter = nameFilter + Constants.PATCH + ".*." + context.getPackageName() + ".obb";
            }

            if (nameFilter.length() == 0) {
                Timber.d("CLEANUP: DON'T KNOW HOW TO BUILD WILDCARD FILTER BASED ON " + actualFile.getName());
            } else {
                Timber.d("CLEANUP: DELETING " + nameFilter + " FROM " + directory.getPath());
            }

            WildcardFileFilter oldFileFilter = new WildcardFileFilter(nameFilter);
            for (File oldFile : FileUtils.listFiles(directory, oldFileFilter, null)) {
                Timber.d("CLEANUP: FOUND " + oldFile.getPath() + ", DELETING");
                FileUtils.deleteQuietly(oldFile);
            }

            FileUtils.moveFile(tempFile, actualFile); // moved to commons-io from using exec and mv because we were getting 0kb obb files on some devices
            FileUtils.deleteQuietly(tempFile); // for some reason I was getting an 0kb .tmp file lingereing
            Timber.d("MOVED TEMP FILE " + tempFile.getPath() + " TO " + actualFile.getPath());
        } catch (IOException ioe) {
            Timber.e("ERROR DURING CLEANUP/MOVING TEMP FILE: " + ioe.getMessage());
            return false;
        }

        // download finished, must clear ZipHelper cache
        ZipHelper.clearCache();

        return true;
    }

    private class LigerCallback implements LicenseCheckerCallback {

        @Override
        public void allow(int reason) {
            Timber.d("LICENSE CHECK ALLOWED, DOWNLOADING FROM GOOGLE PLAY");

            String ligerUrl = null;
            String ligerObb = null;

            int count = ligerPolicy.getExpansionURLCount();
            if (mainOrPatch.equals(Constants.MAIN)) {
                if (count < 1) {
                    Timber.e("LOOKING FOR MAIN FILE BUT URL COUNT IS " + count + ", DOWNLOADING FROM LIGER SERVER");
                    downloadFromLigerServer();
                    return;
                } else {
                    ligerUrl = ligerPolicy.getExpansionURL(APKExpansionPolicy.MAIN_FILE_URL_INDEX);
                    ligerObb = ligerPolicy.getExpansionFileName(APKExpansionPolicy.MAIN_FILE_URL_INDEX);
                }
            }
            if (mainOrPatch.equals(Constants.PATCH)) {
                if (count < 2) {
                    Timber.e("LOOKING FOR PATCH FILE BUT URL COUNT IS " + count + ", DOWNLOADING FROM LIGER SERVER");
                    downloadFromLigerServer();
                    return;
                } else {
                    ligerUrl = ligerPolicy.getExpansionURL(APKExpansionPolicy.PATCH_FILE_URL_INDEX);
                    ligerObb = ligerPolicy.getExpansionFileName(APKExpansionPolicy.PATCH_FILE_URL_INDEX);
                }
            }

            // if we're managing the download, download only to the files folder
            // if we're using the google play api, download only to the obb folder
            File targetFolder = new File(ZipHelper.getObbFolderName(context));

            Timber.d("TARGET FOLDER: " + targetFolder.getPath());

            Timber.d("TARGET URL: " + ligerUrl);

            // clean up old tmps before downloading

            String nameFilter = "";
            if (ligerObb.startsWith(Constants.MAIN)) {
                nameFilter = nameFilter + Constants.MAIN + ".*." + context.getPackageName() + ".*.tmp";
            }
            if (ligerObb.startsWith(Constants.PATCH)) {
                nameFilter = nameFilter + Constants.PATCH + ".*." + context.getPackageName() + ".*.tmp";
            }

            if (nameFilter.length() == 0) {
                Timber.d("CLEANUP: DON'T KNOW HOW TO BUILD WILDCARD FILTER BASED ON " + ligerObb);
            } else {
                Timber.d("CLEANUP: DELETING " + nameFilter + " FROM " + targetFolder.getPath());
            }

            WildcardFileFilter oldFileFilter = new WildcardFileFilter(nameFilter);
            for (File oldFile : FileUtils.listFiles(targetFolder, oldFileFilter, null)) {
                Timber.d("CLEANUP: FOUND " + oldFile.getPath() + ", DELETING");
                FileUtils.deleteQuietly(oldFile);
            }

            File targetFile = new File(targetFolder, ligerObb + ".tmp");

            // if there is no connectivity, do not queue item (no longer seems to pause if connection is unavailable)
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if ((ni != null) && (ni.isConnectedOrConnecting())) {

                if (context instanceof Activity) {
                    Utility.toastOnUiThread((Activity) context, "Starting download of " + mainOrPatch + " expansion file.", false); // FIXME move to strings
                }

                // TODO: revisit this check, if the intent is to download from google play, we probably can't use tor

                // check preferences.  will also need to check whether tor is active within method
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                boolean useTor = settings.getBoolean("pusetor", false);
                boolean useManager = settings.getBoolean("pusedownloadmanager", false);

                //if (checkTor(useTor, context)) {
                if (useTor && useManager) {
                    Timber.e("ANDROID DOWNLOAD MANAGER IS NOT COMPATABLE WITH TOR");

                    if (context instanceof Activity) {
                        Utility.toastOnUiThread((Activity) context, "Check settings, can't use download manager and tor", true); // FIXME move to strings
                    }

                } else if (useTor || !useManager) {
                    downloadWithTor(useTor, Uri.parse(ligerUrl), mAppTitle + " " + mainOrPatch + " file download", ligerObb, targetFile);
                } else {
                    downloadWithManager(Uri.parse(ligerUrl), mAppTitle + " " + mainOrPatch + " file download", ligerObb, Uri.fromFile(targetFile));
                }

            } else {
                Timber.d("NO CONNECTION, NOT QUEUEING DOWNLOAD: " + ligerUrl + ligerObb + " -> " + targetFile.getPath());
            }
        }

        @Override
        public void dontAllow(int reason) {
            Timber.d("LICENSE CHECK NOT ALLOWED, DOWNLOADING FROM LIGER SERVER");
            downloadFromLigerServer();
        }

        @Override
        public void applicationError(int errorCode) {
            // if your app or version is not managed by google play the result appears
            // to be an application error (code 3?) rather than "do not allow"
            Timber.d("LICENSE CHECK ERROR CODE " + errorCode + ", DOWNLOADING FROM LIGER SERVER");
            downloadFromLigerServer();
        }
    }
}
