/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                        (C) 2013-2016 Nikolai Kudashov
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 ******************************************************************************/


package com.b44t.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.AnimatorListenerAdapterProxy;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.AudioPlayerActivity;

public class PlayerView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private ImageView playButton;
    private TextView titleTextView;
    private MessageObject lastMessageObject;
    private AnimatorSet animatorSet;
    private float yPosition;
    private BaseFragment fragment;
    private float topPadding;
    private boolean visible;

    public PlayerView(Context context, BaseFragment parentFragment) {
        super(context);

        fragment = parentFragment;
        visible = true;
        ((ViewGroup) fragment.getFragmentView()).setClipToPadding(false);

        setTag(1);
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.INAPP_PLAYER_BACKGROUND_COLOR);
        addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 0));

        View shadow = new View(context);
        shadow.setBackgroundResource(R.drawable.header_shadow);
        addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.LEFT | Gravity.TOP, 0, 36, 0, 0));

        playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        addView(playButton, LayoutHelper.createFrame(36, 36, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 0));
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MediaController.getInstance().isAudioPaused()) {
                    MediaController.getInstance().playAudio(MediaController.getInstance().getPlayingMessageObject());
                } else {
                    MediaController.getInstance().pauseAudio(MediaController.getInstance().getPlayingMessageObject());
                }
            }
        });

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.INAPP_PLAYER_TITLE_TEXT_COLOR);
        titleTextView.setMaxLines(1);
        titleTextView.setLines(1);
        titleTextView.setSingleLine(true);
        titleTextView.setEllipsize(TextUtils.TruncateAt.END);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.LEFT | Gravity.TOP, 35, 0, 36, 0));

        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.miniplayer_close);
        closeButton.setScaleType(ImageView.ScaleType.CENTER);
        addView(closeButton, LayoutHelper.createFrame(36, 36, Gravity.RIGHT | Gravity.TOP));
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaController.getInstance().cleanupPlayer(true, true);
            }
        });

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
                if (messageObject != null && messageObject.isMusic() && fragment != null) {
                    fragment.presentFragment(new AudioPlayerActivity());
                }
            }
        });
    }

    public float getTopPadding() {
        return topPadding;
    }

    public void setTopPadding(float value) {
        topPadding = value;
        if (fragment != null) {
            View view = fragment.getFragmentView();
            if (view != null) {
                view.setPadding(0, (int) topPadding, 0, 0);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        topPadding = 0;
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidStarted);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidStarted);
        checkPlayer(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, AndroidUtilities.dp(39));
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.audioDidStarted || id == NotificationCenter.audioPlayStateChanged || id == NotificationCenter.audioDidReset) {
            checkPlayer(false);
        }
    }

    private void checkPlayer(boolean create) {
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        View fragmentView = fragment.getFragmentView();
        if (!create && fragmentView != null) {
            if (fragmentView.getParent() == null || ((View) fragmentView.getParent()).getVisibility() != VISIBLE) {
                create = true;
            }
        }
        if (messageObject == null || messageObject.getId() == 0/* || !messageObject.isMusic()*/) {
            lastMessageObject = null;
            if (visible) {
                visible = false;
                if (create) {
                    if (getVisibility() != GONE) {
                        setVisibility(GONE);
                    }
                    setTopPadding(0);
                } else {
                    if (animatorSet != null) {
                        animatorSet.cancel();
                        animatorSet = null;
                    }
                    animatorSet = new AnimatorSet();
                    animatorSet.playTogether(ObjectAnimator.ofFloat(this, "translationY", -AndroidUtilities.dp(36)),
                            ObjectAnimator.ofFloat(this, "topPadding", 0));
                    animatorSet.setDuration(200);
                    animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (animatorSet != null && animatorSet.equals(animation)) {
                                setVisibility(GONE);
                                animatorSet = null;
                            }
                        }
                    });
                    animatorSet.start();
                }
            }
        } else {
            if (create && topPadding == 0) {
                setTopPadding(AndroidUtilities.dp(36));
                setTranslationY(0);
                yPosition = 0;
            }
            if (!visible) {
                if (!create) {
                    if (animatorSet != null) {
                        animatorSet.cancel();
                        animatorSet = null;
                    }
                    animatorSet = new AnimatorSet();
                    animatorSet.playTogether(ObjectAnimator.ofFloat(this, "translationY", -AndroidUtilities.dp(36), 0),
                            ObjectAnimator.ofFloat(this, "topPadding", AndroidUtilities.dp(36)));
                    animatorSet.setDuration(200);
                    animatorSet.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (animatorSet != null && animatorSet.equals(animation)) {
                                animatorSet = null;
                            }
                        }
                    });
                    animatorSet.start();
                }
                visible = true;
                setVisibility(VISIBLE);
            }
            if (MediaController.getInstance().isAudioPaused()) {
                playButton.setImageResource(R.drawable.miniplayer_play);
            } else {
                playButton.setImageResource(R.drawable.miniplayer_pause);
            }
            if (lastMessageObject != messageObject) {
                lastMessageObject = messageObject;
                SpannableStringBuilder stringBuilder;
                if (lastMessageObject.isVoice()) {
                    stringBuilder = new SpannableStringBuilder(String.format("%s %s", messageObject.getMusicAuthor(), messageObject.getMusicTitle()));
                    titleTextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                } else {
                    stringBuilder = new SpannableStringBuilder(String.format("%s - %s", messageObject.getMusicAuthor(), messageObject.getMusicTitle()));
                    titleTextView.setEllipsize(TextUtils.TruncateAt.END);
                }
                TypefaceSpan span = new TypefaceSpan(Typeface.DEFAULT_BOLD, 0, Theme.INAPP_PLAYER_PERFORMER_TEXT_COLOR);
                stringBuilder.setSpan(span, 0, messageObject.getMusicAuthor().length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                titleTextView.setText(stringBuilder);
            }
        }
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        yPosition = translationY;
        invalidate();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        int restoreToCount = canvas.save();
        if (yPosition < 0) {
            canvas.clipRect(0, (int) -yPosition, child.getMeasuredWidth(), AndroidUtilities.dp(39));
        }
        final boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreToCount);
        return result;
    }
}
