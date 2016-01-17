package scal.io.liger.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mnbogner on 11/24/14.
 */
public class ExpansionIndexItem extends BaseIndexItem implements Comparable {

    // required
    String packageName;
    String expansionId;
    String patchOrder;
    String contentType;
    String expansionFileVersion;
    String expansionFilePath; // relative to Environment.getExternalStorageDirectory() <- need to shift to user-specified directory
    String expansionFileUrl;

    //db v2 stuff
    int autoincrementingId;
    java.util.Date creationDate;
    java.util.Date lastModifiedDate;
    java.util.Date lastOpenedDate;
    int sortOrder;

    // not optional, but need to handle nulls
    long expansionFileSize;
    String expansionFileChecksum;

    // patch stuff, optional
    String patchFileVersion;
    long patchFileSize;
    String patchFileChecksum;

    // optional
    String author;
    String website;
    //String date; // FIXME remove this as its unclear waht its for.  we should probably have dateCreated and dateInstalled or dateDownloaded or something to show when it hit this phone
    String dateUpdated;
    ArrayList<String> languages;
    ArrayList<String> tags;
    HashMap<String, String> extras;

    public ExpansionIndexItem() {

    }

    public ExpansionIndexItem(scal.io.liger.model.sqlbrite.ExpansionIndexItem eii) {
        this.packageName = eii.packageName;
        this.expansionId = eii.expansionId;
        this.sortOrder = eii.sortOrder;
        this.patchOrder = eii.patchOrder;
        this.contentType = eii.contentType;
        this.expansionFileVersion = eii.expansionFileVersion;
        this.expansionFilePath = eii.expansionFilePath;
        this.expansionFileUrl = eii.expansionFileUrl;
    }

    public ExpansionIndexItem(String packageName, String expansionId, int sortOrder, String patchOrder, String contentType, String expansionFileVersion, String expansionFilePath, String expansionFileUrl, String expansionThumbnail) {
        this.packageName = packageName;
        this.expansionId = expansionId;
        this.sortOrder = sortOrder;
        this.patchOrder = patchOrder;
        this.contentType = contentType;
        // this.expansionFileName = expansionFileName;
        this.expansionFileVersion = expansionFileVersion;
        this.expansionFilePath = expansionFilePath;
        this.expansionFileUrl = expansionFileUrl;
        this.thumbnailPath = expansionThumbnail;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getExpansionId() {
        return expansionId;
    }

    public void setExpansionId(String expansionId) {
        this.expansionId = expansionId;
    }

    public int getAutoincrementingId() { return autoincrementingId; }

    public java.util.Date getCreationDate() { return creationDate; }

    public void setCreationDate(java.util.Date creationDate) { this.creationDate = creationDate; }

    public java.util.Date getLastModifiedDate() { return lastModifiedDate; }

    public void setLastModifiedDate(java.util.Date lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public java.util.Date getLastOpenedDate() { return lastOpenedDate; }

    public void setLastOpenedDate(java.util.Date lastOpenedDate) { this.lastOpenedDate = lastOpenedDate; }

    public int getSortOrder() { return sortOrder; }

    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getPatchOrder() {
        return patchOrder;
    }

    public void setPatchOrder(String patchOrder) {
        this.patchOrder = patchOrder;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExpansionFileVersion() {
        return expansionFileVersion;
    }

    public void setExpansionFileVersion(String expansionFileVersion) {
        this.expansionFileVersion = expansionFileVersion;
    }

    public String getExpansionFilePath() {
        return expansionFilePath;
    }

    public void setExpansionFilePath(String expansionFilePath) {
        this.expansionFilePath = expansionFilePath;
    }

    public String getExpansionFileUrl() {
        return expansionFileUrl;
    }

    public void setExpansionFileUrl(String expansionFileUrl) {
        this.expansionFileUrl = expansionFileUrl;
    }

    public long getExpansionFileSize() {
        return expansionFileSize;
    }

    public void setExpansionFileSize(long expansionFileSize) {
        this.expansionFileSize = expansionFileSize;
    }

    public String getExpansionFileChecksum() {
        return expansionFileChecksum;
    }

    public void setExpansionFileChecksum(String expansionFileChecksum) {
        this.expansionFileChecksum = expansionFileChecksum;
    }

    public String getPatchFileVersion() {
        return patchFileVersion;
    }

    public void setPatchFileVersion(String patchFileVersion) {
        this.patchFileVersion = patchFileVersion;
    }

    public long getPatchFileSize() {
        return patchFileSize;
    }

    public void setPatchFileSize(long patchFileSize) {
        this.patchFileSize = patchFileSize;
    }

    public String getPatchFileChecksum() {
        return patchFileChecksum;
    }

    public void setPatchFileChecksum(String patchFileChecksum) {
        this.patchFileChecksum = patchFileChecksum;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

//    public String getDate() {
//        return date;
//    }
//
//    public void setDate(String date) {
//        this.date = date;
//    }

    public ArrayList<String> getLanguages() {
        return languages;
    }

    public void setLanguages(ArrayList<String> languages) {
        this.languages = languages;
    }

    public void addLanguage(String language) {
        if (this.languages == null) {
            this.languages = new ArrayList<String>();
        }

        this.languages.add(language);
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<String>();
        }

        this.tags.add(tag);
    }

    public HashMap<String, String> getExtras() {
        return extras;
    }

    public void setExtras(HashMap<String, String> extras) {
        this.extras = extras;
    }

    public void addExtra(String key, String value) {
        if (this.extras == null) {
            this.extras = new HashMap<String, String>();
        }

        this.extras.put(key, value);
    }

    public void removeExtra(String key)
    {
        if (this.extras != null) {
            this.extras.remove(key);
        }
    }

    public String getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        if (another instanceof InstanceIndexItem) {
            //Timber.d(title + " COMPARED TO INSTANCE ITEM: -1");
            return -1; // should always appear below instance index items
        } else if (another instanceof ExpansionIndexItem){

            // if this date is later or null, appear below
            // -1

            // if that date is later or null, appear above
            // 1

            if (dateUpdated == null) {
                //Timber.d(title + " HAS NO DATE: -1");
                return -1;
            }

            if (((ExpansionIndexItem)another).getDateUpdated() == null) {
                //Timber.d(title + " HAS A DATE BUT " + ((ExpansionIndexItem)another).getTitle() + " DOES NOT: 1");
                return 1;
            }

            //Timber.d("COMPARING DATE OF " + title + " TO DATE OF " + ((ExpansionIndexItem)another).getTitle() + ": " + dateUpdated.compareTo(((ExpansionIndexItem)another).getDateUpdated()));
            return dateUpdated.compareTo(((ExpansionIndexItem)another).getDateUpdated());

        } else {
            //Timber.d(title + " HAS NO POINT OF COMPARISON: 0");
            return 0; // otherwise don't care
        }
    }

    public float getMainPercentComplete(long currentSize) {
        return 0;
    }

    public float getPatchPercentComplete(long currentSize) {
        return 0;
    }
}