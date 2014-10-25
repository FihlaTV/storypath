package scal.io.liger.model;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scal.io.liger.Constants;
import scal.io.liger.view.DisplayableCard;

/**
 * @author Matthew Bogner
 * @author Josh Steiner
 */
public abstract class Card extends Observable implements Observer {  // REFACTOR TO AVOID CONFLICT w/ UI CARD CLASS

    protected String type;
    private String id;
    private String title;
    protected StoryPath storyPathReference; // not serialized
    protected ArrayList<String> references;
    private HashMap<String, String> values;

    public String toString() {
        return "card: " + id;
    }

    // NEW
    protected boolean stateVisiblity = false; // FIXME

    // NEW
    public boolean getStateVisiblity() {
        return stateVisiblity;
    }

    // necessary for setting loaded story paths to a clean state
    public void resetStateVisibility() {
        stateVisiblity = false;
    }

    // NEW
    // NOTE: may be sensible to revise this so that it checks references against the card received
    //       by the update method instead of fetching values from the entire story path
    @Override
    public void update(Observable observable, Object o) {
        if (!(observable instanceof Card)) {
            Log.e(this.getClass().getName(), "update notification received from non-card observable");
            return;
        }
        if (storyPathReference == null) {
            Log.e(this.getClass().getName(), "STORY PATH REFERENCE NOT FOUND, CANNOT SEND NOTIFICATION");
            return;
        }

        Card card = (Card)observable;

        //changeCardVisibilityState();
        if (checkVisibilityChanged()) {
            storyPathReference.notifyCardChanged(this);
        }
    }

    public void changeCardVisibilityState() {
        if (stateVisiblity) {
            if (checkReferencedValues() != stateVisiblity) {
                // therefore, we went from visible to invisible, remove
                //getStoryPathReference().inactivateCard(this);
            }
        } else {
            if (checkReferencedValues() != stateVisiblity) {
                // therefore, we went from invisible to visible, add it
                //getStoryPathReference().activateCard(this);
            }
        }
    }

    // NEW
    public boolean checkVisibilityChanged() {
        // TODO return visibility state
        boolean newVisibility = checkReferencedValues(); // FIXME "references" is just "visibility" right?
        if (stateVisiblity != newVisibility) {
            stateVisiblity = newVisibility;
            return true;
        } else {
            return false;
        }
    }

    // foo

    public void registerObservers() {
        if (references != null) {
            for (String reference : references) {
                Card card = storyPathReference.getCardById(reference);
                 card.addObserver(this);
            }
        }
    }

    public void removeObservers() {
        if (references != null) {
            for (String reference : references) {
                Card card = storyPathReference.getCardById(reference);
                card.deleteObserver(this);
            }
        }
    }

    public Card() {
        // required for JSON/GSON
    }

    public abstract DisplayableCard getDisplayableCard(Context context);

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() { return title;}

    public void setTitle(String title) {
        this.title = title;
    }

    public StoryPath getStoryPathReference() {
        return storyPathReference;
    }

    public void setStoryPathReference(StoryPath storyPathReference) {
        this.storyPathReference = storyPathReference;
    }

    public ArrayList<String> getReferences() {
        return references;
    }

    public void setReferences(ArrayList<String> references) {
        this.references = references;
    }

    public void addReference(String reference) {
        if (this.references == null)
            this.references = new ArrayList<String>();

        this.references.add(reference);
    }

    // FIXME we should extend this to allow 1 and 2 part ids for local checks only (key::value or key)
    public boolean checkReferencedValueMatches(String reference) {
        boolean result = true;

        String[] pathParts = reference.split("::");
        String referencedValue = storyPathReference.getReferencedValue(reference);

        if ((referencedValue != null) && (referencedValue.equals(Constants.EXTERNAL))) {
            referencedValue = storyPathReference.getExternalReferencedValue(reference);
        }

        if (pathParts.length == 3) {
            // just check that the value is not null
            if (referencedValue == null) { // FIXME this cold be simplified
                result = false;
            }
        } else { // if length == 4, check its not null and it matches
            if ((referencedValue == null) || (!referencedValue.equals(pathParts[3]))) {
                result = false;
            }
        }

        return result;
    }

    public boolean checkReferencedValues() {
        boolean result = true;

        if (references != null) {
            for (String reference : references) {
                // assumes the format story::card::field::value
                result = checkReferencedValueMatches(reference);
            }
        }

        return result;
    }

    public void clearValues() {
        values = null;
    }

    public void addValue(String key, String value) {
        addValue(key, value, true);
    }

    public void addValue(String key, String value, boolean notify) {
        if (this.values == null)
            this.values = new HashMap<String, String>();

        this.values.put(key, value);

        if (notify) {
            setChanged();
            notifyObservers();

            /*
            // send notification that a value has been saved so that cards can re-check references
            if (storyPathReference != null) {
                storyPathReference.notifyCardChanged();
            } else {
                System.err.println("STORY PATH REFERENCE NOT FOUND, CANNOT SEND NOTIFICATION");
            }
            */
        }
    }

    public boolean isKey(String key) {
        if (values != null) {
            if (values.keySet().contains(key)) {
                return true;
            }
        }

        return false;
    }

    public boolean isField(String key) {
        Class c = null;
        Field f = null;
        try {
            c = this.getClass();
            f = c.getField(key);
            return true;
        } catch (Exception e) {
            // field not found
        }

        return false;
    }

    public String getValueByKey(String key) {
        if (values != null) {
            if (values.keySet().contains(key)) {
                return values.get(key);
            }
        }

        // check class properties if no saved value was found
        Class c = null;
        Field f = null;
        try {
            c = this.getClass();
            f = c.getDeclaredField(key);
            f.setAccessible(true);
            return f.get(this).toString(); // not the best solution, but somehow int fields come back with Integer values
                                           // NEEDS REVISION TO HANDLE NON-STRING FIELDS (IE: CLIPS)
        } catch (Exception e) {
            //Log.d("TESTING", "EXCEPTION THROWN WHILE SEARCHING CLASS PROPERTIES FOR VALUE: " + e.getMessage());
        }

        //Log.e("TESTING", "VALUE " + key + " WAS NOT FOUND");
        return null;
    }

    /**
     * get's a value.
     * @param fullPath accepts is a FQID
     * @return
     */
    public String getValueById (String fullPath) {
        // assumes the format story::card::field
        String[] pathParts = fullPath.split("::");
        //FIXME this should also work for length 3!

         if (pathParts.length == 4 || pathParts.length == 3) {
            // sanity check
            if (!this.id.equals(pathParts[1])) {
                System.err.println("CARD ID " + pathParts[1] + " DOES NOT MATCH");
                return null;
            }

            return getValueByKey(pathParts[2]);
        }

        return null;
    }

    public String fillReferences(String originalString) { // <- need to integrate with observer/update process
        if (originalString == null) {
            return originalString;
        }

        String newString = originalString;

        Pattern p = Pattern.compile("\\{\\{(.+?)\\}\\}");
        Matcher m = p.matcher(newString);

        while (m.find()) {
            String referenceString = m.group(1);
            if (referenceString != null) {
                referenceString = referenceString.trim();
                String referenceValue = storyPathReference.getReferencedValue(referenceString);

                if ((referenceValue != null) && (referenceValue.equals(Constants.EXTERNAL))) {
                    referenceValue = storyPathReference.getExternalReferencedValue(referenceString);
                }

                // doing a replace with a null seems to cause issues
                if (referenceValue == null)
                    referenceValue = "";
                newString = m.replaceFirst(referenceValue);
                m = p.matcher(newString);
            }
        }

        return newString;
    }

    public void loadStoryPath(String storyPathTemplateKey) {
        if (storyPathReference instanceof StoryPathLibrary) {
            ((StoryPathLibrary)storyPathReference).loadStoryPathTemplate(storyPathTemplateKey);
        } else {
            Log.e(this.getClass().getName(), "cannot initiate a story path load from a story path card (use a link card)");
        }
    }
}
