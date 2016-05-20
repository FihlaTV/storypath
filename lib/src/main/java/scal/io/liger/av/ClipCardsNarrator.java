package scal.io.liger.av;

import timber.log.Timber;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import scal.io.liger.Constants;
import scal.io.liger.MediaHelper;
import scal.io.liger.R;
import scal.io.liger.model.AudioClip;
import scal.io.liger.model.ClipCard;
import scal.io.liger.model.ClipMetadata;
import scal.io.liger.model.MediaFile;

/**
 * Plays a collection of ClipCards, as well as a secondary audio track.
 *
 * Development Note : All methods prefixed with '_' are intended for exclusive
 * use by {@link android.os.Handler#handleMessage(android.os.Message)} and other '_' prefixed methods.
 *
 * Created by davidbrodsky on 12/12/14.
 */
public class ClipCardsNarrator extends ClipCardsPlayer {
    public final String TAG = getClass().getSimpleName();

    private AudioManager mAudioManager;
    private MediaRecorderWrapper mRecorder;
    private RecordNarrationState mRecordNarrationState;
    private NarrationListener mListener;
    private Pair<Integer, Integer> mSelectedClipIndexes;

    public static enum RecordNarrationState {

        /** Done / Record Buttons present */
        READY,

        /** Recording countdown then Pause / Stop Buttons present */
        RECORDING,

        /** Recording paused. Resume / Stop Buttons present */
        PAUSED,

        /** Recording stopped. Done / Redo Buttons present */
        STOPPED

    }

    protected static class ClipCardsNarratorHandler extends ClipCardsPlayerHandler {

        // NOTE : Avoid conflicts with ClipCardsPlayerHandler constants
        public static final int START_REC_NARRATION = -1;
        public static final int STOP_REC_NARRATION  = -2;

        private WeakReference<ClipCardsNarrator> mWeakNarrator;

        public ClipCardsNarratorHandler(ClipCardsPlayer player) {
            super(player);
            mWeakNarrator = new WeakReference<>((ClipCardsNarrator) player);
        }

        @Override
        public void handleMessage (Message msg) {
            ClipCardsNarrator narrator = mWeakNarrator.get();
            if (narrator == null) {
                Timber.w("ClipCardsNarrator.handleMessage: narrator is null!");
                return;
            }

            Object obj = msg.obj;
            switch(msg.what) {
                case START_REC_NARRATION:
                    if (obj != null && obj instanceof Pair)
                        narrator._startRecordingNarration((Pair<Integer, Integer>) obj);
                    else
                        narrator._startRecordingNarration();
                    break;
                case STOP_REC_NARRATION:
                    narrator._stopRecordingNarration();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public interface NarrationListener {
        public void onNarrationFinished(AudioClip audioClip, MediaFile narration);
    }

    /**
     * Create a new CLipCardsNarrator.
     *
     * @param container the container into which the player should be inflated
     * @param clipCards the in-order list of cards to allow recording narration for
     */
    public ClipCardsNarrator(@NonNull FrameLayout container,
                             @NonNull List<ClipCard> clipCards,
                             @NonNull List<AudioClip> audioClips) throws IOException {

        super(container, clipCards, audioClips);
        container.setKeepScreenOn(true);
        mHandler = new ClipCardsNarratorHandler(this);
        _changeRecordNarrationState(RecordNarrationState.READY);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        String fileNameNarration = "narration-" + new java.util.Date().getTime();
        mRecorder = new MediaRecorderWrapper(mContext, MediaHelper.getAudioDirectory(mContext),fileNameNarration);
    }

    private void _changeRecordNarrationState(RecordNarrationState newState) {
        Log.i(TAG, "Changing state to " + newState.toString());
        mRecordNarrationState = newState;
    }

    public void setNarrationListener(NarrationListener listener) {
        mListener = listener;
    }

    public void startRecordingNarrationForCards(@NonNull List<ClipCard> selectedCards) {
        Pair<Integer, Integer> indexes = Pair.create(mClipCards.indexOf(selectedCards.get(0)),
                                                     mClipCards.indexOf(selectedCards.get(selectedCards.size()-1)));

        mHandler.sendMessage(mHandler.obtainMessage(ClipCardsNarratorHandler.START_REC_NARRATION, indexes));
    }

    public void startRecordingNarration() {
        mHandler.sendMessage(mHandler.obtainMessage(ClipCardsNarratorHandler.START_REC_NARRATION));
    }

    /**
     * Start recording narration for a selection of {@link #mClipCards}
     *
     * @param indexes the first and last index specifying the selection within {@link #mClipCards}
     */
    private void _startRecordingNarration(@Nullable Pair<Integer, Integer> indexes) {
        _changeRecordNarrationState(RecordNarrationState.RECORDING);

        // Mute all media volume if we're recording narration and headphones
        // are not plugged in. Note that because we're not altering the media routing
        // of our MediaPlayers, if isWiredHeadsetOn returns true it should be a
        // safe assumption that the media will indeed be routed there per system default.
        //noinspection deprecation
        if (!mAudioManager.isWiredHeadsetOn() && !mAudioManager.isBluetoothA2dpOn()) {
            setPlaySecondaryTracks(false);
            setVolume(MUTE_VOLUME);
        }

        mSelectedClipIndexes = indexes;

        if (mRecorder.startRecording()) {
            Toast.makeText(mContext, mContext.getString(R.string.recording_narration), Toast.LENGTH_SHORT).show();
        } else {
            Timber.e("startRecording failed");
            Toast.makeText(mContext, mContext.getString(R.string.could_not_start_narration), Toast.LENGTH_SHORT).show();
            return;
        }

        _advanceToClip(mMainPlayer,
                       mClipCards.get(indexes == null ? 0 : indexes.first),
                       false);

        _startPlayback();
    }

    private void _startRecordingNarration() {
        _startRecordingNarration(null);
    }

    public void stopRecordingNarration() {
        mHandler.sendMessage(mHandler.obtainMessage(ClipCardsNarratorHandler.STOP_REC_NARRATION));
    }

    public void _stopRecordingNarration() {
        _stopRecordingNarration(true);
    }

    public void _stopRecordingNarration(boolean stopPlayback) {
        // Unmute volume in anticipation of the user immediately previewing the recording
        // by touching the play button. If recording is initiated again, the volume will be muted
        // appropriately on _startRecordingNarration
        setVolume(FULL_VOLUME); // FIXME we should respect the users manual mute state they expressed via the button
        setPlaySecondaryTracks(true);
        _changeRecordNarrationState(RecordNarrationState.STOPPED);
        MediaFile mf = mRecorder.stopRecording();
        mRecorder.reset();

        if (mListener != null && mf != null) {
            AudioClip audioClip = new AudioClip(null,                           // position_clip_id
                                                mSelectedClipIndexes.first,     // position_index
                                                1f,                             // volume
                                                mSelectedClipIndexes.second - mSelectedClipIndexes.first + 1,   // Clip span
                                                true,                           // truncate
                                                false,                          // overlap
                                                false,                          // fill_repeat
                                                UUID.randomUUID().toString());  // uuid

            mListener.onNarrationFinished(audioClip, mf);
        } else {
            Timber.w("Narration recorded without a NarrationListener set. Narration data will be lost");
        }
        if (stopPlayback) _stopPlayback();
    }

    public int getMaxRecordingAmplitude() {
        return mRecorder == null ? 0 : mRecorder.getMaxAmplitude();
    }

    public RecordNarrationState getState() {
        return mRecordNarrationState;
    }

    @Override
    protected void _advanceToNextClip(MediaPlayer player) {

        if (mSelectedClipIndexes != null && mSelectedClipIndexes.second == mClipCards.indexOf(mCurrentlyPlayingCard)) {
            if (mRecordNarrationState == RecordNarrationState.RECORDING) {
                Log.i(TAG, "Will stop recording. Current clip exceeds narration selection");
                _stopRecordingNarration(true);
            } else {
                _stopPlayback();
            }
            _advanceToClip(player, mClipCards.get(mSelectedClipIndexes.first), false);
            return;
        }

        super._advanceToNextClip(player);

        if (!stateIs(PlayerState.PLAYING) && mRecordNarrationState == RecordNarrationState.RECORDING) {
            _stopRecordingNarration(false); // super._advanceToNextClip(player) stopped playback
        }
    }

    @Override
    protected void _startPlayback() {
        if (mSelectedClipIndexes != null) {
            _advanceToClip(mMainPlayer,
                    mClipCards.get(mSelectedClipIndexes.first),
                    false);
        }
        super._startPlayback();
    }

    @Override
    protected void _release() {
        super._release();
        if (mRecorder != null) {
            mRecorder.release();
        }
    }
}
