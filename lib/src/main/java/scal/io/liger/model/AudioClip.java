package scal.io.liger.model;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.UUID;

/**
 * Created by josh on 2/13/15.
 */
public class AudioClip {
    @Expose private String position_clip_id; // can be null if unused.  card id we are linked to either this or the next must have a value, but only one
    @Expose private int position_index; // can be -1 if unused.

    public AudioClip(String position_clip_id, int position_index, float volume, int clip_span, boolean truncate, boolean overlap, boolean fill_repeat, String uuid) {
        this.position_clip_id = position_clip_id;
        this.position_index = position_index;
        this.volume = volume;
        this.clip_span = clip_span;
        this.truncate = truncate;
        this.overlap = overlap;
        this.fill_repeat = fill_repeat;
        this.uuid = uuid;
    }

    @Expose private float volume; // 1.0 is full volume
    @Expose private int clip_span;  // how many clips it should try to span
    @Expose private boolean truncate; // should this play out past the clips its spans, or trim its end to match
    @Expose private boolean overlap; // if overlap the next clip or push it out, can we
    @Expose private boolean fill_repeat;  // repeat to fill if this audioclip is shorter than the clips it spans
    @Expose private String uuid; // key to mediaFiles map in StoryModel

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
        return fill_repeat;
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
        return clip_span;
    }

    public void setClipSpan(int newSpan) {
        clip_span = newSpan;
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
        return position_index;
    }

    /**
     * Assign a new value for the starting ClipCard position. This will
     * unset any value passed to {@link #setPositionClipId(String)}
     */
    public void setPositionIndex(int newIndex) {
        position_index = newIndex;
        position_clip_id = null;
    }

    /**
     * @return The id identifying the starting ClipCard in a StoryPathLibrary
     * For a convenience method to quickly find the first ClipCard see
     * {@link scal.io.liger.model.StoryPathLibrary#getFirstClipCardForAudioClip(AudioClip, java.util.List)}
     */
    public String getPositionClipId() {
        return position_clip_id;
    }

    /**
     * Assign a new value for the starting ClipCard Id. This will
     * unset any value passed to {@link #setPositionIndex(int)}
     */
    public void setPositionClipId(String newClipId) {
        position_clip_id = newClipId;
        position_index= -1;
    }

//    public AudioClip() {}
}
