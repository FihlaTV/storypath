package scal.io.liger.model;

import android.content.Context;
import android.util.Log;


import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.UUID;

import scal.io.liger.view.ClipCardView;
import scal.io.liger.view.DisplayableCard;


public class ClipCard extends ExampleCard {

    @Expose private String clipType;
    @Expose private ArrayList<ClipMetadata> clips;
    @Expose private ArrayList<String> goals;

    public ClipCard() {
        super();
        this.type = this.getClass().getName();
    }

    @Override
    public DisplayableCard getDisplayableCard(Context context) {
        // before displaying card, check for references and import clips if necessary
        if ((clips == null) && (references != null)) {
            for (String reference : references) {
                if (reference.endsWith("clips")) {
                    Log.d("CLIPS", "FOUND CLIP REFERENCE IN CLIP CARD " + getId() + ": " + reference);
                    ArrayList<MediaFile> mediaFiles = storyPath.getExternalMediaFile(reference);

                    if (mediaFiles == null) {
                        Log.e("CLIPS", "UNABLE TO PROCESS CLIP REFERENCE " + reference);
                    } else {
                        for (MediaFile mediaFile : mediaFiles) {
                            saveMediaFile(mediaFile);
                            Log.d("CLIPS", "SAVED MEDIA FILE " + mediaFile.getPath() + " LOCALLY");
                        }
                    }
                }
            }
        }

        return new ClipCardView(context, this);
    }

    public String getClipType() {
        return fillReferences(clipType);
    }

    public String getFirstGoal() {
        if (goals != null) {
            return goals.get(0);
        }
        return null;
    }

    public ArrayList<String> getGoals() {
        return goals;
    }

    public void setGoals(ArrayList<String> goals) {
        this.goals = goals;
    }

    public void setClipType(String clipType) {
        this.clipType = clipType;
    }

    public ArrayList<ClipMetadata> getClips() {
        return clips;
    }

    public void setClips(ArrayList<ClipMetadata> clips) {
        this.clips = clips;
    }

    /**
     * Add a new Clip to this card and make it the currently "selected" clip.
     */
    public void addClip(ClipMetadata clip) {
        addClip(clip, true);
    }

    /**
     * Add a new Clip to this card and make it the currently "selected" clip.
     */
    public void addClip(ClipMetadata clip, boolean notify) {
        if (this.clips == null) {
            this.clips = new ArrayList<>();
        }
        // by default, the first recorded clip is considered "selected"
        this.clips.add(0, clip);

        // send notification that a clip has been saved so that cards will be refreshed
        if (notify) {
            setChanged();
            notifyObservers();
        }
    }

    public void saveMediaFile(MediaFile mf) {
        ClipMetadata cmd = new ClipMetadata(clipType, UUID.randomUUID().toString());

        getStoryPath().saveMediaFile(cmd.getUuid(), mf);
        addClip(cmd);
    }

    public MediaFile loadMediaFile(ClipMetadata cmd) {
        return getStoryPath().loadMediaFile(cmd.getUuid());
    }

    public void selectMediaFile(ClipMetadata clip) {
        if ((clips == null) || (clips.size() < 1)) {
            Log.e(this.getClass().getName(), "no clip metadata was found, cannot select a file");
            return;
        }

        if (clips.indexOf(clip) != -1) {
            selectMediaFile(clips.indexOf(clip));
        } else {
            Log.e(this.getClass().getName(), "specified clip not found in clips");
        }
    }

    public void selectMediaFile(int index) {
        // unsure if selection should be based on index or object

        if ((clips == null) || (clips.size() < 1)) {
            Log.e(this.getClass().getName(), "no clip metadata was found, cannot select a file");
            return;
        }

        ClipMetadata cmd = clips.remove(index);
        clips.add(0, cmd);
    }

    public MediaFile getSelectedMediaFile() {
        if ((clips == null) || (clips.size() < 1)) {
            Log.e(this.getClass().getName(), "no clip metadata was found, cannot get a selected file");
            return null;
        }

        return loadMediaFile(clips.get(0));
    }

    public ClipMetadata getSelectedClip() {
        if ((clips == null) || (clips.size() < 1)) {
            Log.e(this.getClass().getName(), "no clip metadata was found, cannot get a selected file");
            return null;
        }

        return clips.get(0);
    }

    // the card-level delete method only deletes the local reference, not the actual media file
    public void deleteMediaFile(int index) {
        // unsure if selection should be based on index or object

        if ((clips == null) || (index >= clips.size())) {
            Log.e(this.getClass().getName(), "index out of range, cannot delete file");
            return;
        }

        clips.remove(index);
    }
}
