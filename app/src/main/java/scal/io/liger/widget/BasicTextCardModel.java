package scal.io.liger.widget;

import android.content.Context;

import com.fima.cardsui.objects.Card;

import scal.io.liger.CardModel;
import scal.io.liger.view.BasicTextCardView;
import scal.io.liger.view.ChooseMediumCardView;

public class BasicTextCardModel extends CardModel {
    private String text;

    public BasicTextCardModel() {
        this.type = this.getClass().getName();
    }

    @Override
    public Card getCardView(Context context) {
        return new BasicTextCardView(context, this);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
