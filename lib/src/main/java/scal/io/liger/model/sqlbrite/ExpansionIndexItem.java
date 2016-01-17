package scal.io.liger.model.sqlbrite;

import scal.io.liger.MainActivity;
import scal.io.liger.StorageHelper;
import timber.log.Timber;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.hannesdorfmann.sqlbrite.objectmapper.annotation.Column;
import com.hannesdorfmann.sqlbrite.objectmapper.annotation.ObjectMappable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import scal.io.liger.Constants;

/**
 * Created by mnbogner on 8/20/15.
 */

@ObjectMappable
public class ExpansionIndexItem extends BaseIndexItem {

    public static final String COLUMN_PACKAGENAME = "packageName";
    public static final String COLUMN_EXPANSIONID = "expansionId";
    public static final String COLUMN_PATCHORDER = "patchOrder";
    public static final String COLUMN_CONTENTTYPE = "contentType";
    public static final String COLUMN_EXPANSIONFILEURL = "expansionFileUrl";
    public static final String COLUMN_EXPANSIONFILEPATH = "expansionFilePath";
    public static final String COLUMN_EXPANSIONFILEVERSION = "expansionFileVersion";
    public static final String COLUMN_EXPANSIONFILESIZE = "expansionFileSize";
    public static final String COLUMN_EXPANSIONFILECHECKSUM = "expansionFileChecksum";
    public static final String COLUMN_PATCHFILEVERSION = "patchFileVersion";
    public static final String COLUMN_PATCHFILESIZE = "patchFileSize";
    public static final String COLUMN_PATCHFILECHECKSUM = "patchFileChecksum";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_WEBSITE = "website";
    public static final String COLUMN_DATEUPDATED = "dateUpdated";
    public static final String COLUMN_LANGUAGES = "languages";
    public static final String COLUMN_TAGS = "tags";
    public static final String COLUMN_INSTALLEDFLAG = "installedFlag";
    public static final String COLUMN_MAINDOWNLOADFLAG = "mainDownloadFlag";
    public static final String COLUMN_PATCHDOWNLOADFLAG = "patchDownloadFlag";


    // required
    @Column(COLUMN_PACKAGENAME) public String packageName;
    @Column(COLUMN_EXPANSIONID) public String expansionId;
    @Column(COLUMN_PATCHORDER) public String patchOrder;
    @Column(COLUMN_CONTENTTYPE) public String contentType;
    @Column(COLUMN_EXPANSIONFILEURL) public String expansionFileUrl;
    @Column(COLUMN_EXPANSIONFILEPATH) public String expansionFilePath; // relative to Context.getExternalFilesDirs()

    // not optional, but need to handle nulls
    @Column(COLUMN_EXPANSIONFILEVERSION) public String expansionFileVersion;
    @Column(COLUMN_EXPANSIONFILESIZE) public long expansionFileSize;
    @Column(COLUMN_EXPANSIONFILECHECKSUM) public String expansionFileChecksum;

    // patch stuff, optional
    @Column(COLUMN_PATCHFILEVERSION) public String patchFileVersion;
    @Column(COLUMN_PATCHFILESIZE) public long patchFileSize;
    @Column(COLUMN_PATCHFILECHECKSUM) public String patchFileChecksum;

    // optional
    @Column(COLUMN_AUTHOR) public String author;
    @Column(COLUMN_WEBSITE) public String website;
    @Column(COLUMN_DATEUPDATED) public String dateUpdated;
    @Column(COLUMN_LANGUAGES) public String languages; // comma-delimited list, need access methods that will construct an ArrayList<String>
    @Column(COLUMN_TAGS) public String tags; // comma-delimited list, need access methods that will construct an ArrayList<String>

    // HashMap<String, String> extras; <- dropping this, don't know a good way to handle hash maps

    // for internal use
    @Column(COLUMN_INSTALLEDFLAG) public int installedFlag;
    @Column(COLUMN_MAINDOWNLOADFLAG) public int mainDownloadFlag;
    @Column(COLUMN_PATCHDOWNLOADFLAG) public int patchDownloadFlag;


    public ExpansionIndexItem() {
        super();

    }

    public ExpansionIndexItem(long id, String title, String description, String thumbnailPath, String packageName, String expansionId, String patchOrder, String contentType, String expansionFileUrl, String expansionFilePath, String expansionFileVersion, long expansionFileSize, String expansionFileChecksum, String patchFileVersion, long patchFileSize, String patchFileChecksum, String author, String website, String dateUpdated, String languages, String tags, int installedFlag, int mainDownloadFlag, int patchDownloadFlag) {
        super(id, title, description, thumbnailPath);
        this.packageName = packageName;
        this.expansionId = expansionId;
        this.patchOrder = patchOrder;
        this.contentType = contentType;
        this.expansionFileUrl = expansionFileUrl;
        this.expansionFilePath = expansionFilePath;
        this.expansionFileVersion = expansionFileVersion;
        this.expansionFileSize = expansionFileSize;
        this.expansionFileChecksum = expansionFileChecksum;
        this.patchFileVersion = patchFileVersion;
        this.patchFileSize = patchFileSize;
        this.patchFileChecksum = patchFileChecksum;
        this.author = author;
        this.website = website;
        this.dateUpdated = dateUpdated;
        this.languages = languages;
        this.tags = tags;
        this.installedFlag = installedFlag;
        this.mainDownloadFlag = mainDownloadFlag;
        this.patchDownloadFlag = patchDownloadFlag;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getExpansionId() {
        return expansionId;
    }

    public String getPatchOrder() {
        return patchOrder;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExpansionFileUrl() {
        return expansionFileUrl;
    }

    public String getExpansionFilePath() {
        return expansionFilePath;
    }

    public String getExpansionFileVersion() {
        return expansionFileVersion;
    }

    public long getExpansionFileSize() {
        return expansionFileSize;
    }

    public String getExpansionFileChecksum() {
        return expansionFileChecksum;
    }

    public String getPatchFileVersion() {
        return patchFileVersion;
    }

    public long getPatchFileSize() {
        return patchFileSize;
    }

    public String getPatchFileChecksum() {
        return patchFileChecksum;
    }

    public String getAuthor() {
        return author;
    }

    public String getWebsite() {
        return website;
    }

    public String getDateUpdated() {
        return dateUpdated;
    }

    public String getLanguages() {
        return languages;
    }

    public String getTags() {
        return tags;
    }

    public int getInstalledFlag() {
        return installedFlag;
    }

    public int getMainDownloadFlag() {
        return mainDownloadFlag;
    }

    public int getPatchDownloadFlag() {
        return patchDownloadFlag;
    }

    // sqlite doesn't support boolean columns, so provide an interface to fake it

    public void setInstalledFlag(boolean installedFlag) {
        if (installedFlag) {
            this.installedFlag = 1;
        } else {
            this.installedFlag = 0;
        }
    }

    public boolean isInstalled() {
        if (installedFlag > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setMainDownloadFlag(boolean mainDownloadFlag) {
        if (mainDownloadFlag) {
            this.mainDownloadFlag = 1;
        } else {
            this.mainDownloadFlag = 0;
        }
    }

    public boolean isDownloadingMain() {
        if (mainDownloadFlag > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setPatchDownloadFlag(boolean patchDownloadFlag) {
        if (patchDownloadFlag) {
            this.patchDownloadFlag = 1;
        } else {
            this.patchDownloadFlag = 0;
        }
    }

    public boolean isDownloadingPatch() {
        if (patchDownloadFlag > 0) {
            return true;
        } else {
            return false;
        }
    }

    // methods added for convenience
    public void setDownloadFlag(boolean downloadFlag, String fileName) {

        Timber.d("SETTING FLAG FOR " + fileName + " TO " + downloadFlag);

        if (fileName.contains(Constants.MAIN)) {
            setMainDownloadFlag(downloadFlag);
        } else if (fileName.contains(Constants.PATCH)) {
            setPatchDownloadFlag(downloadFlag);
        } else {
            Timber.e("CANNOT SET DOWNLOAD FLAG STATE FOR " + fileName);
        }
    }

    public boolean isDownloading(String fileName) {
        if (fileName.contains(Constants.MAIN)) {
            return isDownloadingMain();
        } else if (fileName.contains(Constants.PATCH)) {
            return isDownloadingPatch();
        } else {
            Timber.e("CANNOT DETERMINE DOWNLOAD FLAG STATE FOR " + fileName);
            return false;
        }
    }

    public void update(scal.io.liger.model.ExpansionIndexItem item) {

        // update db item with values from available index item

        // leave id alone

        this.title = item.getTitle();
        this.description = item.getDescription();
        this.thumbnailPath = item.getThumbnailPath();
        this.packageName = item.getPackageName();
        this.expansionId = item.getExpansionId();
        this.patchOrder = item.getPatchOrder();
        this.contentType = item.getContentType();
        this.expansionFileUrl = item.getExpansionFileUrl();
        this.expansionFilePath = item.getExpansionFilePath();
        this.expansionFileVersion = item.getExpansionFileVersion();
        this.expansionFileSize = item.getExpansionFileSize();
        this.expansionFileChecksum = item.getExpansionFileChecksum();
        this.patchFileVersion = item.getPatchFileVersion();
        this.patchFileSize = item.getPatchFileSize();
        this.patchFileChecksum = item.getPatchFileChecksum();
        this.author = item.getAuthor();
        this.website = item.getWebsite();
        this.dateUpdated = item.getDateUpdated();

        String languageString = null;
        String tagString = null;

        if (item.getLanguages() != null) {
            languageString = item.getLanguages().toString();
            Timber.d("WHAT DOES THIS LOOK LIKE? " + languageString);
        }
        if (item.getTags() != null) {
            tagString = item.getTags().toString();
            Timber.d("WHAT DOES THIS LOOK LIKE? " + tagString);
        }

        this.languages = languageString;
        this.tags = tagString;

        // leave installed flag alone
        // leave download flags alone

    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setExpansionId(String expansionId) {
        this.expansionId = expansionId;
    }

    public void setPatchOrder(String patchOrder) {
        this.patchOrder = patchOrder;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setExpansionFileUrl(String expansionFileUrl) {
        this.expansionFileUrl = expansionFileUrl;
    }

    public void setExpansionFilePath(String expansionFilePath) {
        this.expansionFilePath = expansionFilePath;
    }

    public void setExpansionFileVersion(String expansionFileVersion) {
        this.expansionFileVersion = expansionFileVersion;
    }

    public void setExpansionFileSize(long expansionFileSize) {
        this.expansionFileSize = expansionFileSize;
    }

    public void setExpansionFileChecksum(String expansionFileChecksum) {
        this.expansionFileChecksum = expansionFileChecksum;
    }

    public void setPatchFileVersion(String patchFileVersion) {
        this.patchFileVersion = patchFileVersion;
    }

    public void setPatchFileSize(long patchFileSize) {
        this.patchFileSize = patchFileSize;
    }

    public void setPatchFileChecksum(String patchFileChecksum) {
        this.patchFileChecksum = patchFileChecksum;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public long getLastModifiedTime() {

        // questionable solution to the lack of context

        Application app = null;

        try {
            app =  (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
        } catch (IllegalAccessException e) {
            Timber.e("compare - illegal access");
        } catch (InvocationTargetException e) {
            Timber.e("compare - invocation target");
        } catch (NoSuchMethodException e) {
            Timber.e("compare - no such method");
        } catch (ClassNotFoundException e) {
            Timber.e("compare - class not found");
        }

        if (app != null) {
            String filePath = StorageHelper.getActualStorageDirectory(app.getApplicationContext()).getPath();
            if (filePath != null) {
                String fileName = getExpansionId() + "." + scal.io.liger.Constants.MAIN + "." + getExpansionFileVersion() + ".obb";
                File expansionFile = new File(filePath + File.separator + fileName);
                if (expansionFile.exists()) {
                    // Timber.d("compare - got date for file: " + expansionFile.getPath());
                    return expansionFile.lastModified();
                } else {
                    // Timber.e("compare - file doesn't exist: " + expansionFile.getPath());
                }
            } else {
                Timber.e("compare - file path is null");
            }
        } else {
            Timber.e("compare - application context is null");
        }

        return 0;
    }

    // move comparison down into specific classes
    @Override
    public int compareTo(@NonNull Object another) {
        return super.compareTo(another);
    }
}
