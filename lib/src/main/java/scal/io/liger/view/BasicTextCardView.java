package scal.io.liger.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import scal.io.liger.R;
import scal.io.liger.model.Card;
import scal.io.liger.model.HowToCard;

public class BasicTextCardView implements DisplayableCard {

    private HowToCard mCardModel;
    private Context mContext;

    public BasicTextCardView(Context context, Card cardModel) {
        mContext = context;
        mCardModel = (HowToCard) cardModel;
    }

    @Override
    public View getCardView(Context context) {
        if (mCardModel == null) {
            return null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.card_basic_text, null);
        TextView tvText = ((TextView) view.findViewById(R.id.tv_text));

        tvText.setText(mCardModel.getText());

        // supports automated testing
        view.setTag(mCardModel.getId());

        return view;
    }
}
