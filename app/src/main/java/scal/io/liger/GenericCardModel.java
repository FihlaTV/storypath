package scal.io.liger;

import android.content.Context;

import com.fima.cardsui.objects.Card;

import java.util.ArrayList;

import scal.io.liger.view.GenericCardView;

/**
 * Created by mnbogner on 7/17/14.
 */
public class GenericCardModel extends CardModel {
    private String mediaPath;
    private String header;
    private String text;
    public ArrayList<String> storyPaths;

    public GenericCardModel() {
        this.type = this.getClass().getName();
    }

    @Override
    public Card getCardView(Context context) {
        return new GenericCardView(context, this);
    }

    public String getMediaPath() { return mediaPath; }

    public void setMediaPath(String mediaPath) { this.mediaPath = mediaPath; }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<String> getStoryPaths() {
        return storyPaths;
    }

    public void setStoryPaths(ArrayList<String> storyPaths) {
        this.storyPaths = storyPaths;
    }

    public void addStoryPath(String storyPath) {
        if (this.storyPaths == null)
            this.storyPaths = new ArrayList<String>();

        this.storyPaths.add(storyPath);
    }
}
