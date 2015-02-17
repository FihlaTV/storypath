package scal.io.liger.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

/**
 * Created by josh on 2/13/15.
 */
public class AudioClip implements Parcelable {
    @Expose private String positionClipId; // can be null if unused.  card id we are linked to either this or the next must have a value, but only one
    @Expose private String uuid; // key to mediaFiles map in StoryModel
    @Expose private int positionIndex; // can be -1 if unused.
    @Expose private float volume; // 1.0 is full volume
    @Expose private int clipSpan;  // how many clips it should try to span
    @Expose private boolean truncate; // should this play out past the clips its spans, or trim its end to match
    @Expose private boolean overlap; // if overlap the next clip or push it out, can we
    @Expose private boolean fillRepeat;  // repeat to fill if this audioclip is shorter than the clips it spans

    public AudioClip(String positionClipId, int positionIndex, float volume, int clipSpan, boolean truncate, boolean overlap, boolean fillRepeat, String uuid) {
        this.positionClipId = positionClipId;
        this.positionIndex = positionIndex;
        this.volume = volume;
        this.clipSpan = clipSpan;
        this.truncate = truncate;
        this.overlap = overlap;
        this.fillRepeat = fillRepeat;
        this.uuid = uuid;
    }

    /**
     * @return the uuid used to retrieve the corresponding audio MediaFile
     * from {@link scal.io.liger.model.StoryPathLibrary#mediaFiles}
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return whether this audio should repeat to fill clips it spans.
     * as defined by {@link #getClipSpan()} and {@link #getPositionClipId()} or {@link #getPositionIndex()}
     */
    public boolean doFillRepeat() {
        return fillRepeat;
    }

    /**
     * @return whether this audio should be allowed to overlap with adjacent clips.
     * Used if {@link #doTruncate()} is false.
     * e.g: If this audio extends beyond the clips it spans, should the audio
     * continue against no video / photo, or be mixed into the next Clip.
     */
    public boolean doOverlap() {
        return overlap;
    }

    /**
     * @return whether this audio should be truncated
     * to have duration no longer than the clips it spans.
     */
    public boolean doTruncate() {
        return truncate;
    }

    /**
     * @return how many ClipCards this audio should span, assuming
     * it's length allows
     */
    public int getClipSpan() {
        return clipSpan;
    }

    public void setClipSpan(int newSpan) {
        clipSpan = newSpan;
    }

    public float getVolume() {
        return volume;
    }

    /**
     * @return the index of the starting ClipCard within the StoryPath's ClipCards.
     * For a convenience method to quickly find the first ClipCard see
     * {@link scal.io.liger.model.StoryPathLibrary#getFirstClipCardForAudioClip(AudioClip, java.util.List)}
     */
    public int getPositionIndex() {
        return positionIndex;
    }

    /**
     * Assign a new value for the starting ClipCard position. This will
     * unset any value passed to {@link #setPositionClipId(String)}
     */
    public void setPositionIndex(int newIndex) {
        positionIndex = newIndex;
        positionClipId = null;
    }

    /**
     * @return The id identifying the starting ClipCard in a StoryPathLibrary
     * For a convenience method to quickly find the first ClipCard see
     * {@link scal.io.liger.model.StoryPathLibrary#getFirstClipCardForAudioClip(AudioClip, java.util.List)}
     */
    public String getPositionClipId() {
        return positionClipId;
    }

    /**
     * Assign a new value for the starting ClipCard Id. This will
     * unset any value passed to {@link #setPositionIndex(int)}
     */
    public void setPositionClipId(String newClipId) {
        positionClipId = newClipId;
        positionIndex= -1;
    }

    // TODO this is a cleaner form of parcelable: http://www.parcelabler.com/

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        String[] data = new String[7];
        out.writeStringArray(new String[]{
                this.positionClipId,
                "" + this.positionIndex,
                "" + this.volume,
                "" + this.clipSpan,
                (this.truncate ? "1" : "0"),
                (this.overlap ? "1" : "0"),
                (this.fillRepeat ? "1" : "0")
        });
    }



    public AudioClip(Parcel in) {
        String[] data = new String[7];

        in.readStringArray(data);

        this.positionClipId = data[0];
        this.positionIndex = Integer.parseInt(data[1]);
        this.volume = Float.parseFloat(data[2]);
        this.clipSpan = Integer.parseInt(data[3]);
        this.truncate = data[4].equals("1");
        this.overlap = data[5].equals("1");
        this.fillRepeat = data[6].equals("1");
    }


    public static final Parcelable.Creator<AudioClip> CREATOR = new Parcelable.Creator<AudioClip>() {
        public AudioClip createFromParcel(Parcel in) {
            return new AudioClip(in);
        }

        public AudioClip[] newArray(int size) {
            return new AudioClip[size];
        }
    };

//    public AudioClip() {}
}
