package scal.io.liger.model;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by mnbogner on 8/13/14.
 */
public class TipCardModel extends MarkdownCardModel {

    private Random random;
    public ArrayList<String> tips;

    public TipCardModel() {
        this.type = this.getClass().getName();
        random = new Random();
    }

    @Override
    public String getText() {
        if (text == null)
            text = randomTip();
        return super.getText();
    }

    public ArrayList<String> getTips() {
        ArrayList<String> a = new ArrayList<String>();
        for (String s : tips) {
            a.add(fillReferences(s));
        }

        return a;
    }

    public void setTips(ArrayList<String> tips) {
        this.tips = tips;
    }

    public void addTip(String tip) {
        if (this.tips == null)
            this.tips = new ArrayList<String>();

        this.tips.add(tip);
    }

    public String randomTip() {
        return tips.get(random.nextInt(tips.size()));
    }
}
