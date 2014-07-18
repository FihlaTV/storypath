package scal.io.liger.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.fima.cardsui.objects.Card;

import scal.io.liger.R;

public class CardView extends Card {

    private String mDesc = "";
    private View mView;
    private TextView mTextViewTitle;
    private TextView mTextViewDesc;
    private int mId = -1;
    private int mIcon = -1;
    private Drawable mImage = null;

    public CardView() {
        super("");
    }

    public CardView(String title, String desc) {
        super(title);
        mDesc = desc;
    }

    public void setIcon(int icon) {
        mIcon = icon;
    }

    public void setImage(Drawable image) {
        mImage = image;
    }

    public void setId(int id) {
        mId = id;
    }

    private OnClickListener mListener;

    @Override
    public void setOnClickListener(OnClickListener listener) {

        mListener = listener;

        super.setOnClickListener(mListener);
    }

    @Override
    public View getCardContent(Context context) {

        mView = LayoutInflater.from(context).inflate(R.layout.card_picture, null);

        mTextViewTitle = ((TextView) mView.findViewById(R.id.title));
        mTextViewTitle.setText(title);

        mTextViewDesc = ((TextView) mView.findViewById(R.id.description));
        mTextViewDesc.setText(mDesc);

        if (mId != -1) {
            mView.setId(mId);
            mView.setOnClickListener(mListener);
            mTextViewDesc.setId(mId);
            mTextViewTitle.setId(mId);
            mView.setOnClickListener(mListener);
        }

        if (mImage != null) {
            ImageView iv = ((ImageView) mView.findViewById(R.id.imageView1));
            iv.setImageDrawable(mImage);

            if (mId != -1) {
                iv.setId(mId);
                iv.setOnClickListener(mListener);
            }
        }

        if (mIcon != -1) {
            ImageView iv = ((ImageView) mView.findViewById(R.id.cardIcon));
            iv.setImageResource(mIcon);

            if (mId != -1) {
                iv.setId(mId);
                iv.setOnClickListener(mListener);
            }
        }

        return mView;
    }
}
