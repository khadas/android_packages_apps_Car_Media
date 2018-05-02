package com.android.car.media;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.car.media.common.MediaItemMetadata;
import com.android.car.media.common.PlaybackModel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Common controller for displaying current track's metadata.
 */
public class MetadataController {

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("m:ss", Locale.US);

    @NonNull
    private final TextView mTitle;
    @NonNull
    private final TextView mSubtitle;
    @Nullable
    private final TextView mTime;
    @NonNull
    private final SeekBar mSeekBar;
    @Nullable
    private final ImageView mAlbumArt;

    @Nullable
    private PlaybackModel mModel;
    @Nullable
    private MediaItemMetadata mCurrentMetadata;

    private final PlaybackModel.PlaybackObserver mPlaybackObserver =
            new PlaybackModel.PlaybackObserver() {
                @Override
                public void onPlaybackStateChanged() {
                    updateState();
                }

                @Override
                public void onSourceChanged() {
                    updateState();
                    updateMetadata();
                }

                @Override
                public void onMetadataChanged() {
                    updateMetadata();
                }
            };

    /**
     * Create a new MetadataController that operates on the provided Views
     * @param title Displays the track's title. Must not be {@code null}.
     * @param subtitle Displays the track's artist. Must not be {@code null}.
     * @param time Displays the track's progress as text. May be {@code null}.
     * @param seekBar Displays the track's progress visually. Must not be {@code null}.
     * @param albumArt Displays the track's album art. May be {@code null}.
     */
    public MetadataController(@NonNull TextView title, @NonNull TextView subtitle,
            @Nullable TextView time, @NonNull SeekBar seekBar, @Nullable ImageView albumArt) {
        mTitle = title;
        mSubtitle = subtitle;
        mTime = time;
        mSeekBar = seekBar;
        mAlbumArt = albumArt;
    }

    /**
     * Registers the {@link PlaybackModel} this widget will use to follow playback state.
     * Consumers of this class must unregister the {@link PlaybackModel} by calling this method with
     * null.
     *
     * @param model {@link PlaybackModel} to subscribe, or null to unsubscribe.
     */
    public void setModel(@Nullable PlaybackModel model) {
        if (mModel != null) {
            mModel.unregisterObserver(mPlaybackObserver);
        }
        mModel = model;
        if (mModel != null) {
            mModel.registerObserver(mPlaybackObserver);
        }
    }

    private void updateState() {
        updateProgress();

        if (mModel != null && mModel.isPlaying()) {
            mSeekBar.post(mSeekBarRunnable);
        } else {
            mSeekBar.removeCallbacks(mSeekBarRunnable);
        }
    }

    private void updateMetadata() {
        MediaItemMetadata metadata = mModel != null ? mModel.getMetadata() : null;
        if (Objects.equals(mCurrentMetadata, metadata)) {
            return;
        }
        mCurrentMetadata = metadata;
        mTitle.setText(metadata != null ? metadata.getTitle() : null);
        mSubtitle.setText(metadata != null ? metadata.getSubtitle() : null);
        if (mAlbumArt != null) {
            MediaItemMetadata.updateImageView(mAlbumArt.getContext(), metadata, mAlbumArt, 0);
        }
    }

    private static final long SEEK_BAR_UPDATE_TIME_INTERVAL_MS = 1000;

    private final Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mModel == null || !mModel.isPlaying()) {
                return;
            }
            updateProgress();
            mSeekBar.postDelayed(this, SEEK_BAR_UPDATE_TIME_INTERVAL_MS);

        }
    };

    private void updateProgress() {
        if (mModel == null) {
            mTime.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
            return;
        }
        long maxProgress = mModel.getMaxProgress();
        int visibility = maxProgress > 0 ? View.VISIBLE : View.INVISIBLE;
        if (mTime != null) {
            String time = String.format("%s / %s",
                    TIME_FORMAT.format(new Date(mModel.getProgress())),
                    TIME_FORMAT.format(new Date(maxProgress)));
            mTime.setVisibility(visibility);
            mTime.setText(time);
        }
        mSeekBar.setVisibility(visibility);
        mSeekBar.setMax((int) mModel.getMaxProgress());
        mSeekBar.setProgress((int) mModel.getProgress());
    }


}
