package scal.io.liger.model;

import timber.log.Timber;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.UUID;

import scal.io.liger.Constants;
import scal.io.liger.R;
import scal.io.liger.view.ClipCardView;
import scal.io.liger.view.DisplayableCard;


public class ClipCard extends ExampleCard implements Cloneable {

    public final String TAG = this.getClass().getSimpleName();

    @Expose private String clipType;
    @Expose private ArrayList<ClipMetadata> clips;
    @Expose private ArrayList<String> goals;

    public ClipCard() {
        super();
    }

    @Override
    @NonNull
    public DisplayableCard getDisplayableCard(Context context) {
        // before displaying card, check for references and import clips if necessary
        if ((clips == null) && (references != null)) {
            for (String reference : references) {
                if (reference.endsWith("clips")) {
                    Timber.d("FOUND CLIP REFERENCE IN CLIP CARD " + getId() + ": " + reference);
                    ArrayList<MediaFile> mediaFiles = storyPath.getExternalMediaFile(reference);

                    if (mediaFiles == null) {
                        Timber.e("UNABLE TO PROCESS CLIP REFERENCE " + reference);
                    } else {
                        for (MediaFile mediaFile : mediaFiles) {
                            saveMediaFile(mediaFile);
                            Timber.d("SAVED MEDIA FILE " + mediaFile.getPath() + " LOCALLY");
                        }
                    }
                }
            }
        }

        return new ClipCardView(context, this);
    }

    @Nullable
    public String getClipType() {
        return fillReferences(clipType);
    }

    @NonNull
    public String getClipTypeLocalized() {
        return Constants.getClipTypeLocalized(getStoryPath().getContext(), getClipType());
    }

    @Nullable
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
     *
     * @param notify whether to notify observers of this card. This should generally be true
     *               unless called during deserialization of this object or any state
     *               where the containing StoryPath is not fully initialized.
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

    public void removeClip(@NonNull ClipMetadata clip) {
        if (this.clips == null || this.clips.size() == 0) {
            Timber.w("removeClip called, but no clips exist");
            return;
        }

        boolean didRemove = this.clips.remove(clip);

        if (didRemove) {
            setChanged();
            notifyObservers();
        } else {
            Timber.w("Requested clip could not be removed because it was not found");
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

    public void selectMediaFile(@Nullable ClipMetadata clip) {
        if ((clips == null) || (clips.size() < 1)) {
            Timber.e("no clip metadata was found, cannot select a file");
            return;
        }

        if (clips.indexOf(clip) != -1) {
            selectMediaFile(clips.indexOf(clip));
        } else {
            Timber.e("specified clip not found in clips");
        }
    }

    public void selectMediaFile(int index) {
        // unsure if selection should be based on index or object

        if ((clips == null) || (clips.size() < 1)) {
            Timber.e("no clip metadata was found, cannot select a file");
            return;
        }

        ClipMetadata cmd = clips.remove(index);
        clips.add(0, cmd); // FIXME "selected" is hardcoded as 0th item which will fall apart if we move to selection not rearranging list
    }

    @Nullable
    public MediaFile getSelectedMediaFile() {
        if ((clips == null) || (clips.size() < 1)) {
            Timber.e("no clip metadata was found, cannot get a selected file");
            return null;
        }

        return loadMediaFile(clips.get(0)); // FIXME "selected" is hardcoded as 0th item which will fall apart if we move to selection not rearranging list
    }

    @Nullable
    public ClipMetadata getSelectedClip() {
        if ((clips == null) || (clips.size() < 1)) {
            Timber.e("no clip metadata was found, cannot get a selected file");
            return null;
        }

        return clips.get(0); // FIXME "selected" is hardcoded as 0th item which will fall apart if we move to selection not rearranging list
    }

    // the card-level delete method only deletes the local reference, not the actual media file
    public void deleteMediaFile(int index) {
        // unsure if selection should be based on index or object

        if ((clips == null) || (index >= clips.size())) {
            Timber.e("index out of range, cannot delete file");
            return;
        }

        clips.remove(index);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ClipCard clone = (ClipCard) super.clone();
        clone.clipType = this.clipType; // Strings are immutable
        if (this.goals != null) clone.goals = (ArrayList<String>) this.goals.clone();
        if (this.clips != null) {
            // we need to clone all the clips in the arraylist so when you trim on in teh new card it doesn't effect them in the old card
            ArrayList<ClipMetadata> newClips = new ArrayList<ClipMetadata>();
            for (ClipMetadata clip : this.clips) {
                ClipMetadata newClip = (ClipMetadata) clip.clone();
                newClips.add(newClip);
            }
            clone.clips = newClips;
        }

        String uuid = UUID.randomUUID().toString(); // FIXME move this logic up to the Card base class
        String rand = uuid.substring(uuid.length() - 4, uuid.length());
        String newId = getId() + "." + rand;
        while (storyPath.getCardByIdOnly(newId) != null) {
            uuid = UUID.randomUUID().toString();
            rand = uuid.substring(uuid.length() - 4, uuid.length());
            newId = getId() + "." + rand;
        }
        clone.setId(newId);



        return clone;
    }


    @Override
    public void copyText(@NonNull Card card) {
        if (!(card instanceof ClipCard)) {
            Timber.e("CARD " + card.getId() + " IS NOT AN INSTANCE OF ClipCard");
            return;
        }
        if (!(this.getId().equals(card.getId()))) {
            Timber.e("CAN'T COPY STRINGS FROM " + card.getId() + " TO " + this.getId() + " (CARD ID'S MUST MATCH)");
            return;
        }

        ClipCard castCard = (ClipCard)card;

        this.title = castCard.getTitle();
        this.header = castCard.getHeader();
        this.clipType = castCard.getClipType();
        this.goals = castCard.getGoals();
    }
}
