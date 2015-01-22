package scal.io.liger.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import scal.io.liger.Constants;
import scal.io.liger.R;
import scal.io.liger.model.Card;
import scal.io.liger.model.ClipCard;
import scal.io.liger.model.MediaFile;
import scal.io.liger.model.OrderMediaCard;
import scal.io.liger.popup.OrderMediaPopup;
import scal.io.liger.touch.DraggableGridView;
import scal.io.liger.touch.OnRearrangeListener;

public class OrderMediaCardView implements DisplayableCard {
    public static final String TAG = "OrderMediaCardView";

    private OrderMediaCard mCardModel;
    private Context mContext;
    private List<ClipCard> mListCards = new ArrayList<>();
    private PopupWindow mPopup;

    public OrderMediaCardView(Context context, Card cardModel) {
        mContext = context;
        mCardModel = (OrderMediaCard) cardModel;
    }

    @Override
    public View getCardView(Context context) {
        if (mCardModel == null) {
            return null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.card_order_media, null);
        DraggableGridView dgvOrderClips = ((DraggableGridView) view.findViewById(R.id.dgv_media_clips));

        loadClips(mCardModel.getClipPaths(), dgvOrderClips);

        // supports automated testing
        view.setTag(mCardModel.getId());

        return view;
    }

    public void fillList(ArrayList<String> clipPaths) {

        // TODO : Modify getCardsById to allow passing type
        List<Card> cards =  mCardModel.getStoryPath().getCardsByIds(clipPaths);
        List<ClipCard> clipCards = new ArrayList<>();
        for (Card card : cards) {
            if (card instanceof ClipCard)
                clipCards.add((ClipCard) card);
        }
        mListCards = clipCards;
    }

    public void loadClips(ArrayList<String> clipPaths, DraggableGridView dgvOrderClips) {
        dgvOrderClips.removeAllViews();

        String medium = mCardModel.getMedium();

        ImageView ivTemp;
        File fileTemp;
        Bitmap bmTemp;

        fillList(clipPaths);

        // removing size check and 1->3 loop, should be covered by fillList + for loop
        for (Card cm : mListCards) {

            ClipCard ccm = null;

            if (cm instanceof ClipCard) {
                ccm = (ClipCard) cm;
            } else {
                continue;
            }

            String mediaPath = null;
            MediaFile mf = ccm.getSelectedMediaFile();

            if (mf == null) {
                Log.e(TAG, "no media file was found");
            } else {
                mediaPath = mf.getPath();
            }

            //File mediaFile = null;
            Uri mediaURI = null;

            if(mediaPath != null) {
                /*
                mediaFile = MediaHelper.loadFileFromPath(ccm.getStoryPath().buildZipPath(mediaPath));
                if(mediaFile.exists() && !mediaFile.isDirectory()) {
                    mediaURI = Uri.parse(mediaFile.getPath());
                }
                */
                mediaURI = Uri.parse(mediaPath);
            }

            if (medium != null && mediaURI != null) {
                if (medium.equals(Constants.VIDEO)) {
                    ivTemp = new ImageView(mContext);
                    //Bitmap videoFrame = Utility.getFrameFromVideo(mediaURI.getPath());
                    Bitmap videoFrame = mf.getThumbnail(mContext);
                    if(null != videoFrame) {
                        ivTemp.setImageBitmap(videoFrame);
                    }
                    dgvOrderClips.addView(ivTemp);
                    continue;
                }else if (medium.equals(Constants.PHOTO)) {
                    ivTemp = new ImageView(mContext);
                    ivTemp.setImageURI(mediaURI);
                    dgvOrderClips.addView(ivTemp);
                    continue;
                }
            }

            //handle fall-through cases: (media==null || medium==AUDIO)
            ivTemp = new ImageView(mContext);

            String clipType = ccm.getClipType();
            int drawable = R.drawable.ic_launcher;

            // FIXME these need to be replaced with the real overlays
            if (clipType.equals(Constants.CHARACTER)) {
                drawable = R.drawable.cliptype_close;
            } else if (clipType.equals(Constants.ACTION)) {
                drawable = R.drawable.cliptype_medium;
            } else if (clipType.equals(Constants.RESULT)){
                drawable = R.drawable.cliptype_long;
            } else if (clipType.equals(Constants.PLACE)){
                drawable = R.drawable.cliptype_wide;
            } else if (clipType.equals(Constants.SIGNATURE)){
                drawable = R.drawable.cliptype_detail;
            }

            ivTemp.setImageDrawable(mContext.getResources().getDrawable(drawable));
            dgvOrderClips.addView(ivTemp);
        }

        dgvOrderClips.setOnRearrangeListener(new OnRearrangeListener() {
            @Override
            public void onRearrange(int currentIndex, int newIndex) {
                //update actual card list
                Card currentCard = mListCards.get(currentIndex);
                int currentCardIndex = mCardModel.getStoryPath().getCardIndex(currentCard);
                int newCardIndex = currentCardIndex - (currentIndex - newIndex);

                mCardModel.getStoryPath().rearrangeCards(currentCardIndex, newCardIndex);

            }
        });

        dgvOrderClips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO Unsafe Cast
                OrderMediaPopup.show(((Activity) view.getContext()), mCardModel.getMedium(), mListCards, null);
            }
        });
    }

}