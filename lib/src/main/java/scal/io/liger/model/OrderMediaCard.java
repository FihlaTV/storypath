package scal.io.liger.model;

import timber.log.Timber;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Observable;

import scal.io.liger.Constants;
import scal.io.liger.ReferenceHelper;
import scal.io.liger.view.DisplayableCard;
import scal.io.liger.view.OrderMediaCardView;

public class OrderMediaCard extends Card {

    public final String TAG = this.getClass().getSimpleName();

    @Expose private String header;
    @Expose private String medium;
    @Expose private ArrayList<String> storyMedium; // why is this an array?
    @Expose private ArrayList<String> videoClipCards;
    @Expose private ArrayList<String> audioClipCards;
    @Expose private ArrayList<String> photoClipCards;

    public OrderMediaCard() {
        super();
    }

    // NEW - SHOULD THESE BE SERIALIZED?
    @Expose private String stateMedium;
    @Expose private int stateVideo;
    @Expose private int stateAudio;
    @Expose private int statePhoto;

    // NEW
    public String getStateMedium() {
        return stateMedium;
    }
    public int getStateVideo() {
        return stateVideo;
    }
    public int getStateAudio() {
        return stateAudio;
    }
    public int getStatePhoto() {
        return statePhoto;
    }

    // no setters, should not be set from outside the class

    // NEW
    @Override
    public void registerObservers() {
        super.registerObservers();

        if (storyMedium != null) {
            for (String reference : storyMedium) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.addObserver(this);
                }
            }
        }
        if (videoClipCards != null) {
            for (String reference : videoClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.addObserver(this);
                }
            }
        }
        if (audioClipCards != null) {
            for (String reference : audioClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.addObserver(this);
                }
            }
        }
        if (photoClipCards != null) {
            for (String reference : photoClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.addObserver(this);
                }
            }
        }
    }

    // NEW
    @Override
    public void removeObservers() {
        super.removeObservers();

        if (storyMedium != null) {
            for (String reference : storyMedium) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.deleteObserver(this);
                }
            }
        }
        if (videoClipCards != null) {
            for (String reference : videoClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.deleteObserver(this);
                }
            }
        }
        if (audioClipCards != null) {
            for (String reference : audioClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.deleteObserver(this);
                }
            }
        }
        if (photoClipCards != null) {
            for (String reference : photoClipCards) {
                Card card = storyPath.getCardById(reference);
                // need to filter out cards that are not found (ie: references to external files)
                if (card != null) {
                    card.deleteObserver(this);
                }
            }
        }
    }

    // NEW
    @Override
    public void update(Observable observable, Object o) {
        if (!(observable instanceof Card)) {
            Timber.e("update notification received from non-card observable");
            return;
        }
        if (storyPath == null) {
            Timber.e("STORY PATH REFERENCE NOT FOUND, CANNOT SEND NOTIFICATION");
            return;
        }

        Card card = (Card)observable;

        if (checkVisibilityChanged() ||
            checkStateMedium() ||
            checkStateVideo() ||
            checkStateAudio() ||
            checkStatePhoto()) {
            storyPath.notifyCardChanged(this);
        }
    }

    // NEW
    public boolean checkStateMedium() {
        String mediumReference = "";

        if (storyMedium.size() == 1) {
            mediumReference = storyMedium.get(0);
        } else {
            Timber.e("unexpected number of story medium references: " + storyMedium.size());
            return false;
        }

        String newState = storyPath.getReferencedValue(mediumReference);

        if ((newState != null) && (newState.equals(Constants.EXTERNAL))) {
            newState = storyPath.getExternalReferencedValue(mediumReference);
        }

        if (stateMedium != newState) {
            stateMedium = newState;
            return true;
        } else {
            return false;
        }
    }

    // NEW
    public boolean checkStateVideo() {
        int newState = 0;

        ArrayList<String> values = storyPath.getValues(videoClipCards);

        if (values == null) {
            newState = 0;
        } else {
            for (String s : values) {
                if ((s != null) && (s.length() > 0)) {
                    newState++;
                }
            }
        }

        if (stateVideo != newState) {
            stateVideo = newState;
            return true;
        } else {
            return false;
        }
    }

    // NEW
    public boolean checkStateAudio() {
        int newState = 0;

        ArrayList<String> values = storyPath.getValues(audioClipCards);

        if (values == null) {
            newState = 0;
        } else {
            for (String s : values) {
                if ((s != null) && (s.length() > 0)) {
                    newState++;
                }
            }
        }

        if (stateAudio != newState) {
            stateAudio = newState;
            return true;
        } else {
            return false;
        }
    }

    // NEW
    public boolean checkStatePhoto() {
        int newState = 0;

        ArrayList<String> values = storyPath.getValues(photoClipCards);

        if (values == null) {
            newState = 0;
        } else {
            for (String s : values) {
                if ((s != null) && (s.length() > 0)) {
                    newState++;
                }
            }
        }

        if (statePhoto != newState) {
            statePhoto = newState;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public DisplayableCard getDisplayableCard(Context context) {
        return new OrderMediaCardView(context, this);
    }

    public String getHeader() {
        return fillReferences(header);
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public ArrayList<String> getStoryMedium() {
        return storyMedium;
    }

    public void setStoryMedium(ArrayList<String> storyMedium) {
        this.storyMedium = storyMedium;
    }

    public ArrayList<String> getVideoClipCards() {
        return videoClipCards;
    }

    public void setVideoClipCards(ArrayList<String> videoClipCards) {
        this.videoClipCards = videoClipCards;
    }

    public ArrayList<String> getAudioClipCards() {
        return audioClipCards;
    }

    public void setAudioClipCards(ArrayList<String> audioClipCards) {
        this.audioClipCards = audioClipCards;
    }

    public ArrayList<String> getPhotoClipCards() {
        return photoClipCards;
    }

    public void setPhotoClipCards(ArrayList<String> photoClipCards) {
        this.photoClipCards = photoClipCards;
    }

    public ArrayList<String> getClipPaths() {
        ArrayList<String> clipPaths = new ArrayList<String>();

        String mediumReference = "";

        if (storyMedium.size() == 1) {
            mediumReference = storyMedium.get(0);
        }
        else {
            Timber.e("unexpected number of story medium references: " + storyMedium.size());
            return clipPaths;
        }

        medium = storyPath.getReferencedValue(mediumReference);

        if ((medium != null) && (medium.equals(Constants.EXTERNAL))) {
            medium = storyPath.getExternalReferencedValue(mediumReference);
        }

        if ((medium == null) || (medium.length() == 0 )) {
            Timber.e("no value found for story medium referenced by " + mediumReference);
            return clipPaths;
        }
        else if (medium.equals(Constants.VIDEO)) {
            clipPaths.addAll(videoClipCards);
        }
        else if (medium.equals(Constants.AUDIO)) {
            clipPaths.addAll(audioClipCards);
        }
        else if (medium.equals(Constants.PHOTO)) {
            clipPaths.addAll(photoClipCards);
        }

        return  clipPaths;
    }

    @Override
    public void copyText(Card card) {
        if (!(card instanceof OrderMediaCard)) {
            Timber.e("CARD " + card.getId() + " IS NOT AN INSTANCE OF OrderMediaCard");
        }
        if (!(this.getId().equals(card.getId()))) {
            Timber.e("CAN'T COPY STRINGS FROM " + card.getId() + " TO " + this.getId() + " (CARD ID'S MUST MATCH)");
            return;
        }

        OrderMediaCard castCard = (OrderMediaCard)card;

        this.title = castCard.getTitle();
        this.header = castCard.getHeader();
    }
}