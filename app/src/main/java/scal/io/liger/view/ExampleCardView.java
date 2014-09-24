package scal.io.liger.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;

import com.fima.cardsui.objects.Card;

import java.io.File;
import java.io.IOException;

import scal.io.liger.Constants;
import scal.io.liger.MediaHelper;
import scal.io.liger.R;
import scal.io.liger.Utility;
import scal.io.liger.model.CardModel;
import scal.io.liger.model.ExampleCardModel;

public class ExampleCardView extends Card {

    public ExampleCardModel mCardModel;
    public Context mContext;
    public static final String MEDIA_PATH_KEY = "value";

    public MediaController mMediaController;

    public ExampleCardView () {
        // empty, required for ClipCardView
    }

    public ExampleCardView(Context context, CardModel cardModel) {
        mContext = context;
        mCardModel = (ExampleCardModel) cardModel;
    }

    @Override
    public View getCardContent(Context context) {
        if (mCardModel == null) {
            return null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.card_example, null);
        final VideoView vvCardVideo = ((VideoView) view.findViewById(R.id.vv_card_video));
        final ImageView ivCardPhoto = ((ImageView) view.findViewById(R.id.iv_card_photo));
        TextView tvHeader = ((TextView) view.findViewById(R.id.tv_header));
        final ToggleButton btnMediaPlay = ((ToggleButton) view.findViewById(R.id.tb_card_audio));

        tvHeader.setText(mCardModel.getHeader());

        final String clipMedium = mCardModel.getClipMedium();
        final String cardMediaId = mCardModel.getStoryPathReference().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;

        //set up media display
        final File mediaFile = getValidFile(null, mCardModel.getExampleMediaPath());

        if (mediaFile == null) {
            // using medium cliptype image as default in case media file is missing
            ivCardPhoto.setImageDrawable(mContext.getResources().getDrawable(R.drawable.cliptype_medium));
            ivCardPhoto.setVisibility(View.VISIBLE);
        } else if (mediaFile.exists() && !mediaFile.isDirectory()) {
            if (clipMedium.equals(Constants.VIDEO)) {

                //set up image as preview
                Bitmap videoFrame = Utility.getFrameFromVideo(mediaFile.getPath());
                if(null != videoFrame) {
                    ivCardPhoto.setImageBitmap(videoFrame);
                }

                ivCardPhoto.setVisibility(View.VISIBLE);
                btnMediaPlay.setVisibility(View.VISIBLE);
                btnMediaPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri video = Uri.parse(mediaFile.getPath());
                        vvCardVideo.setVideoURI(video);
                        vvCardVideo.seekTo(5);
                        vvCardVideo.setMediaController(null);
                        vvCardVideo.setVisibility(View.VISIBLE);
                        ivCardPhoto.setVisibility(View.GONE);
                        btnMediaPlay.setVisibility(View.GONE);
                        vvCardVideo.start();
                    }
                });

                //revert back to image on video completion
                vvCardVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        vvCardVideo.setVisibility(View.GONE);
                        ivCardPhoto.setVisibility(View.VISIBLE);
                        btnMediaPlay.setVisibility(View.VISIBLE);
                        btnMediaPlay.setChecked(false);
                    }
                });
            } else if (clipMedium.equals(Constants.PHOTO)) {
                Uri uri = Uri.parse(mediaFile.getPath());
                ivCardPhoto.setImageURI(uri);
                ivCardPhoto.setVisibility(View.VISIBLE);
            } else if (clipMedium.equals(Constants.AUDIO)) {
                Uri myUri = Uri.parse(mediaFile.getPath());
                final MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                //set background image (using medium cliptype image as placeholder)
                ivCardPhoto.setImageDrawable(mContext.getResources().getDrawable(R.drawable.cliptype_medium));
                ivCardPhoto.setVisibility(View.VISIBLE);

                //set up media player
                try {
                    mediaPlayer.setDataSource(mContext, myUri);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                btnMediaPlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mediaPlayer.seekTo(5);
                            mediaPlayer.start();
                        } else {
                            mediaPlayer.pause();
                        }
                    }
                });

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer arg0) {
                        btnMediaPlay.setChecked(false);
                    }
                });

                mediaPlayer.seekTo(5);
                btnMediaPlay.setVisibility(View.VISIBLE);
            } else {
                //TODO handle invalid-medium error
            }
        }

        return view;
    }

    //returns stored mediaPath (if exists) or exampleMediaPath (if exists)
    public File getValidFile(String mediaPath, String exampleMediaPath) {
        File mediaFile = null;

        if (mediaPath != null) {
            mediaFile = MediaHelper.loadFileFromPath(mCardModel.getStoryPathReference().buildPath(mediaPath));
        } else if (exampleMediaPath != null) {
            mediaFile = MediaHelper.loadFileFromPath(mCardModel.getStoryPathReference().buildPath(exampleMediaPath));
        }

        return mediaFile;
    }
}
