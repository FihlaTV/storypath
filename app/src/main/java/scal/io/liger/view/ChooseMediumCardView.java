package scal.io.liger.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fima.cardsui.objects.Card;

import scal.io.liger.Constants;
import scal.io.liger.model.CardModel;
import scal.io.liger.model.ChooseMediumCardModel;
import scal.io.liger.R;


public class ChooseMediumCardView extends Card {
    private ChooseMediumCardModel mCardModel;
    private Context mContext;

    private Button mBtnMediumVideo;
    private Button mBtnMediumAudio;
    private Button mBtnMediumPhoto;

    public ChooseMediumCardView(Context context, CardModel cardModel) {
        mContext = context;
        mCardModel = (ChooseMediumCardModel) cardModel;
    }

    @Override
    public View getCardContent(Context context) {
        if (mCardModel == null) {
            return null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.card_choose_medium, null);
        TextView tvHeader = ((TextView) view.findViewById(R.id.tv_header));
        mBtnMediumVideo = ((Button) view.findViewById(R.id.btn_medium_video));
        mBtnMediumAudio = ((Button) view.findViewById(R.id.btn_medium_audio));
        mBtnMediumPhoto = ((Button) view.findViewById(R.id.btn_medium_photo));

        tvHeader.setText(mCardModel.getHeader());

        mBtnMediumVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext, "Video click", Toast.LENGTH_SHORT).show();
                mCardModel.clearValues();
                mCardModel.addValue("value::" + Constants.VIDEO);
                highlightButton(v);
            }
        });

        mBtnMediumAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext, "Audio click", Toast.LENGTH_SHORT).show();
                mCardModel.clearValues();
                mCardModel.addValue("value::" + Constants.AUDIO);
                highlightButton(v);
            }
        });

        mBtnMediumPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext, "Photo click", Toast.LENGTH_SHORT).show();
                mCardModel.clearValues();
                mCardModel.addValue("value::" + Constants.PHOTO);
                highlightButton(v);
            }
        });

        String value = mCardModel.getValueByKey("value");
        if (value != null) {
            if (value.equals(Constants.VIDEO)) {
                mBtnMediumVideo.setBackgroundColor(mContext.getResources().getColor(R.color.holo_blue_light));
                mBtnMediumVideo.setTextColor(mContext.getResources().getColor(R.color.white));
            } else if (value.equals(Constants.AUDIO)) {
                mBtnMediumAudio.setBackgroundColor(mContext.getResources().getColor(R.color.holo_blue_light));
                mBtnMediumAudio.setTextColor(mContext.getResources().getColor(R.color.white));
            } else if (value.equals(Constants.PHOTO)) {
                mBtnMediumPhoto.setBackgroundColor(mContext.getResources().getColor(R.color.holo_blue_light));
                mBtnMediumPhoto.setTextColor(mContext.getResources().getColor(R.color.white));
            }
        }

        return view;
    }

    private void highlightButton(View button) {
        mBtnMediumVideo.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        mBtnMediumAudio.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        mBtnMediumPhoto.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        button.setBackgroundColor(mContext.getResources().getColor(R.color.holo_blue_light));
        ((Button) button).setTextColor(mContext.getResources().getColor(R.color.white));
    }
}
