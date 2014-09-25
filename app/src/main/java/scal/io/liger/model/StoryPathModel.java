package scal.io.liger.model;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.fima.cardsui.objects.Card;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.MalformedJsonException;

import java.io.File;
import java.util.ArrayList;

import scal.io.liger.Constants;
import scal.io.liger.JsonHelper;
import scal.io.liger.MainActivity;
import scal.io.liger.StoryPathDeserializer;

/**
 * Created by mnbogner on 7/10/14.
 */
public class StoryPathModel {
    public String id;
    public String title;
    public ArrayList<CardModel> cards;
    public ArrayList<DependencyModel> dependencies;
    public String fileLocation;

    // this is used by the JsonHelper class to load json assets
    // if there is an alternate way to load them, this should be removed
    // also must be cleared before serializing story path
    public Context context;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<CardModel> getCards() {
        return cards;
    }

    public void setCards(ArrayList<CardModel> cards) {
        this.cards = cards;
    }

    public void addCard(CardModel card) {
        if (this.cards == null)
            this.cards = new ArrayList<CardModel>();

        this.cards.add(card);
    }

    public CardModel getCardById(String fullPath) {
        // assumes the format story::card::field::value
        String[] pathParts = fullPath.split("::");

        // sanity check
        if (!this.id.equals(pathParts[0])) {
            System.err.println("STORY PATH ID " + pathParts[0] + " DOES NOT MATCH");
            return null;
        }

        for (CardModel card : cards) {
            if (card.getId().equals(pathParts[1])) {
                return card;
            }
        }

        System.err.println("CARD ID " + pathParts[1] + " WAS NOT FOUND");
        return null;
    }

    // new method to get batches of cards while preserving card order
    public ArrayList<CardModel> getCardsByIds(ArrayList<String> fullPaths) {
        ArrayList<String> cardIds = new ArrayList<String>();
        for (String fullPath : fullPaths) {
            // assumes the format story::card::field::value
            String[] pathParts = fullPath.split("::");
            cardIds.add(pathParts[1]);
        }

        ArrayList<CardModel> foundCards = new ArrayList<CardModel>();
        for (CardModel card : cards) {
            if (cardIds.contains(card.getId())) {
                foundCards.add(card);
            }
        }

        return foundCards;
    }

    public ArrayList<CardModel> getValidCards() {
        ArrayList<CardModel> validCards = new ArrayList<CardModel>();

        for (CardModel card : cards) {
            if (card.checkReferencedValues()) {
                validCards.add(card);
            }
        }

        return validCards;
    }

    public ArrayList<DependencyModel> getDependencies() {
        return dependencies;
    }

    public void setDependencies(ArrayList<DependencyModel> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(DependencyModel dependency) {
        if (this.dependencies == null)
            this.dependencies = new ArrayList<DependencyModel>();

        this.dependencies.add(dependency);
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    // set a reference to this story path in each card
    // must be done before cards attempt to reference
    // values from previous story paths or cards
    public void setCardReferences() {
        for (CardModel card : cards) {
            card.setStoryPathReference(this);
        }
    }

    // clear references to this story path from each card
    // must be done before serializing this story path to
    // prevent duplication or circular references
    public void clearCardReferences() {
        for (CardModel card : cards) {
            card.setStoryPathReference(null);
        }
    }

    public String getReferencedValue(String fullPath) {
        // assumes the format story::card::field::value
        String[] pathParts = fullPath.split("::");

        if (!this.getId().equals(pathParts[0])) {
            return Constants.EXTERNAL;
        }

        CardModel card = this.getCardById(fullPath);

        if (card == null) {
            return null;
        } else {
            String value = card.getValueById(fullPath);

            if (value == null) {
                return null;
            } else {
                return value;
            }
        }
    }

    public String getExternalReferencedValue(String fullPath) {
        // assumes the format story::card::field::value
        String[] pathParts = fullPath.split("::");

        StoryPathModel story = null;

        // reference targets a serialized story path
        for (DependencyModel dependency : dependencies) {
            if (dependency.getDependencyId().equals(pathParts[0])) {
                GsonBuilder gBuild = new GsonBuilder();
                gBuild.registerTypeAdapter(StoryPathModel.class, new StoryPathDeserializer());
                Gson gson = gBuild.create();

                String json = JsonHelper.loadJSONFromPath(buildPath(dependency.getDependencyFile()));
                story = gson.fromJson(json, StoryPathModel.class);

                story.context = this.context;
                story.setCardReferences();
                story.setFileLocation(buildPath(dependency.getDependencyFile()));
            }
        }

        if (story == null) {
            Log.e(this.getClass().getName(), "STORY PATH ID " + pathParts[0] + " WAS NOT FOUND");
            return null;
        }

        CardModel card = story.getCardById(fullPath);

        if (card == null) {
            return null;
        } else {
            String value = card.getValueById(fullPath);

            if (value == null) {
                return null;
            } else {
                return value;
            }
        }
    }

    public String buildPath(String originalPath) {
        if (originalPath.startsWith(File.separator)) {
            return originalPath;
        }

        // construct path relative to location of story path
        String relativePath = getFileLocation();

        if ((relativePath != null) && (relativePath.length() != 0)) {
            relativePath = relativePath.substring(0, relativePath.lastIndexOf(File.separator));
            relativePath = relativePath + File.separator + originalPath;
            return relativePath;
        } else {
            Log.e(this.getClass().getName(), "NO ROOT TO CONSTRUCT RELATIVE PATH FOR " + originalPath);
            return originalPath;
        }
    }

    public void notifyActivity() {
        Log.d("StoryPathModel", "notifyActivity");
        if (context != null) {
            MainActivity mainActivity = (MainActivity) context; // FIXME this isn't a save cast as context can sometimes not be an activity (getApplicationContext())
            mainActivity.refreshCardView();
        } else {
            System.err.println("APP CONTEXT REFERENCE NOT FOUND, CANNOT SEND NOTIFICATION");
        }
    }

    public void linkNotification(String linkPath) {
        if (context != null) {
            try {
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.goToCard(linkPath);
            } catch (Exception e) {
                Toast.makeText(context, "JSON parsing error: " + e.getMessage().substring(e.getMessage().indexOf(":") + 2), Toast.LENGTH_LONG).show();
            }
        } else {
            System.err.println("APP CONTEXT REFERENCE NOT FOUND, CANNOT SEND LINK NOTIFICATION");
        }
    }

    public int getCardIndex(CardModel cardModel) {
        return cards.indexOf(cardModel);
    }

    public int getValidCardIndex(CardModel cardModel) {
        return getValidCards().indexOf(cardModel);
    }

    public CardModel getCardFromIndex(int index) {
        if(index >= cards.size()) {
            return null;
        }

        return cards.get(index);
    }

    public CardModel getValidCardFromIndex(int index) {
        ArrayList<CardModel> validCards = getValidCards();

        if(index >= validCards.size()) {
            return null;
        }

        return validCards.get(index);
    }

    public void rearrangeCards(int currentIndex, int newIndex) {
        CardModel card = cards.remove(currentIndex);
        cards.add(newIndex, card);
        notifyActivity();
    }
}
