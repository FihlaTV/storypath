package scal.io.liger.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.VideoView;

import com.fima.cardsui.objects.Card;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import scal.io.liger.model.CardModel;
import scal.io.liger.model.OrderMediaCardModel;
import scal.io.liger.R;
import scal.io.liger.touch.DraggableGridView;
import scal.io.liger.touch.OnRearrangeListener;

public class OrderMediaCardView extends Card {
    private OrderMediaCardModel mCardModel;
    private Context mContext;

    ImageView ivCardImage;
    List<Integer> listDrawables = new ArrayList<Integer>();
    List<CardModel> listCards = new ArrayList<CardModel>();

    private static boolean firstTime = true;

    public OrderMediaCardView(Context context, CardModel cardModel) {
        mContext = context;
        mCardModel = (OrderMediaCardModel) cardModel;
    }

    @Override
    public View getCardContent(Context context) {
        if (mCardModel == null) {
            return null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.card_order_media, null);
        ivCardImage = ((ImageView) view.findViewById(R.id.iv_card_image));
        DraggableGridView dgvOrderClips = ((DraggableGridView) view.findViewById(R.id.dgv_media_clips));

        if(firstTime) {
            listDrawables.add(0, R.drawable.sample0);
            listDrawables.add(1, R.drawable.sample1);
            listDrawables.add(2, R.drawable.sample2);
        }

        ivCardImage.setImageDrawable(mContext.getResources().getDrawable(listDrawables.get(0)));
        loadClips(mCardModel.getClipPaths(), dgvOrderClips);

        return view;
    }

    public void loadClips(ArrayList<String> clipPaths, DraggableGridView dgvOrderClips) {
        dgvOrderClips.removeAllViews();

        ImageView ivTemp;
        File fileTemp;
        Bitmap bmTemp;

        for (int i=0; i<3; i++) {
            ivTemp = new ImageView(mContext);

            ivTemp.setImageDrawable(mContext.getResources().getDrawable(listDrawables.get(i)));
            dgvOrderClips.addView(ivTemp);

            //TODO TERRIBLE
            if(firstTime) {
                CardModel cm = mCardModel.storyPathReference.getCardById(clipPaths.get(i));
                listCards.add(i, cm);
            }

            //TODO have the clips dynamically pulled from the cards
            /*
            fileTemp =  new File(clipPaths.get(i));

            if(fileTemp.exists() && !fileTemp.isDirectory()) {
                bmTemp = BitmapFactory.decodeFile(fileTemp.getPath());
                ivTemp.setImageBitmap(bmTemp);
                dgvOrderClips.addView(ivTemp);
            }*/
        }

        firstTime = false;

        dgvOrderClips.setOnRearrangeListener(new OnRearrangeListener() {
            @Override
            public void onRearrange(int currentIndex, int newIndex) {
                mCardModel.getStoryPathReference().rearrangeCards(currentIndex, newIndex);

                //TODO: REMOVE - just for visualization
                int currentValue = listDrawables.remove(currentIndex);
                listDrawables.add(newIndex, currentValue);
                if(newIndex == 0) {
                    ivCardImage.setImageDrawable(mContext.getResources().getDrawable(listDrawables.get(0)));
                }
            }
        });

        dgvOrderClips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int i = 1;
            }
        });
    }
}