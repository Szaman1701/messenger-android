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


package com.b44t.ui.Cells;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.ViewStructure;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ContactsController;
import com.b44t.messenger.ImageLoader;
import com.b44t.messenger.ImageReceiver;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MediaController;
import com.b44t.messenger.FileLoader;
import com.b44t.messenger.FileLog;
import com.b44t.messenger.MessageObject;
import com.b44t.messenger.MessagesController;
import com.b44t.messenger.MrChat;
import com.b44t.messenger.MrContact;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.R;
import com.b44t.messenger.UserObject;
import com.b44t.messenger.browser.Browser;
import com.b44t.messenger.TLRPC;
import com.b44t.ui.Components.AvatarDrawable;
import com.b44t.ui.Components.LinkPath;
import com.b44t.ui.Components.SeekBar;
import com.b44t.ui.Components.SeekBarWaveform;
import com.b44t.ui.Components.StaticLayoutEx;
import com.b44t.ui.ActionBar.Theme;
import com.b44t.ui.PhotoViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static com.b44t.messenger.AndroidUtilities.*;

public class ChatMessageCell extends BaseCell implements SeekBar.SeekBarDelegate, ImageReceiver.ImageReceiverDelegate {

    public interface ChatMessageCellDelegate {
        void didPressedUserAvatar(ChatMessageCell cell, TLRPC.User user);
        void didPressedChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId);
        void didPressedCancelSendButton(ChatMessageCell cell);
        void didLongPressed(ChatMessageCell cell);
        void didPressedUrl(MessageObject messageObject, ClickableSpan url, boolean longPress);
        void didPressedImage(ChatMessageCell cell);
        void didPressedShare(ChatMessageCell cell);
        void didPressedOther(ChatMessageCell cell);
        boolean needPlayAudio(MessageObject messageObject);
        boolean canPerformActions();
    }

    private final static int DOCUMENT_ATTACH_TYPE_NONE = 0;
    private final static int DOCUMENT_ATTACH_TYPE_DOCUMENT = 1;
    private final static int DOCUMENT_ATTACH_TYPE_GIF = 2;
    private final static int DOCUMENT_ATTACH_TYPE_AUDIO = 3;
    private final static int DOCUMENT_ATTACH_TYPE_VIDEO = 4;
    private final static int DOCUMENT_ATTACH_TYPE_MUSIC = 5;
    private final static int DOCUMENT_ATTACH_TYPE_STICKER = 6;

    private int textX;
    private int textY;
    private int totalHeight;
    private int keyboardHeight;
    private int linkBlockNum;
    private int linkSelectionBlockNum;

    private Rect scrollRect = new Rect();

    private int lastVisibleBlockNum;
    private int firstVisibleBlockNum;
    private int totalVisibleBlocksCount;
    private boolean needNewVisiblePart;

    //private RadialProgress radialProgress;
    private ImageReceiver photoImage;

    private boolean disallowLongPress;

    private boolean drawImageButton;
    private int documentAttachType;
    private TLRPC.Document documentAttach;
    private boolean drawPhotoImage;
    private final boolean hasLinkPreview = false;
    private int mediaOffsetY;
    private StaticLayout titleLayout;

    private StaticLayout docTitleLayout;
    private int docTitleOffsetX;

    private StaticLayout captionLayout;
    private int captionX;
    private int captionY;
    private int captionHeight;

    private StaticLayout infoLayout;
    private int infoWidth;

    private String currentUrl;

    private int buttonX;
    private int buttonY;
    private int buttonState;
    private int buttonPressed;
    private int otherX;
    private int otherY;
    private boolean imagePressed;
    private boolean otherPressed;
    private boolean photoNotSet;
    private RectF deleteProgressRect = new RectF();
    private RectF rect = new RectF();
    private TLRPC.PhotoSize currentPhotoObject;
    private TLRPC.PhotoSize currentPhotoObjectThumb;
    private String currentPhotoFilter;
    private String currentPhotoFilterThumb;
    private boolean cancelLoading;

    private static TextPaint infoPaint;
    private static TextPaint docNamePaint;
    private static Paint docBackPaint;
    private static Paint urlPaint;
    private static Paint urlSelectionPaint;
    private static TextPaint durationPaint;

    private ClickableSpan pressedLink;
    private int pressedLinkType;
    private boolean linkPreviewPressed;
    private ArrayList<LinkPath> urlPathCache = new ArrayList<>();
    private ArrayList<LinkPath> urlPath = new ArrayList<>();
    private ArrayList<LinkPath> urlPathSelection = new ArrayList<>();

    private int iconX, iconY;

    private boolean useSeekBarWaweform;
    private SeekBar seekBar;
    private SeekBarWaveform seekBarWaveform;
    private int seekBarX;
    private int seekBarY;

    private StaticLayout durationLayout;
    private String lastTimeString;
    private int timeWidthAudio;
    private int timeAudioX;

    private static TextPaint audioTimePaint;
    private static TextPaint audioTitlePaint;
    private static TextPaint audioPerformerPaint;
    private static TextPaint contactPhonePaint;

    private StaticLayout songLayout;
    private int songX;

    private StaticLayout performerLayout;
    private int performerX;

    //private int TAG;

    public boolean isChat;
    private boolean isPressed;
    private boolean isHighlighted;
    private boolean mediaBackground;
    private boolean isCheckPressed = true;
    private boolean wasLayout;
    private boolean isAvatarVisible;
    private boolean drawBackground = true;
    private int substractBackgroundHeight;
    private boolean allowAssistant;
    private Drawable currentBackgroundDrawable;
    private MessageObject currentMessageObject;
    private final int viaWidth = 0;
    private int viaNameWidth;
    private int availableTimeWidth;

    private static TextPaint timePaint;
    private static TextPaint namePaint;
    private static TextPaint forwardNamePaint;
    private static TextPaint replyNamePaint;
    private static TextPaint replyTextPaint;
    private static Paint replyLinePaint;

    private int backgroundWidth = 100;

    private int layoutWidth;
    private int layoutHeight;

    private ImageReceiver avatarImage;
    private AvatarDrawable avatarDrawable;
    private boolean avatarPressed;
    private boolean forwardNamePressed;
    private boolean forwardBotPressed;

    private StaticLayout replyNameLayout;
    private StaticLayout replyTextLayout;
    private ImageReceiver replyImageReceiver;
    private int replyStartX;
    private int replyStartY;
    private int replyNameWidth;
    private float replyNameOffset;
    private int replyTextWidth;
    private float replyTextOffset;
    private boolean needReplyImage;
    private boolean replyPressed;
    private TLRPC.FileLocation currentReplyPhoto;

    private boolean drawShareButton;
    private boolean sharePressed;
    private int shareStartX;
    private int shareStartY;

    private StaticLayout nameLayout;
    private int nameWidth;
    private float nameOffsetX;
    private float nameX;
    private float nameY;
    private boolean drawName;
    private boolean drawNameLayout;

    private StaticLayout[] forwardedNameLayout = new StaticLayout[2];
    private int forwardedNameWidth;
    private boolean drawForwardedName;
    private int forwardNameX;
    private int forwardNameY;
    private float forwardNameOffsetX[] = new float[2];

    private StaticLayout timeLayout;
    private int timeWidth;
    private int timeTextWidth;
    private int timeX;
    private String currentTimeString;
    private boolean drawTime = true;

    private TLRPC.User currentUser;
    private TLRPC.Chat currentChat;
    private TLRPC.FileLocation currentPhoto;
    private String currentNameString;

    private String currentForwardNameString;

    private ChatMessageCellDelegate delegate;

    private int namesOffset;

    private int lastSendState;
    private int lastViewsCount;

    public ChatMessageCell(Context context) {
        super(context);
        if (infoPaint == null) {
            infoPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            infoPaint.setTextSize(dp(12));

            docNamePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            docNamePaint.setTextSize(dp(16));

            docBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            urlPaint = new Paint();
            urlPaint.setColor(Theme.MSG_LINK_SELECT_BACKGROUND_COLOR);

            urlSelectionPaint = new Paint();
            urlSelectionPaint.setColor(Theme.MSG_TEXT_SELECT_BACKGROUND_COLOR);

            audioTimePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            audioTimePaint.setTextSize(dp(12));

            audioTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            audioTitlePaint.setTextSize(dp(16));

            audioPerformerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            audioPerformerPaint.setTextSize(dp(15));

            durationPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            durationPaint.setTextSize(dp(12));
            durationPaint.setColor(Theme.MSG_WEB_PREVIEW_DURATION_TEXT_COLOR);

            timePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(dp(12));

            namePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            namePaint.setTypeface(Typeface.DEFAULT_BOLD);
            namePaint.setTextSize(dp(14));

            forwardNamePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            forwardNamePaint.setTextSize(dp(14));

            replyNamePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            replyNamePaint.setTypeface(Typeface.DEFAULT_BOLD);
            replyNamePaint.setTextSize(dp(14));

            replyTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            replyTextPaint.setTextSize(dp(14));
            replyTextPaint.linkColor = Theme.MSG_LINK_TEXT_COLOR;

            replyLinePaint = new Paint();
        }
        avatarImage = new ImageReceiver(this);
        avatarImage.setRoundRadius(dp(21));
        avatarDrawable = new AvatarDrawable();
        replyImageReceiver = new ImageReceiver(this);
        //TAG = MediaController.getInstance().generateObserverTag();

        photoImage = new ImageReceiver(this);
        photoImage.setDelegate(this);
        //radialProgress = new RadialProgress(this);
        seekBar = new SeekBar(context);
        seekBar.setDelegate(this);
        seekBarWaveform = new SeekBarWaveform(context);
        seekBarWaveform.setDelegate(this);
        seekBarWaveform.setParentView(this);
    }

    private void resetPressedLink(int type) {
        if (pressedLink == null || pressedLinkType != type && type != -1) {
            return;
        }
        resetUrlPaths(false);
        pressedLink = null;
        pressedLinkType = -1;
        invalidate();
    }

    private void resetUrlPaths(boolean text) {
        if (text) {
            if (urlPathSelection.isEmpty()) {
                return;
            }
            urlPathCache.addAll(urlPathSelection);
            urlPathSelection.clear();
        } else {
            if (urlPath.isEmpty()) {
                return;
            }
            urlPathCache.addAll(urlPath);
            urlPath.clear();
        }
    }

    private LinkPath obtainNewUrlPath(boolean text) {
        LinkPath linkPath;
        if (!urlPathCache.isEmpty()) {
            linkPath = urlPathCache.get(0);
            urlPathCache.remove(0);
        } else {
            linkPath = new LinkPath();
        }
        if (text) {
            urlPathSelection.add(linkPath);
        } else {
            urlPath.add(linkPath);
        }
        return linkPath;
    }

    private boolean checkTextBlockMotionEvent(MotionEvent event) {
        if (currentMessageObject.type != MessageObject.MO_TYPE0_TEXT || currentMessageObject.textLayoutBlocks == null || currentMessageObject.textLayoutBlocks.isEmpty() || !(currentMessageObject.messageText instanceof Spannable)) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP && pressedLinkType == 1) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x >= textX && y >= textY && x <= textX + currentMessageObject.textWidth && y <= textY + currentMessageObject.textHeight) {
                y -= textY;
                int blockNum = 0;
                for (int a = 0; a < currentMessageObject.textLayoutBlocks.size(); a++) {
                    if (currentMessageObject.textLayoutBlocks.get(a).textYOffset > y) {
                        break;
                    }
                    blockNum = a;
                }
                try {
                    MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(blockNum);
                    x -= textX - (int) Math.ceil(block.textXOffset);
                    y -= block.textYOffset;
                    final int line = block.textLayout.getLineForVertical(y);
                    final int off = block.textLayout.getOffsetForHorizontal(line, x) + block.charactersOffset;

                    final float left = block.textLayout.getLineLeft(line);
                    if (left <= x && left + block.textLayout.getLineWidth(line) >= x) {
                        Spannable buffer = (Spannable) currentMessageObject.messageText;
                        ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
                        boolean ignore = false;
                        if ( link.length == 0 ) {
                            ignore = true;
                        }
                        if (!ignore) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                pressedLink = link[0];
                                linkBlockNum = blockNum;
                                pressedLinkType = 1;
                                resetUrlPaths(false);
                                try {
                                    LinkPath path = obtainNewUrlPath(false);
                                    int start = buffer.getSpanStart(pressedLink) - block.charactersOffset;
                                    int end = buffer.getSpanEnd(pressedLink);
                                    int length = block.textLayout.getText().length();
                                    path.setCurrentLayout(block.textLayout, start, 0);
                                    block.textLayout.getSelectionPath(start, end - block.charactersOffset, path);
                                    if (end >= block.charactersOffset + length) {
                                        for (int a = blockNum + 1; a < currentMessageObject.textLayoutBlocks.size(); a++) {
                                            MessageObject.TextLayoutBlock nextBlock = currentMessageObject.textLayoutBlocks.get(a);
                                            length = nextBlock.textLayout.getText().length();
                                            ClickableSpan[] nextLink = buffer.getSpans(nextBlock.charactersOffset, nextBlock.charactersOffset, ClickableSpan.class);
                                            if (nextLink == null || nextLink.length == 0 || nextLink[0] != pressedLink) {
                                                break;
                                            }
                                            path = obtainNewUrlPath(false);
                                            path.setCurrentLayout(nextBlock.textLayout, 0, nextBlock.height);
                                            nextBlock.textLayout.getSelectionPath(0, end - nextBlock.charactersOffset, path);
                                            if (end < block.charactersOffset + length - 1) {
                                                break;
                                            }
                                        }
                                    }
                                    if (start < 0) {
                                        for (int a = blockNum - 1; a >= 0; a--) {
                                            MessageObject.TextLayoutBlock nextBlock = currentMessageObject.textLayoutBlocks.get(a);
                                            length = nextBlock.textLayout.getText().length();
                                            ClickableSpan[] nextLink = buffer.getSpans(nextBlock.charactersOffset + length - 1, nextBlock.charactersOffset + length - 1, ClickableSpan.class);
                                            if (nextLink == null || nextLink.length == 0 || nextLink[0] != pressedLink) {
                                                break;
                                            }
                                            path = obtainNewUrlPath(false);
                                            start = buffer.getSpanStart(pressedLink) - nextBlock.charactersOffset;
                                            path.setCurrentLayout(nextBlock.textLayout, start, -nextBlock.height);
                                            nextBlock.textLayout.getSelectionPath(start, buffer.getSpanEnd(pressedLink) - nextBlock.charactersOffset, path);
                                            if (start >= 0) {
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    FileLog.e("messenger", e);
                                }
                                invalidate();
                                return true;
                            } else {
                                if (link[0] == pressedLink) {
                                    delegate.didPressedUrl(currentMessageObject, pressedLink, false);
                                    resetPressedLink(1);
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            } else {
                resetPressedLink(1);
            }
        }
        return false;
    }

    private boolean checkCaptionMotionEvent(MotionEvent event) {
        if (!(currentMessageObject.caption instanceof Spannable) || captionLayout == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || (linkPreviewPressed || pressedLink != null) && event.getAction() == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x >= captionX && x <= captionX + backgroundWidth && y >= captionY && y <= captionY + captionHeight) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        x -= captionX;
                        y -= captionY;
                        final int line = captionLayout.getLineForVertical(y);
                        final int off = captionLayout.getOffsetForHorizontal(line, x);

                        final float left = captionLayout.getLineLeft(line);
                        if (left <= x && left + captionLayout.getLineWidth(line) >= x) {
                            Spannable buffer = (Spannable) currentMessageObject.caption;
                            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
                            boolean ignore = false;
                            if (link.length == 0 ) {
                                ignore = true;
                            }
                            if (!ignore) {
                                pressedLink = link[0];
                                pressedLinkType = 3;
                                resetUrlPaths(false);
                                try {
                                    LinkPath path = obtainNewUrlPath(false);
                                    int start = buffer.getSpanStart(pressedLink);
                                    path.setCurrentLayout(captionLayout, start, 0);
                                    captionLayout.getSelectionPath(start, buffer.getSpanEnd(pressedLink), path);
                                } catch (Exception e) {
                                    FileLog.e("messenger", e);
                                }
                                invalidate();
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                } else if (pressedLinkType == 3) {
                    delegate.didPressedUrl(currentMessageObject, pressedLink, false);
                    resetPressedLink(3);
                    return true;
                }
            } else {
                resetPressedLink(3);
            }
        }
        return false;
    }

    private boolean checkOtherButtonMotionEvent(MotionEvent event) {
        if (documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT && documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC && documentAttachType != DOCUMENT_ATTACH_TYPE_VIDEO && documentAttachType != DOCUMENT_ATTACH_TYPE_GIF && currentMessageObject.type != MessageObject.MO_TYPE8_GIF) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (x >= otherX - dp(20) && x <= otherX + dp(20) && y >= otherY - dp(4) && y <= otherY + dp(30)) {
                otherPressed = true;
                result = true;
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (otherPressed) {
                    otherPressed = false;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    delegate.didPressedOther(this);
                }
            }
        }
        return result;
    }

    private boolean checkPhotoImageMotionEvent(MotionEvent event) {
        if (!drawPhotoImage && documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (buttonState != -1 && x >= buttonX && x <= buttonX + dp(48) && y >= buttonY && y <= buttonY + dp(48)) {
                buttonPressed = 1;
                invalidate();
                result = true;
            } else {
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                    if (x >= photoImage.getImageX() && x <= photoImage.getImageX() + backgroundWidth - dp(50) && y >= photoImage.getImageY() && y <= photoImage.getImageY() + photoImage.getImageHeight()) {
                        imagePressed = true;
                        result = true;
                    }
                } else if (currentMessageObject.type != MessageObject.MO_TYPE13_STICKER || currentMessageObject.getInputStickerSet() != null) {
                    if (x >= photoImage.getImageX() && x <= photoImage.getImageX() + backgroundWidth && y >= photoImage.getImageY() && y <= photoImage.getImageY() + photoImage.getImageHeight()) {
                        imagePressed = true;
                        result = true;
                    }
                }
            }
            if (imagePressed) {
                if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF && buttonState == -1 && MediaController.getInstance().canAutoplayGifs() && photoImage.getAnimation() == null) {
                    imagePressed = false;
                    result = false;
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (buttonPressed == 1) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressedButton(false);
                    //radialProgress.swapBackground(getDrawableForCurrentState());
                    invalidate();
                } else if (imagePressed) {
                    imagePressed = false;
                    if (buttonState == -1 || buttonState == 2 || buttonState == 3) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didClickedImage();
                    } else if (buttonState == 0 && documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didPressedButton(false);
                    }
                    invalidate();
                }
            }
        }
        return result;
    }

    private boolean checkAudioMotionEvent(MotionEvent event) {
        if (documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO && documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();
        boolean result;
        if (useSeekBarWaweform) {
            result = seekBarWaveform.onTouch(event.getAction(), event.getX() - seekBarX - dp(13), event.getY() - seekBarY);
        } else {
            result = seekBar.onTouch(event.getAction(), event.getX() - seekBarX, event.getY() - seekBarY);
        }
        if (result) {
            if (!useSeekBarWaweform && event.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (useSeekBarWaweform && !seekBarWaveform.isStartDraging() && event.getAction() == MotionEvent.ACTION_UP) {
                didPressedButton(true);
            }
            disallowLongPress = true;
            invalidate();
        } else {
            int side = dp(36);
            boolean area;
            if (buttonState == 0 || buttonState == 1 || buttonState == 2) {
                area = x >= buttonX - dp(12) && x <= buttonX - dp(12) + backgroundWidth && y >= namesOffset + mediaOffsetY && y <= layoutHeight;
            } else {
                area = x >= buttonX && x <= buttonX + side && y >= buttonY && y <= buttonY + side;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (area) {
                    buttonPressed = 1;
                    invalidate();
                    result = true;
                    //radialProgress.swapBackground(getDrawableForCurrentState());
                }
            } else if (buttonPressed != 0) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressedButton(true);
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    buttonPressed = 0;
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!area) {
                        buttonPressed = 0;
                        invalidate();
                    }
                }
                //radialProgress.swapBackground(getDrawableForCurrentState());
            }
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMessageObject == null || !delegate.canPerformActions()) {
            return super.onTouchEvent(event);
        }

        disallowLongPress = false;

        boolean result = checkTextBlockMotionEvent(event);
        if (!result) {
            result = checkOtherButtonMotionEvent(event);
        }
        /*if (!result) {
            result = checkLinkPreviewMotionEvent(event);
        }*/
        if (!result) {
            result = checkCaptionMotionEvent(event);
        }
        if (!result) {
            result = checkAudioMotionEvent(event);
        }
        if (!result) {
            result = checkPhotoImageMotionEvent(event);
            if( result ) {
                result = true;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            buttonPressed = 0;
            linkPreviewPressed = false;
            otherPressed = false;
            imagePressed = false;
            result = false;
            resetPressedLink(-1);
        }
        if (!disallowLongPress && result && event.getAction() == MotionEvent.ACTION_DOWN) {
            startCheckLongPress();
        }
        if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) {
            cancelCheckLongPress();
        }

        if (!result) {
            float x = event.getX();
            float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (delegate == null || delegate.canPerformActions()) {
                    if (isAvatarVisible && avatarImage.isInsideImage(x, y)) {
                        avatarPressed = true;
                        result = true;
                    } else if (drawForwardedName && forwardedNameLayout[0] != null && x >= forwardNameX && x <= forwardNameX + forwardedNameWidth && y >= forwardNameY && y <= forwardNameY + dp(32)) {
                        if (viaWidth != 0 && x >= forwardNameX + viaNameWidth + dp(4)) {
                            forwardBotPressed = true;
                        } else {
                            forwardNamePressed = true;
                        }
                        result = true;
                    } else if (drawNameLayout && nameLayout != null && viaWidth != 0 && x >= nameX + viaNameWidth && x <= nameX + viaNameWidth + viaWidth && y >= nameY - dp(4) && y <= nameY + dp(20)) {
                        forwardBotPressed = true;
                        result = true;
                    } else if (drawShareButton && x >= shareStartX && x <= shareStartX + dp(40) && y >= shareStartY && y <= shareStartY + dp(32)) {
                        sharePressed = true;
                        result = true;
                        invalidate();
                    }
                    if (result) {
                        startCheckLongPress();
                    }
                }
            } else {
                if (event.getAction() != MotionEvent.ACTION_MOVE) {
                    cancelCheckLongPress();
                }
                if (avatarPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        avatarPressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            if (currentUser != null) {
                                delegate.didPressedUserAvatar(this, currentUser);
                            } else if (currentChat != null) {
                                delegate.didPressedChannelAvatar(this, currentChat, 0);
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        avatarPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (isAvatarVisible && !avatarImage.isInsideImage(x, y)) {
                            avatarPressed = false;
                        }
                    }
                } else if (forwardNamePressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        forwardNamePressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        /*if (delegate != null) {
                            if (currentForwardUser != null) {
                                delegate.didPressedUserAvatar(this, currentForwardUser);
                            }
                        }*/
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        forwardNamePressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (!(x >= forwardNameX && x <= forwardNameX + forwardedNameWidth && y >= forwardNameY && y <= forwardNameY + dp(32))) {
                            forwardNamePressed = false;
                        }
                    }
                } else if (forwardBotPressed) {
                    forwardBotPressed = false;
                } else if (replyPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        replyPressed = false;
                        /*playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            delegate.didPressedReplyMessage(this, currentMessageObject.messageOwner.reply_to_msg_id);
                        }*/
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        replyPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (!(x >= replyStartX && x <= replyStartX + Math.max(replyNameWidth, replyTextWidth) && y >= replyStartY && y <= replyStartY + dp(35))) {
                            replyPressed = false;
                        }
                    }
                } else if (sharePressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        sharePressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            delegate.didPressedShare(this);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        sharePressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (!(x >= shareStartX && x <= shareStartX + dp(40) && y >= shareStartY && y <= shareStartY + dp(32))) {
                            sharePressed = false;
                        }
                    }
                    invalidate();
                }
            }
        }
        return result;
    }

    public void updateAudioProgress() {
        if (currentMessageObject == null || documentAttach == null) {
            return;
        }

        if (useSeekBarWaweform) {
            if (!seekBarWaveform.isDragging()) {
                seekBarWaveform.setProgress(currentMessageObject.audioProgress);
            }
        } else {
            if (!seekBar.isDragging()) {
                seekBar.setProgress(currentMessageObject.audioProgress);
            }
        }

        int duration = 0;
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            if (!MediaController.getInstance().isPlayingAudio(currentMessageObject)) {
                for (int a = 0; a < documentAttach.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                    if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                        duration = attribute.duration;
                        break;
                    }
                }
            } else {
                duration = currentMessageObject.audioProgressSec;
            }
            String timeString = String.format("%02d:%02d", duration / 60, duration % 60);
            if (lastTimeString == null || lastTimeString != null && !lastTimeString.equals(timeString)) {
                lastTimeString = timeString;
                timeWidthAudio = (int) Math.ceil(audioTimePaint.measureText(timeString));
                durationLayout = new StaticLayout(timeString, audioTimePaint, timeWidthAudio, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        } else {
            int currentProgress = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    duration = attribute.duration;
                    break;
                }
            }
            if (MediaController.getInstance().isPlayingAudio(currentMessageObject)) {
                currentProgress = currentMessageObject.audioProgressSec;
            }
            String timeString = String.format("%d:%02d / %d:%02d", currentProgress / 60, currentProgress % 60, duration / 60, duration % 60);
            if (lastTimeString == null || lastTimeString != null && !lastTimeString.equals(timeString)) {
                lastTimeString = timeString;
                int timeWidth = (int) Math.ceil(audioTimePaint.measureText(timeString));
                durationLayout = new StaticLayout(timeString, audioTimePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        }
        invalidate();
    }

    public void downloadAudioIfNeed() {
        if (documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO || documentAttach.size >= 1024 * 1024 * 5) {
            return;
        }
        if (buttonState == 2) {
            FileLoader.getInstance().loadFile(documentAttach, true, false);
            buttonState = 4;
            //radialProgress.setBackground(getDrawableForCurrentState(), false, false);
        }
    }

    public void setVisiblePart(int position, int height) {
        if (currentMessageObject == null || currentMessageObject.textLayoutBlocks == null) {
            return;
        }
        position -= textY;

        int newFirst = -1, newLast = -1, newCount = 0;

        int startBlock = 0;
        for (int a = 0; a < currentMessageObject.textLayoutBlocks.size(); a++) {
            if (currentMessageObject.textLayoutBlocks.get(a).textYOffset > position) {
                break;
            }
            startBlock = a;
        }

        for (int a = startBlock; a < currentMessageObject.textLayoutBlocks.size(); a++) {
            MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(a);
            float y = block.textYOffset;
            if (intersect(y, y + block.height, position, position + height)) {
                if (newFirst == -1) {
                    newFirst = a;
                }
                newLast = a;
                newCount++;
            } else if (y > position) {
                break;
            }
        }

        if (lastVisibleBlockNum != newLast || firstVisibleBlockNum != newFirst || totalVisibleBlocksCount != newCount) {
            lastVisibleBlockNum = newLast;
            firstVisibleBlockNum = newFirst;
            totalVisibleBlocksCount = newCount;
            invalidate();
        }
    }

    private boolean intersect(float left1, float right1, float left2, float right2) {
        if (left1 <= left2) {
            return right1 >= left2;
        }
        return left1 <= right2;
    }

    public static StaticLayout generateStaticLayout(CharSequence text, TextPaint paint, int maxWidth, int smallWidth, int linesCount, int maxLines) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
        int addedChars = 0;
        StaticLayout layout = new StaticLayout(text, paint, smallWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        for (int a = 0; a < linesCount; a++) {
            Layout.Directions directions = layout.getLineDirections(a);
            if (layout.getLineLeft(a) != 0 || layout.isRtlCharAt(layout.getLineStart(a)) || layout.isRtlCharAt(layout.getLineEnd(a))) {
                maxWidth = smallWidth;
            }
            int pos = layout.getLineEnd(a);
            if (pos == text.length()) {
                break;
            }
            pos--;
            if (stringBuilder.charAt(pos + addedChars) == ' ') {
                stringBuilder.replace(pos + addedChars, pos + addedChars + 1, "\n");
            } else if (stringBuilder.charAt(pos + addedChars) != '\n') {
                stringBuilder.insert(pos + addedChars, "\n");
                addedChars++;
            }
            if (a == layout.getLineCount() - 1 || a == maxLines - 1) {
                break;
            }
        }
        return StaticLayoutEx.createStaticLayout(stringBuilder, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, dp(1), false, TextUtils.TruncateAt.END, maxWidth, maxLines);
    }

    private void didClickedImage() {
        if (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO || currentMessageObject.type == MessageObject.MO_TYPE13_STICKER) {
            if (buttonState == -1) {
                delegate.didPressedImage(this);
            } else if (buttonState == 0) {
                didPressedButton(false);
            }
        } else if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF) {
            if (buttonState == -1) {
                if (MediaController.getInstance().canAutoplayGifs()) {
                    delegate.didPressedImage(this);
                } else {
                    buttonState = 2;
                    currentMessageObject.audioProgress = 1;
                    photoImage.setAllowStartAnimation(false);
                    photoImage.stopAnimation();
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, false);
                    invalidate();
                }
            } else if (buttonState == 2 || buttonState == 0) {
                didPressedButton(false);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
            if (buttonState == 0 || buttonState == 3) {
                didPressedButton(false);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            if (buttonState == -1) {
                delegate.didPressedImage(this);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            if (buttonState == -1) {
                TLRPC.WebPage webPage = currentMessageObject.messageOwner.media.webpage;
                Browser.openUrl(getContext(), webPage.url);
            }
        }
    }

    private boolean isPhotoDataChanged(MessageObject object) {
        if (object.type == MessageObject.MO_TYPE0_TEXT || object.type == MessageObject.MO_TYPE14_MUSIC) {
            return false;
        }
        if (currentPhotoObject == null || currentPhotoObject.location instanceof TLRPC.TL_fileLocationUnavailable) {
            return true;
        } else if (currentMessageObject != null && photoNotSet) {
            File cacheFile = FileLoader.getPathToMessage(currentMessageObject.messageOwner);
            if (cacheFile.exists()) { //TODO
                return true;
            }
        }
        return false;
    }

    private boolean isUserDataChanged() {
        if (currentMessageObject != null && (!hasLinkPreview && currentMessageObject.messageOwner.media != null &&  false/*currentMessageObject.messageOwner.media.webpage instanceof TLRPC.TL_webPage*/)) {
            return true;
        }
        if (currentMessageObject == null || currentUser == null && currentChat == null) {
            return false;
        }
        if (lastSendState != currentMessageObject.messageOwner.send_state) {
            return true;
        }
        if (lastViewsCount != currentMessageObject.messageOwner.views) {
            return true;
        }

        TLRPC.User newUser = null;
        TLRPC.Chat newChat = null;
        if (currentMessageObject.isFromUser()) {
            newUser = MessagesController.getInstance().getUser(currentMessageObject.messageOwner.from_id);
        } else if (currentMessageObject.messageOwner.from_id < 0) {
            newChat = MessagesController.getInstance().getChat(-currentMessageObject.messageOwner.from_id);
        } else if (currentMessageObject.messageOwner.post) {
            newChat = MessagesController.getInstance().getChat(currentMessageObject.messageOwner.to_id.channel_id);
        }
        TLRPC.FileLocation newPhoto = null;

        if (isAvatarVisible) {
            if (newUser != null && newUser.photo != null){
                newPhoto = newUser.photo.photo_small;
            } else if (newChat != null && newChat.photo != null) {
                newPhoto = newChat.photo.photo_small;
            }
        }

        if (replyTextLayout == null && currentMessageObject.replyMessageObject != null) {
            return true;
        }

        if (currentPhoto == null && newPhoto != null || currentPhoto != null && newPhoto == null || currentPhoto != null && newPhoto != null && (currentPhoto.local_id != newPhoto.local_id || currentPhoto.volume_id != newPhoto.volume_id)) {
            return true;
        }

        TLRPC.FileLocation newReplyPhoto = null;

        if (currentReplyPhoto == null && newReplyPhoto != null) {
            return true;
        }

        String newNameString = null;
        if (drawName && isChat && !currentMessageObject.isOutOwner()) {
            if (newUser != null) {
                newNameString = UserObject.getUserName(newUser);
            } else if (newChat != null) {
                newNameString = newChat.title;
            }
        }

        if (currentNameString == null && newNameString != null || currentNameString != null && newNameString == null || currentNameString != null && newNameString != null && !currentNameString.equals(newNameString)) {
            return true;
        }

        if (drawForwardedName) {
            newNameString = currentMessageObject.getForwardedName();
            return currentForwardNameString == null && newNameString != null || currentForwardNameString != null && newNameString == null || currentForwardNameString != null && newNameString != null && !currentForwardNameString.equals(newNameString);
        }
        return false;
    }

    public ImageReceiver getPhotoImage() {
        return photoImage;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
        replyImageReceiver.onDetachedFromWindow();
        photoImage.onDetachedFromWindow();
        //MediaController.getInstance().removeLoadingFileObserver(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
        replyImageReceiver.onAttachedToWindow();
        if (drawPhotoImage) {
            if (photoImage.onAttachedToWindow()) {
                updateButtonState(false);
            }
        } else {
            updateButtonState(false);
        }
    }

    @Override
    protected void onLongPress() {
        if (pressedLink instanceof URLSpan) {
            delegate.didPressedUrl(currentMessageObject, pressedLink, true);
            return;
        }
        resetPressedLink(-1);
        if (buttonPressed != 0 ) {
            buttonPressed = 0;
            invalidate();
        }
        if (delegate != null) {
            delegate.didLongPressed(this);
        }
    }

    public void setCheckPressed(boolean value, boolean pressed) {
        isCheckPressed = value;
        isPressed = pressed;
        //radialProgress.swapBackground(getDrawableForCurrentState());
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectedBackground());
        } else {
            seekBar.setSelected(isDrawSelectedBackground());
        }
        invalidate();
    }

    public void setHighlighted(boolean value) {
        if (isHighlighted == value) {
            return;
        }
        isHighlighted = value;
        //radialProgress.swapBackground(getDrawableForCurrentState());
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectedBackground());
        } else {
            seekBar.setSelected(isDrawSelectedBackground());
        }
        invalidate();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        //radialProgress.swapBackground(getDrawableForCurrentState());
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectedBackground());
        } else {
            seekBar.setSelected(isDrawSelectedBackground());
        }
        invalidate();
    }

    @Override
    public void onSeekBarDrag(float progress) {
        if (currentMessageObject == null) {
            return;
        }
        currentMessageObject.audioProgress = progress;
        MediaController.getInstance().seekToProgress(currentMessageObject, progress);
    }

    private void updateWaveform() {
        if (currentMessageObject == null || documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO) {
            return;
        }
        for (int a = 0; a < documentAttach.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                if (attribute.waveform == null || attribute.waveform.length == 0) {
                    MediaController.getInstance().generateWaveform(currentMessageObject);
                }
                useSeekBarWaweform = attribute.waveform != null;
                seekBarWaveform.setWaveform(attribute.waveform);
                break;
            }
        }
    }

    private int createDocumentLayout(int maxWidth, MessageObject messageObject) {
        if (messageObject.type == MessageObject.MO_TYPE0_TEXT) {
            documentAttach = messageObject.messageOwner.media.webpage.document;
        } else {
            documentAttach = messageObject.messageOwner.media.document;
        }
        if (documentAttach == null) {
            return 0;
        }
        if (MessageObject.isVoiceDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_AUDIO;
            int duration = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    duration = attribute.duration;
                    break;
                }
            }
            availableTimeWidth = maxWidth - dp(76 + 18) - (int) Math.ceil(audioTimePaint.measureText("00:00"));
            measureTime(messageObject);
            int minSize = dp(40 + 14 + 20 + 90 + 10) + timeWidth;
            backgroundWidth = Math.min(maxWidth, minSize + duration * dp(10));

            if (messageObject.isOutOwner()) {
                seekBarWaveform.setColors(Theme.MSG_OUT_VOICE_SEEKBAR_COLOR, Theme.MSG_OUT_VOICE_SEEKBAR_FILL_COLOR, Theme.MSG_OUT_VOICE_SEEKBAR_SELECTED_COLOR);
                seekBar.setColors(Theme.MSG_OUT_AUDIO_SEEKBAR_COLOR, Theme.MSG_OUT_AUDIO_SEEKBAR_FILL_COLOR, Theme.MSG_OUT_AUDIO_SEEKBAR_SELECTED_COLOR);
            } else {
                seekBarWaveform.setColors(Theme.MSG_IN_VOICE_SEEKBAR_COLOR, Theme.MSG_IN_VOICE_SEEKBAR_FILL_COLOR, Theme.MSG_IN_VOICE_SEEKBAR_SELECTED_COLOR);
                seekBar.setColors(Theme.MSG_IN_AUDIO_SEEKBAR_COLOR, Theme.MSG_IN_AUDIO_SEEKBAR_FILL_COLOR, Theme.MSG_IN_AUDIO_SEEKBAR_SELECTED_COLOR);
            }
            seekBarWaveform.setMessageObject(messageObject);
            return 0;
        } else if (MessageObject.isMusicDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_MUSIC;
            if (messageObject.isOutOwner()) {
                seekBar.setColors(Theme.MSG_OUT_AUDIO_SEEKBAR_COLOR, Theme.MSG_OUT_AUDIO_SEEKBAR_FILL_COLOR, Theme.MSG_OUT_AUDIO_SEEKBAR_SELECTED_COLOR);
            } else {
                seekBar.setColors(Theme.MSG_IN_AUDIO_SEEKBAR_COLOR, Theme.MSG_IN_AUDIO_SEEKBAR_FILL_COLOR, Theme.MSG_IN_AUDIO_SEEKBAR_SELECTED_COLOR);
            }

            maxWidth = maxWidth - dp(86);

            CharSequence stringFinal = TextUtils.ellipsize(messageObject.getMusicTitle().replace('\n', ' '), audioTitlePaint, maxWidth - dp(12), TextUtils.TruncateAt.END);
            songLayout = new StaticLayout(stringFinal, audioTitlePaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (songLayout.getLineCount() > 0) {
                songX = -(int) Math.ceil(songLayout.getLineLeft(0));
            }

            stringFinal = TextUtils.ellipsize(messageObject.getMusicAuthor().replace('\n', ' '), audioPerformerPaint, maxWidth, TextUtils.TruncateAt.END);
            performerLayout = new StaticLayout(stringFinal, audioPerformerPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (performerLayout.getLineCount() > 0) {
                performerX = -(int) Math.ceil(performerLayout.getLineLeft(0));
            }

            int duration = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    duration = attribute.duration;
                    break;
                }
            }
            int durationWidth = (int) Math.ceil(audioTimePaint.measureText(String.format("%d:%02d / %d:%02d", duration / 60, duration % 60, duration / 60, duration % 60)));
            availableTimeWidth = backgroundWidth - dp(76 + 18) - durationWidth;
            return durationWidth;
        } else if (MessageObject.isVideoDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_VIDEO;
            int duration = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    duration = attribute.duration;
                    break;
                }
            }
            int minutes = duration / 60;
            int seconds = duration - minutes * 60;
            String str = String.format("%d:%02d, %s", minutes, seconds, formatFileSize(documentAttach.size));
            infoWidth = (int) Math.ceil(infoPaint.measureText(str));
            infoLayout = new StaticLayout(str, infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            return 0;
        } else {
            drawPhotoImage = documentAttach.mime_type != null && documentAttach.mime_type.toLowerCase().startsWith("image/") || documentAttach.thumb instanceof TLRPC.TL_photoSize && !(documentAttach.thumb.location instanceof TLRPC.TL_fileLocationUnavailable);
            if (!drawPhotoImage) {
                maxWidth += dp(30);
            }
            documentAttachType = DOCUMENT_ATTACH_TYPE_DOCUMENT;
            String name = FileLoader.getDocumentFileName(documentAttach);
            if (name == null || name.length() == 0) {
                name = LocaleController.getString("AttachDocument", R.string.AttachDocument);
            }
            docTitleLayout = StaticLayoutEx.createStaticLayout(name, docNamePaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.MIDDLE, maxWidth, drawPhotoImage ? 2 : 1);
            docTitleOffsetX = Integer.MIN_VALUE;
            int width;
            if (docTitleLayout != null && docTitleLayout.getLineCount() > 0) {
                int maxLineWidth = 0;
                for (int a = 0; a < docTitleLayout.getLineCount(); a++) {
                    maxLineWidth = Math.max(maxLineWidth, (int) Math.ceil(docTitleLayout.getLineWidth(a)));
                    docTitleOffsetX = Math.max(docTitleOffsetX, (int) Math.ceil(-docTitleLayout.getLineLeft(a)));
                }
                width = Math.min(maxWidth, maxLineWidth);
            } else {
                width = maxWidth;
                docTitleOffsetX = 0;
            }

            String str = formatFileSize(documentAttach.size) + " " + FileLoader.getDocumentExtension(documentAttach);
            infoWidth = Math.min(maxWidth - AndroidUtilities.dp(30), (int) Math.ceil(infoPaint.measureText(str)));
            CharSequence str2 = TextUtils.ellipsize(str, infoPaint, infoWidth, TextUtils.TruncateAt.END);
            try {
                if (infoWidth < 0) {
                    infoWidth = dp(10);
                }
                infoLayout = new StaticLayout(str2, infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }

            if (drawPhotoImage) {
                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, getPhotoSize());
                photoImage.setNeedsQualityThumb(true);
                photoImage.setShouldGenerateQualityThumb(true);
                photoImage.setParentMessageObject(messageObject);
                if (currentPhotoObject != null) {
                    currentPhotoFilter = "86_86_b";
                    photoImage.setImage(null, null, null, null, currentPhotoObject.location, currentPhotoFilter, 0, null, true);
                } else {
                    photoImage.setImageBitmap((BitmapDrawable) null);
                }
            }
            return width;
        }
    }

    private void calcBackgroundWidth(int maxWidth, int timeMore, int maxChildWidth) {
        if (maxWidth - currentMessageObject.lastLineWidth < timeMore) {
            totalHeight += dp(14);
            backgroundWidth = Math.max(maxChildWidth, currentMessageObject.lastLineWidth) + dp(31);
            backgroundWidth = Math.max(backgroundWidth, timeWidth + dp(31));
        } else {
            int diff = maxChildWidth - currentMessageObject.lastLineWidth;
            if (diff >= 0 && diff <= timeMore) {
                backgroundWidth = maxChildWidth + timeMore - diff + dp(31);
            } else {
                backgroundWidth = Math.max(maxChildWidth, currentMessageObject.lastLineWidth + timeMore) + dp(31);
            }
        }
    }

    public void setHighlightedText(String text) {
        if (currentMessageObject.messageOwner.message == null || currentMessageObject == null || currentMessageObject.type != MessageObject.MO_TYPE0_TEXT || TextUtils.isEmpty(currentMessageObject.messageText) || text == null) {
            if (!urlPathSelection.isEmpty()) {
                linkSelectionBlockNum = -1;
                resetUrlPaths(true);
                invalidate();
            }
            return;
        }
        int start = TextUtils.indexOf(currentMessageObject.messageOwner.message.toLowerCase(), text.toLowerCase());
        if (start == -1) {
            if (!urlPathSelection.isEmpty()) {
                linkSelectionBlockNum = -1;
                resetUrlPaths(true);
                invalidate();
            }
            return;
        }
        int end = start + text.length();
        for (int c = 0; c < currentMessageObject.textLayoutBlocks.size(); c++) {
            MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(c);
            if (start >= block.charactersOffset && start < block.charactersOffset + block.textLayout.getText().length()) {
                linkSelectionBlockNum = c;
                resetUrlPaths(true);
                try {
                    LinkPath path = obtainNewUrlPath(true);
                    int length = block.textLayout.getText().length();
                    path.setCurrentLayout(block.textLayout, start, 0);
                    block.textLayout.getSelectionPath(start, end - block.charactersOffset, path);
                    if (end >= block.charactersOffset + length) {
                        for (int a = c + 1; a < currentMessageObject.textLayoutBlocks.size(); a++) {
                            MessageObject.TextLayoutBlock nextBlock = currentMessageObject.textLayoutBlocks.get(a);
                            length = nextBlock.textLayout.getText().length();
                            path = obtainNewUrlPath(true);
                            path.setCurrentLayout(nextBlock.textLayout, 0, nextBlock.height);
                            nextBlock.textLayout.getSelectionPath(0, end - nextBlock.charactersOffset, path);
                            if (end < block.charactersOffset + length - 1) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                invalidate();
                break;
            }
        }
    }

    public void setMessageObject(MessageObject messageObject) {
        boolean messageChanged = currentMessageObject != messageObject || messageObject.forceUpdate;
        boolean dataChanged = currentMessageObject == messageObject && (isUserDataChanged() || photoNotSet);
        if (messageChanged || dataChanged || isPhotoDataChanged(messageObject)) {
            currentMessageObject = messageObject;
            lastSendState = messageObject.messageOwner.send_state;
            lastViewsCount = messageObject.messageOwner.views;
            isPressed = false;
            isCheckPressed = true;
            isAvatarVisible = false;
            wasLayout = false;
            drawShareButton = checkNeedDrawShareButton(messageObject);
            replyNameLayout = null;
            replyTextLayout = null;
            replyNameWidth = 0;
            replyTextWidth = 0;
            viaNameWidth = 0;
            currentReplyPhoto = null;
            currentUser = null;
            currentChat = null;
            drawNameLayout = false;

            resetPressedLink(-1);
            messageObject.forceUpdate = false;
            drawPhotoImage = false;
            linkPreviewPressed = false;
            buttonPressed = 0;
            mediaOffsetY = 0;
            documentAttachType = DOCUMENT_ATTACH_TYPE_NONE;
            documentAttach = null;
            titleLayout = null;
            captionLayout = null;
            docTitleLayout = null;
            drawImageButton = false;
            currentPhotoObject = null;
            currentPhotoObjectThumb = null;
            currentPhotoFilter = null;
            infoLayout = null;
            cancelLoading = false;
            buttonState = -1;
            currentUrl = null;
            photoNotSet = false;
            drawBackground = true;
            drawName = false;
            useSeekBarWaweform = false;
            drawForwardedName = false;
            mediaBackground = false;
            availableTimeWidth = 0;
            photoImage.setNeedsQualityThumb(false);
            photoImage.setShouldGenerateQualityThumb(false);
            photoImage.setParentMessageObject(null);
            photoImage.setRoundRadius(dp(3));

            if (messageChanged) {
                firstVisibleBlockNum = 0;
                lastVisibleBlockNum = 0;
                needNewVisiblePart = true;
            }

            if (messageObject.type == MessageObject.MO_TYPE0_TEXT) {
                drawForwardedName = true;

                int maxWidth;
                if (isTablet()) {
                    if (isChat && !messageObject.isOutOwner() && messageObject.isFromUser()) {
                        maxWidth = getMinTabletSide() - dp(122);
                        drawName = true;
                    } else {
                        drawName = messageObject.messageOwner.to_id.channel_id != 0 && !messageObject.isOutOwner();
                        maxWidth = getMinTabletSide() - dp(80);
                    }
                } else {
                    if (isChat && !messageObject.isOutOwner() && messageObject.isFromUser()) {
                        maxWidth = Math.min(displaySize.x, displaySize.y) - dp(122);
                        drawName = true;
                    } else {
                        maxWidth = Math.min(displaySize.x, displaySize.y) - dp(80);
                        drawName = messageObject.messageOwner.to_id.channel_id != 0 && !messageObject.isOutOwner();
                    }
                }
                measureTime(messageObject);
                int timeMore = timeWidth + dp(6);
                if (messageObject.isOutOwner()) {
                    timeMore += dp(20.5f);
                }

                //hasLinkPreview = messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage && messageObject.messageOwner.media.webpage instanceof TLRPC.TL_webPage;
                backgroundWidth = maxWidth;
                if (hasLinkPreview || maxWidth - messageObject.lastLineWidth < timeMore) {
                    backgroundWidth = Math.max(backgroundWidth, messageObject.lastLineWidth) + dp(31);
                    backgroundWidth = Math.max(backgroundWidth, timeWidth + dp(31));
                } else {
                    int diff = backgroundWidth - messageObject.lastLineWidth;
                    if (diff >= 0 && diff <= timeMore) {
                        backgroundWidth = backgroundWidth + timeMore - diff + dp(31);
                    } else {
                        backgroundWidth = Math.max(backgroundWidth, messageObject.lastLineWidth + timeMore) + dp(31);
                    }
                }
                availableTimeWidth = backgroundWidth - dp(31);

                setMessageObjectInternal(messageObject);

                backgroundWidth = messageObject.textWidth;
                totalHeight = messageObject.textHeight + dp(19.5f) + namesOffset;

                int maxChildWidth = Math.max(backgroundWidth, nameWidth);
                maxChildWidth = Math.max(maxChildWidth, forwardedNameWidth);
                maxChildWidth = Math.max(maxChildWidth, replyNameWidth);
                maxChildWidth = Math.max(maxChildWidth, replyTextWidth);

                photoImage.setImageBitmap((Drawable) null);
                calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
            } else if (messageObject.type == MessageObject.MO_TYPE2_VOICE) {
                drawForwardedName = true;
                if (isTablet()) {
                    backgroundWidth = Math.min(getMinTabletSide() - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                } else {
                    backgroundWidth = Math.min(displaySize.x - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                }
                createDocumentLayout(backgroundWidth, messageObject);

                setMessageObjectInternal(messageObject);

                totalHeight = dp(70) + namesOffset;
            } else if (messageObject.type == MessageObject.MO_TYPE14_MUSIC) {
                if (isTablet()) {
                    backgroundWidth = Math.min(getMinTabletSide() - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                } else {
                    backgroundWidth = Math.min(displaySize.x - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                }

                createDocumentLayout(backgroundWidth, messageObject);

                setMessageObjectInternal(messageObject);

                totalHeight = dp(82) + namesOffset;
            } else {
                drawForwardedName = messageObject.messageOwner.fwd_from != null && messageObject.type != MessageObject.MO_TYPE13_STICKER;
                mediaBackground = messageObject.type != MessageObject.MO_TYPE9_FILE;
                drawImageButton = false; // we do not want the image button for images
                drawPhotoImage = true;

                int photoWidth = 0;
                int photoHeight = 0;
                int additionHeight = 0;

                if (messageObject.audioProgress != 2 && !MediaController.getInstance().canAutoplayGifs() && messageObject.type == MessageObject.MO_TYPE8_GIF) {
                    messageObject.audioProgress = 1;
                }

                photoImage.setAllowStartAnimation(messageObject.audioProgress == 0);

                photoImage.setForcePreview(false);
                if (messageObject.type == MessageObject.MO_TYPE9_FILE) {
                    if (isTablet()) {
                        backgroundWidth = Math.min(getMinTabletSide() - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                    } else {
                        backgroundWidth = Math.min(displaySize.x - dp(isChat && messageObject.isFromUser() && !messageObject.isOutOwner() ? 102 : 50), dp(270));
                    }
                    if (checkNeedDrawShareButton(messageObject)) {
                        backgroundWidth -= dp(20);
                    }
                    int maxWidth = backgroundWidth - dp(86 + 52);
                    createDocumentLayout(maxWidth, messageObject);
                    if (!TextUtils.isEmpty(messageObject.caption)) {
                        maxWidth += AndroidUtilities.dp(86);
                    }
                    if (drawPhotoImage) {
                        photoWidth = dp(86);
                        photoHeight = dp(86);
                    } else {
                        photoWidth = dp(56);
                        photoHeight = dp(56);
                        maxWidth += AndroidUtilities.dp(TextUtils.isEmpty(messageObject.caption) ? 51 : 21);
                    }
                    availableTimeWidth = maxWidth;
                    if (!drawPhotoImage) {
                        if (TextUtils.isEmpty(messageObject.caption) && infoLayout.getLineCount() > 0) {
                            measureTime(messageObject);
                            int timeLeft = backgroundWidth - AndroidUtilities.dp(40 + 18 + 56 + 8) - (int) Math.ceil(infoLayout.getLineWidth(0));
                            if (timeLeft < timeWidth) {
                                photoHeight += AndroidUtilities.dp(8);
                            }
                        }
                    }
                } else if (messageObject.type == MessageObject.MO_TYPE13_STICKER) { //webp
                    drawBackground = false;
                    for (int a = 0; a < messageObject.messageOwner.media.document.attributes.size(); a++) {
                        TLRPC.DocumentAttribute attribute = messageObject.messageOwner.media.document.attributes.get(a);
                        if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                            photoWidth = attribute.w;
                            photoHeight = attribute.h;
                            break;
                        }
                    }
                    float maxHeight;
                    float maxWidth;
                    if (isTablet()) {
                        maxHeight = maxWidth = getMinTabletSide() * 0.4f;
                    } else {
                        maxHeight = maxWidth = Math.min(displaySize.x, displaySize.y) * 0.5f;
                    }
                    if (photoWidth == 0) {
                        photoHeight = (int) maxHeight;
                        photoWidth = photoHeight + dp(100);
                    }
                    photoHeight *= maxWidth / photoWidth;
                    photoWidth = (int) maxWidth;
                    if (photoHeight > maxHeight) {
                        photoWidth *= maxHeight / photoHeight;
                        photoHeight = (int) maxHeight;
                    }
                    documentAttachType = DOCUMENT_ATTACH_TYPE_STICKER;
                    availableTimeWidth = photoWidth - dp(14);
                    backgroundWidth = photoWidth + dp(12);
                    currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 80);
                    if (messageObject.attachPathExists) {
                        photoImage.setImage(null, messageObject.messageOwner.attachPath,
                                String.format(Locale.US, "%d_%d", photoWidth, photoHeight),
                                null,
                                currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null,
                                "b1",
                                messageObject.messageOwner.media.document.size, "webp", true);
                    } else if (messageObject.messageOwner.media.document.id != 0) {
                        photoImage.setImage(messageObject.messageOwner.media.document, null,
                                String.format(Locale.US, "%d_%d", photoWidth, photoHeight),
                                null,
                                currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null,
                                "b1",
                                messageObject.messageOwner.media.document.size, "webp", true);
                    }
                } else {
                    int maxPhotoWidth;
                    if (isTablet()) {
                        maxPhotoWidth = photoWidth = (int) (getMinTabletSide() * 0.7f);
                    } else {
                        maxPhotoWidth = photoWidth = (int) (Math.min(displaySize.x, displaySize.y) * 0.7f);
                    }
                    photoHeight = photoWidth + dp(100);
                    if (checkNeedDrawShareButton(messageObject)) {
                        maxPhotoWidth -= dp(20);
                        photoWidth -= dp(20);
                    }

                    if (photoWidth > getPhotoSize()) {
                        photoWidth = getPhotoSize();
                    }
                    if (photoHeight > getPhotoSize()) {
                        photoHeight = getPhotoSize();
                    }

                    if (messageObject.type == MessageObject.MO_TYPE1_PHOTO) {
                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 80);
                    } else if (messageObject.type == MessageObject.MO_TYPE3_VIDEO) {
                        createDocumentLayout(0, messageObject);
                        photoImage.setNeedsQualityThumb(true);
                        photoImage.setShouldGenerateQualityThumb(true);
                        photoImage.setParentMessageObject(messageObject);
                    } else if (messageObject.type == MessageObject.MO_TYPE8_GIF) {
                        String str = formatFileSize(messageObject.messageOwner.media.document.size);
                        infoWidth = (int) Math.ceil(infoPaint.measureText(str));
                        infoLayout = new StaticLayout(str, infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                        photoImage.setNeedsQualityThumb(true);
                        photoImage.setShouldGenerateQualityThumb(true);
                        photoImage.setParentMessageObject(messageObject);
                    }

                    if (messageObject.caption != null) {
                        mediaBackground = false;
                    }

                    currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, getPhotoSize());

                    int w = 0;
                    int h = 0;

                    if (currentPhotoObject != null && currentPhotoObject == currentPhotoObjectThumb) {
                        currentPhotoObjectThumb = null;
                    }

                    if (currentPhotoObject != null) {
                        float scale = (float) currentPhotoObject.w / (float) photoWidth;
                        w = (int) (currentPhotoObject.w / scale);
                        h = (int) (currentPhotoObject.h / scale);
                        if (w == 0) {
                            w = dp(150);
                        }
                        if (h == 0) {
                            h = dp(150);
                        }
                        if (h > photoHeight) {
                            float scale2 = h;
                            h = photoHeight;
                            scale2 /= h;
                            w = (int) (w / scale2);
                        } else if (h < dp(120)) {
                            h = dp(120);
                            float hScale = (float) currentPhotoObject.h / h;
                            if (currentPhotoObject.w / hScale < photoWidth) {
                                w = (int) (currentPhotoObject.w / hScale);
                            }
                        }
                    }

                    if ((w == 0 || h == 0) && messageObject.type == MessageObject.MO_TYPE8_GIF) {
                        for (int a = 0; a < messageObject.messageOwner.media.document.attributes.size(); a++) {
                            TLRPC.DocumentAttribute attribute = messageObject.messageOwner.media.document.attributes.get(a);
                            if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                float scale = (float) attribute.w / (float) photoWidth;
                                w = (int) (attribute.w / scale);
                                h = (int) (attribute.h / scale);
                                if (h > photoHeight) {
                                    float scale2 = h;
                                    h = photoHeight;
                                    scale2 /= h;
                                    w = (int) (w / scale2);
                                } else if (h < dp(120)) {
                                    h = dp(120);
                                    float hScale = (float) attribute.h / h;
                                    if (attribute.w / hScale < photoWidth) {
                                        w = (int) (attribute.w / hScale);
                                    }
                                }
                                break;
                            }
                        }
                    }


                    if (w == 0 || h == 0) {
                        w = h = dp(150);
                    }
                    if (messageObject.type == MessageObject.MO_TYPE3_VIDEO) {
                        if (w < infoWidth + dp(16 + 24)) {
                            w = infoWidth + dp(16 + 24);
                        }
                    }

                    availableTimeWidth = maxPhotoWidth - dp(14);
                    measureTime(messageObject);
                    int timeWidthTotal = timeWidth + dp(14 + (messageObject.isOutOwner() ? 20 : 0));
                    if (w < timeWidthTotal) {
                        w = timeWidthTotal;
                    }

                    photoWidth = w;
                    photoHeight = h;
                    backgroundWidth = w + dp(12);
                    if (!mediaBackground) {
                        backgroundWidth += dp(9);
                    }
                    if (messageObject.caption != null) {
                        try {
                            captionLayout = new StaticLayout(messageObject.caption, MessageObject.getTextPaint(), photoWidth - dp(10), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            if (captionLayout.getLineCount() > 0) {
                                captionHeight = captionLayout.getHeight();
                                additionHeight += captionHeight + dp(9);
                                float lastLineWidth = captionLayout.getLineWidth(captionLayout.getLineCount() - 1) + captionLayout.getLineLeft(captionLayout.getLineCount() - 1);
                                if (photoWidth - dp(8) - lastLineWidth < timeWidthTotal) {
                                    additionHeight += dp(14);
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e("messenger", e);
                        }
                    }

                    currentPhotoFilter = String.format(Locale.US, "%d_%d", (int) (w / density), (int) (h / density));
                    if (messageObject.photoThumbs != null && messageObject.photoThumbs.size() > 1 || messageObject.type == MessageObject.MO_TYPE3_VIDEO || messageObject.type == MessageObject.MO_TYPE8_GIF) {
                        currentPhotoFilter += "_b";
                    }

                    boolean noSize = false;
                    if (messageObject.type == MessageObject.MO_TYPE3_VIDEO || messageObject.type == MessageObject.MO_TYPE8_GIF) {
                        noSize = true;
                    }
                    if (currentPhotoObject != null && !noSize && currentPhotoObject.size == 0) {
                        currentPhotoObject.size = -1;
                    }

                    if (messageObject.type == MessageObject.MO_TYPE1_PHOTO) {
                        if (currentPhotoObject != null) {
                            boolean photoExist = true;
                            //String fileName = FileLoader.getAttachFileName(currentPhotoObject);
                            if (messageObject.mediaExists) {
                                //MediaController.getInstance().removeLoadingFileObserver(this);
                            } else {
                                photoExist = false;
                            }
                            if (photoExist || MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_PHOTO) /*|| FileLoader.getInstance().isLoadingFile(fileName)*/) {
                                photoImage.setImage(currentPhotoObject.location, currentPhotoFilter, currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null, currentPhotoFilter, noSize ? 0 : currentPhotoObject.size, null, false);
                            } else {
                                photoNotSet = true;
                                if (currentPhotoObjectThumb != null) {
                                    photoImage.setImage(null, null, currentPhotoObjectThumb.location, currentPhotoFilter, 0, null, false);
                                } else {
                                    photoImage.setImageBitmap((Drawable) null);
                                }
                            }
                        } else {
                            photoImage.setImageBitmap((BitmapDrawable) null);
                        }
                    } else if (messageObject.type == MessageObject.MO_TYPE8_GIF) {
                        String fileName = FileLoader.getAttachFileName(messageObject.messageOwner.media.document);
                        int localFile = 0;
                        if (messageObject.attachPathExists) {
                            //MediaController.getInstance().removeLoadingFileObserver(this);
                            localFile = 1;
                        } else if (messageObject.mediaExists) {
                            localFile = 2;
                        }
                        if (!messageObject.isSending() && (localFile != 0 || MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_GIF) && MessageObject.isNewGifDocument(messageObject.messageOwner.media.document) || FileLoader.getInstance().isLoadingFile(fileName))) {
                            if (localFile == 1) {
                                photoImage.setImage(null, messageObject.isSendError() ? null : messageObject.messageOwner.attachPath, null, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, 0, null, false);
                            } else {
                                photoImage.setImage(messageObject.messageOwner.media.document, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, messageObject.messageOwner.media.document.size, null, false);
                            }
                        } else {
                            photoNotSet = true;
                            photoImage.setImage(null, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, 0, null, false);
                        }
                    } else {
                        photoImage.setImage(null, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, 0, null, false);
                    }
                }
                setMessageObjectInternal(messageObject);

                if (drawForwardedName) {
                    namesOffset += dp(5);
                } else if (drawNameLayout && messageObject.messageOwner.reply_to_msg_id == 0) {
                    namesOffset += dp(7);
                }

                invalidate();

                photoImage.setImageCoords(0, dp(7) + namesOffset, photoWidth, photoHeight);
                totalHeight = photoHeight + dp(14) + namesOffset + additionHeight;
            }
            if (captionLayout == null && messageObject.caption != null && messageObject.type != MessageObject.MO_TYPE13_STICKER) {
                try {
                    int width = backgroundWidth - AndroidUtilities.dp(31);
                    captionLayout = new StaticLayout(messageObject.caption, MessageObject.getTextPaint(), width - dp(10), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    if (captionLayout.getLineCount() > 0) {
                        int timeWidthTotal = timeWidth + (messageObject.isOutOwner() ? dp(20) : 0);
                        captionHeight = captionLayout.getHeight();
                        totalHeight += captionHeight + dp(9);
                        float lastLineWidth = captionLayout.getLineWidth(captionLayout.getLineCount() - 1) + captionLayout.getLineLeft(captionLayout.getLineCount() - 1);
                        if (width - dp(8) - lastLineWidth < timeWidthTotal) {
                            totalHeight += dp(14);
                            captionHeight += dp(14);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
            }

            {
                substractBackgroundHeight = 0;
                keyboardHeight = 0;
            }
        }
        updateWaveform();
        updateButtonState(dataChanged);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), totalHeight + keyboardHeight);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentMessageObject == null) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        if (changed || !wasLayout) {
            layoutWidth = getMeasuredWidth();
            layoutHeight = getMeasuredHeight() - substractBackgroundHeight;
            if (timeTextWidth < 0) {
                timeTextWidth = dp(10);
            }
            timeLayout = new StaticLayout(currentTimeString, timePaint, timeTextWidth + dp(6), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (!mediaBackground) {
                if (!currentMessageObject.isOutOwner()) {
                    timeX = backgroundWidth - dp(9) - timeWidth + (isChat && currentMessageObject.isFromUser() ? dp(48) : 0);
                } else {
                    timeX = layoutWidth - timeWidth - dp(38.5f);
                }
            } else {
                if (!currentMessageObject.isOutOwner()) {
                    timeX = backgroundWidth - dp(4) - timeWidth + (isChat && currentMessageObject.isFromUser() ? dp(48) : 0);
                } else {
                    timeX = layoutWidth - timeWidth - dp(42.0f);
                }
            }

            if (isAvatarVisible) {
                avatarImage.setImageCoords(dp(6), layoutHeight - dp(44), dp(42), dp(42));
            }

            wasLayout = true;
        }

        if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT) {
            textY = dp(10) + namesOffset;
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            if (currentMessageObject.isOutOwner()) {
                seekBarX = layoutWidth - backgroundWidth + dp(57);
                buttonX = layoutWidth - backgroundWidth + dp(14);
                timeAudioX = layoutWidth - backgroundWidth + dp(67);
            } else {
                if (isChat && currentMessageObject.isFromUser()) {
                    seekBarX = dp(114);
                    buttonX = dp(71);
                    timeAudioX = dp(124);
                } else {
                    seekBarX = dp(66);
                    buttonX = dp(23);
                    timeAudioX = dp(76);
                }
            }
            seekBarWaveform.setSize(backgroundWidth - dp(92 + (hasLinkPreview ? 10 : 0)), dp(30));
            seekBar.setSize(backgroundWidth - dp(72 + (hasLinkPreview ? 10 : 0)), dp(30));
            seekBarY = dp(13) + namesOffset + mediaOffsetY;
            buttonY = dp(13) + namesOffset + mediaOffsetY;

            iconX = buttonX;
            iconY = buttonY;
            //radialProgress.setProgressRect(buttonX, buttonY, buttonX + dp(44), buttonY + dp(44));

            updateAudioProgress();
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.isOutOwner()) {
                seekBarX = layoutWidth - backgroundWidth + dp(56);
                buttonX = layoutWidth - backgroundWidth + dp(14);
                timeAudioX = layoutWidth - backgroundWidth + dp(67);
            } else {
                if (isChat && currentMessageObject.isFromUser()) {
                    seekBarX = dp(113);
                    buttonX = dp(71);
                    timeAudioX = dp(124);
                } else {
                    seekBarX = dp(65);
                    buttonX = dp(23);
                    timeAudioX = dp(76);
                }
            }
            seekBar.setSize(backgroundWidth - dp(65 + (hasLinkPreview ? 10 : 0)), dp(30));
            seekBarY = dp(29) + namesOffset + mediaOffsetY;
            buttonY = dp(13) + namesOffset + mediaOffsetY;

            iconX = buttonX;
            iconY = buttonY;
            //radialProgress.setProgressRect(buttonX, buttonY, buttonX + dp(44), buttonY + dp(44));

            updateAudioProgress();
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT && !drawPhotoImage) {
            if (currentMessageObject.isOutOwner()) {
                buttonX = layoutWidth - backgroundWidth + dp(14);
            } else {
                if (isChat && currentMessageObject.isFromUser()) {
                    buttonX = dp(71);
                } else {
                    buttonX = dp(23);
                }
            }
            buttonY = dp(13) + namesOffset + mediaOffsetY;

            iconX = buttonX;
            iconY = buttonY;
            //radialProgress.setProgressRect(buttonX, buttonY, buttonX + dp(44), buttonY + dp(44));

            photoImage.setImageCoords(buttonX - dp(10), buttonY - dp(10), photoImage.getImageWidth(), photoImage.getImageHeight());
        } else {
            int x;
            if (currentMessageObject.isOutOwner()) {
                if (mediaBackground) {
                    x = layoutWidth - backgroundWidth - dp(3);
                } else {
                    x = layoutWidth - backgroundWidth + dp(6);
                }
            } else {
                if (isChat && currentMessageObject.isFromUser()) {
                    x = dp(63);
                } else {
                    x = dp(15);
                }
            }
            photoImage.setImageCoords(x, photoImage.getImageY(), photoImage.getImageWidth(), photoImage.getImageHeight());
            buttonX = (int) (x + (photoImage.getImageWidth() - dp(48)) / 2.0f);
            buttonY = (int) (dp(7) + (photoImage.getImageHeight() - dp(48)) / 2.0f) + namesOffset;

            iconX = buttonX;
            iconY = buttonY;
            //radialProgress.setProgressRect(buttonX, buttonY, buttonX + dp(48), buttonY + dp(48));

            deleteProgressRect.set(buttonX + dp(3), buttonY + dp(3), buttonX + dp(45), buttonY + dp(45));
        }
    }

    private void drawContent(Canvas canvas) {

        if (needNewVisiblePart && currentMessageObject.type == MessageObject.MO_TYPE0_TEXT) {
            getLocalVisibleRect(scrollRect);
            setVisiblePart(scrollRect.top, scrollRect.bottom - scrollRect.top);
            needNewVisiblePart = false;
        }

        photoImage.setPressed(isDrawSelectedBackground());
        photoImage.setVisible(!PhotoViewer.getInstance().isShowingImage(currentMessageObject), false);
        //radialProgress.setHideCurrentDrawable(false);
        //radialProgress.setProgressColor(Theme.MSG_MEDIA_PROGRESS_COLOR);

        boolean imageDrawn = false;
        boolean drawIcon = false;
        if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT && currentMessageObject.textLayoutBlocks != null && !currentMessageObject.textLayoutBlocks.isEmpty()) {
            if (currentMessageObject.isOutOwner()) {
                textX = currentBackgroundDrawable.getBounds().left + dp(11);
            } else {
                textX = currentBackgroundDrawable.getBounds().left + dp(17);
            }

            textY = dp(10) + namesOffset;

            if (firstVisibleBlockNum >= 0) {
                for (int a = firstVisibleBlockNum; a <= lastVisibleBlockNum; a++) {
                    if (a >= currentMessageObject.textLayoutBlocks.size()) {
                        break;
                    }
                    MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(a);
                    canvas.save();
                    canvas.translate(textX - (int) Math.ceil(block.textXOffset), textY + block.textYOffset);
                    if (pressedLink != null && a == linkBlockNum) {
                        for (int b = 0; b < urlPath.size(); b++) {
                            canvas.drawPath(urlPath.get(b), urlPaint);
                        }
                    }
                    if (a == linkSelectionBlockNum && !urlPathSelection.isEmpty()) {
                        for (int b = 0; b < urlPathSelection.size(); b++) {
                            canvas.drawPath(urlPathSelection.get(b), urlSelectionPaint);
                        }
                    }
                    try {
                        block.textLayout.draw(canvas);
                    } catch (Exception e) {
                        FileLog.e("messenger", e);
                    }
                    canvas.restore();
                }
            }
            drawTime = true;
        } else if (drawPhotoImage) {
            imageDrawn = photoImage.draw(canvas);
            drawTime = photoImage.getVisible();
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || currentMessageObject.type == MessageObject.MO_TYPE8_GIF) {
            if (photoImage.getVisible()) {
                setDrawableBounds(Theme.docMenuDrawable[3], otherX = photoImage.getImageX() + photoImage.getImageWidth() - dp(14), otherY = photoImage.getImageY() + dp(8.1f));
                Theme.docMenuDrawable[3].draw(canvas);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            drawIcon = true;
            if (currentMessageObject.isOutOwner()) {
                audioTitlePaint.setColor(Theme.MSG_OUT_AUDIO_TITLE_TEXT_COLOR);
                audioPerformerPaint.setColor(Theme.MSG_OUT_AUDIO_PERFORMER_TEXT_COLOR);
                audioTimePaint.setColor(Theme.MSG_OUT_AUDIO_DURATION_TEXT_COLOR);
                //radialProgress.setProgressColor(isDrawSelectedBackground() || buttonPressed != 0 ? Theme.MSG_OUT_AUDIO_SELECTED_PROGRESS_COLOR : Theme.MSG_OUT_AUDIO_PROGRESS_COLOR);
            } else {
                audioTitlePaint.setColor(Theme.MSG_IN_AUDIO_TITLE_TEXT_COLOR);
                audioPerformerPaint.setColor(Theme.MSG_IN_AUDIO_PERFORMER_TEXT_COLOR);
                audioTimePaint.setColor(Theme.MSG_IN_AUDIO_DURATION_TEXT_COLOR);
                //radialProgress.setProgressColor(isDrawSelectedBackground() || buttonPressed != 0 ? Theme.MSG_IN_AUDIO_SELECTED_PROGRESS_COLOR : Theme.MSG_IN_AUDIO_PROGRESS_COLOR);
            }
            //radialProgress.draw(canvas);

            canvas.save();
            canvas.translate(timeAudioX + songX, dp(13) + namesOffset + mediaOffsetY);
            songLayout.draw(canvas);
            canvas.restore();

            canvas.save();
            if (MediaController.getInstance().isPlayingAudio(currentMessageObject)) {
                canvas.translate(seekBarX, seekBarY);
                seekBar.draw(canvas);
            } else {
                canvas.translate(timeAudioX + performerX, dp(35) + namesOffset + mediaOffsetY);
                performerLayout.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(timeAudioX, dp(57) + namesOffset + mediaOffsetY);
            durationLayout.draw(canvas);
            canvas.restore();

            Drawable menuDrawable;
            if (currentMessageObject.isOutOwner()) {
                menuDrawable = Theme.docMenuDrawable[1];
            } else {
                menuDrawable = Theme.docMenuDrawable[isDrawSelectedBackground() ? 2 : 0];
            }
            setDrawableBounds(menuDrawable, otherX = buttonX + backgroundWidth - dp(currentMessageObject.type == MessageObject.MO_TYPE0_TEXT ? 58 : 48), otherY = buttonY - dp(5));
            menuDrawable.draw(canvas);
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            drawIcon = true;
            if (currentMessageObject.isOutOwner()) {
                audioTimePaint.setColor(isDrawSelectedBackground() ? Theme.MSG_OUT_AUDIO_DURATION_SELECTED_TEXT_COLOR : Theme.MSG_OUT_AUDIO_DURATION_TEXT_COLOR);
                //radialProgress.setProgressColor(isDrawSelectedBackground() || buttonPressed != 0 ? Theme.MSG_OUT_AUDIO_SELECTED_PROGRESS_COLOR : Theme.MSG_OUT_AUDIO_PROGRESS_COLOR);
            } else {
                audioTimePaint.setColor(isDrawSelectedBackground() ? Theme.MSG_IN_AUDIO_DURATION_SELECTED_TEXT_COLOR : Theme.MSG_IN_AUDIO_DURATION_TEXT_COLOR);
                //radialProgress.setProgressColor(isDrawSelectedBackground() || buttonPressed != 0 ? Theme.MSG_IN_AUDIO_SELECTED_PROGRESS_COLOR : Theme.MSG_IN_AUDIO_PROGRESS_COLOR);
            }
            //radialProgress.draw(canvas);

            canvas.save();
            if (useSeekBarWaweform) {
                canvas.translate(seekBarX + dp(13), seekBarY);
                seekBarWaveform.draw(canvas);
            } else {
                canvas.translate(seekBarX, seekBarY);
                seekBar.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(timeAudioX, dp(44) + namesOffset + mediaOffsetY);
            durationLayout.draw(canvas);
            canvas.restore();

            if (currentMessageObject.type != MessageObject.MO_TYPE0_TEXT && currentMessageObject.messageOwner.to_id.channel_id == 0 && currentMessageObject.isContentUnread()) {
                docBackPaint.setColor(currentMessageObject.isOutOwner() ? Theme.MSG_OUT_VOICE_SEEKBAR_FILL_COLOR : Theme.MSG_IN_VOICE_SEEKBAR_FILL_COLOR);
                canvas.drawCircle(timeAudioX + timeWidthAudio + dp(6), dp(51) + namesOffset + mediaOffsetY, dp(3), docBackPaint);
            }
        }

        if (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
            if (photoImage.getVisible()) {
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                    setDrawableBounds(Theme.docMenuDrawable[3], otherX = photoImage.getImageX() + photoImage.getImageWidth() - dp(14), otherY = photoImage.getImageY() + dp(8.1f));
                    Theme.docMenuDrawable[3].draw(canvas);
                }

                if (infoLayout != null && (buttonState == 1 || buttonState == 0 || buttonState == 3)) {
                    infoPaint.setColor(Theme.MSG_MEDIA_INFO_TEXT_COLOR);
                    setDrawableBounds(Theme.timeBackgroundDrawable, photoImage.getImageX() + dp(4), photoImage.getImageY() + dp(4), infoWidth + dp(8), dp(16.5f));
                    Theme.timeBackgroundDrawable.draw(canvas);

                    canvas.save();
                    canvas.translate(photoImage.getImageX() + dp(8), photoImage.getImageY() + dp(5.5f));
                    infoLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }

        if (captionLayout != null) {
            canvas.save();
            if (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || currentMessageObject.type == MessageObject.MO_TYPE8_GIF) {
                canvas.translate(captionX = photoImage.getImageX() + dp(5), captionY = photoImage.getImageY() + photoImage.getImageHeight() + dp(6));
            } else {
                canvas.translate(captionX = currentBackgroundDrawable.getBounds().left + dp(currentMessageObject.isOutOwner() ? 11 : 17), captionY = totalHeight - captionHeight - dp(10));
            }
            if (pressedLink != null) {
                for (int b = 0; b < urlPath.size(); b++) {
                    canvas.drawPath(urlPath.get(b), urlPaint);
                }
            }
            try {
                captionLayout.draw(canvas);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            canvas.restore();
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            drawIcon = true;
            docNamePaint.setColor(Theme.MSG_DOC_NAME_COLOR);
            infoPaint.setColor(Theme.MSG_DOC_NAME_COLOR);
            if (currentMessageObject.isOutOwner()) {
                docBackPaint.setColor(isDrawSelectedBackground() ? Theme.MSG_OUT_FILE_BACKGROUND_SELECTED_COLOR : Theme.MSG_OUT_FILE_BACKGROUND_COLOR);
            } else {
                docBackPaint.setColor(isDrawSelectedBackground() ? Theme.MSG_IN_FILE_BACKGROUND_SELECTED_COLOR : Theme.MSG_IN_FILE_BACKGROUND_COLOR);
            }

            int x;
            int titleY;
            int subtitleY;
            if (drawPhotoImage) {
                x = photoImage.getImageX() + photoImage.getImageWidth() + dp(10);
                titleY = photoImage.getImageY() + dp(8);
                subtitleY = photoImage.getImageY() + docTitleLayout.getLineBottom(docTitleLayout.getLineCount() - 1) + dp(13);
                /* we do not use the photo image states
                if (buttonState >= 0 && buttonState < 4) {
                    if (!imageDrawn) {
                        int image = buttonState;
                        if (buttonState == 0) {
                            image = currentMessageObject.isOutOwner() ? 7 : 10;
                        } else if (buttonState == 1) {
                            image = currentMessageObject.isOutOwner() ? 8 : 11;
                        }
                        radialProgress.swapBackground(Theme.photoStatesDrawables[image][isDrawSelectedBackground() || buttonPressed != 0 ? 1 : 0]);
                    } else {
                        radialProgress.swapBackground(Theme.photoStatesDrawables[buttonState][buttonPressed]);
                    }
                }
                */

                if (!imageDrawn) {
                    rect.set(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX() + photoImage.getImageWidth(), photoImage.getImageY() + photoImage.getImageHeight());
                    canvas.drawRoundRect(rect, dp(3), dp(3), docBackPaint);
                } else {
                    if (buttonState == -1) {
                        //radialProgress.setHideCurrentDrawable(true);
                    }
                }
            } else {
                x = buttonX + dp(53);
                titleY = buttonY + dp(4);
                subtitleY = buttonY + dp(27);
            }

            try {
                if (docTitleLayout != null) {
                    canvas.save();
                    canvas.translate(x + docTitleOffsetX, titleY);
                    docTitleLayout.draw(canvas);
                    canvas.restore();
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }

            try {
                if (infoLayout != null) {
                    canvas.save();
                    canvas.translate(x, subtitleY);
                    infoLayout.draw(canvas);
                    canvas.restore();
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        //if (drawImageButton && photoImage.getVisible()) {
            //radialProgress.draw(canvas);
        //}

        if( drawIcon ) {
            setDrawableBounds(Theme.fileDrawable, iconX, iconY, dp(44), dp(44));
            Theme.fileDrawable.draw(canvas);
        }
    }

    /*private Drawable getDrawableForCurrentState() {
        if( documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT )
        {
            return Theme.fileDrawable;
        }
        else if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (buttonState == -1) {
                return null;
            }
            radialProgress.setAlphaForPrevious(false);
            return Theme.fileStatesDrawable[currentMessageObject.isOutOwner() ? buttonState : buttonState + 5][isDrawSelectedBackground() || buttonPressed != 0 ? 1 : 0];
        } else {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT && !drawPhotoImage) {
                radialProgress.setAlphaForPrevious(false);
                if (buttonState == -1) {
                    return Theme.fileStatesDrawable[currentMessageObject.isOutOwner() ? 3 : 8][isDrawSelectedBackground() ? 1 : 0];
                } else if (buttonState == 0) {
                    return Theme.fileStatesDrawable[currentMessageObject.isOutOwner() ? 2 : 7][isDrawSelectedBackground() ? 1 : 0];
                } else if (buttonState == 1) {
                    return Theme.fileStatesDrawable[currentMessageObject.isOutOwner() ? 4 : 9][isDrawSelectedBackground() ? 1 : 0];
                }
            } else {
                radialProgress.setAlphaForPrevious(true);
                if (buttonState >= 0 && buttonState < 4) {
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        int image = buttonState;
                        if (buttonState == 0) {
                            image = currentMessageObject.isOutOwner() ? 7 : 10;
                        } else if (buttonState == 1) {
                            image = currentMessageObject.isOutOwner() ? 8 : 11;
                        }
                        return Theme.photoStatesDrawables[image][isDrawSelectedBackground() || buttonPressed != 0 ? 1 : 0];
                    } else {
                        return Theme.photoStatesDrawables[buttonState][buttonPressed];
                    }
                } else if (buttonState == -1 && documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                    return Theme.photoStatesDrawables[currentMessageObject.isOutOwner() ? 9 : 12][isDrawSelectedBackground() ? 1 : 0];
                }
                return null;
            }
        }
        return null;
    }*/

    private int getMaxNameWidth() {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER) {
            int maxWidth;
            if (isTablet()) {
                if (isChat && !currentMessageObject.isOutOwner() && currentMessageObject.isFromUser()) {
                    maxWidth = getMinTabletSide() - dp(42);
                } else {
                    maxWidth = getMinTabletSide();
                }
            } else {
                if (isChat && !currentMessageObject.isOutOwner() && currentMessageObject.isFromUser()) {
                    maxWidth = Math.min(displaySize.x, displaySize.y) - dp(42);
                } else {
                    maxWidth = Math.min(displaySize.x, displaySize.y);
                }
            }
            return maxWidth - backgroundWidth - dp(57);
        }
        return backgroundWidth - dp(mediaBackground ? 22 : 31);
    }

    public void updateButtonState(boolean animated) {
        String fileName = null;
        boolean fileExists = false;
        if (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO) {
            if (currentPhotoObject == null) {
                return;
            }
            fileName = FileLoader.getAttachFileName(currentPhotoObject);
            fileExists = currentMessageObject.mediaExists;
        } else if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || currentMessageObject.type == MessageObject.MO_TYPE9_FILE || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.attachPathExists) {
                fileName = currentMessageObject.messageOwner.attachPath;
                fileExists = true;
            } else if (!currentMessageObject.isSendError() || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                fileName = currentMessageObject.getFileName();
                fileExists = currentMessageObject.mediaExists;
            }
        } else if (documentAttachType != DOCUMENT_ATTACH_TYPE_NONE) {
            fileName = FileLoader.getAttachFileName(documentAttach);
            fileExists = currentMessageObject.mediaExists;
        } else if (currentPhotoObject != null) {
            fileName = FileLoader.getAttachFileName(currentPhotoObject);
            fileExists = currentMessageObject.mediaExists;
        }
        if (fileName == null || fileName.length() == 0) {
            //radialProgress.setBackground(null, false, false);
            return;
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            /*if ((currentMessageObject.isOut() && currentMessageObject.isSending()) || (currentMessageObject.isSendError() && fromBot)) {
                MediaController.getInstance().addLoadingFileObserver(currentMessageObject.messageOwner.attachPath, currentMessageObject, this);
                buttonState = 4;
                radialProgress.setBackground(getDrawableForCurrentState(), !fromBot, animated);

                Float progress = ImageLoader.getInstance().getFileProgress(currentMessageObject.messageOwner.attachPath);
                if (progress == null && SendMessagesHelper.getInstance().isSendingMessage(currentMessageObject.getId())) {
                    progress = 1.0f;
                }
                radialProgress.setProgress(progress != null ? progress : 0, false);
            } else*/ {
                if (fileExists) {
                    //MediaController.getInstance().removeLoadingFileObserver(this);
                    boolean playing = MediaController.getInstance().isPlayingAudio(currentMessageObject);
                    if (!playing || playing && MediaController.getInstance().isAudioPaused()) {
                        buttonState = 0;
                    } else {
                        buttonState = 1;
                    }
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
                } else {
                    //MediaController.getInstance().addLoadingFileObserver(fileName, currentMessageObject, this);
                    if (!FileLoader.getInstance().isLoadingFile(fileName)) {
                        buttonState = 2;
                        //radialProgress.setProgress(0, animated);
                        //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
                    } else {
                        buttonState = 4;
                        //Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                        //if (progress != null) {
                        //    radialProgress.setProgress(progress, animated);
                        //} else {
                        //    radialProgress.setProgress(0, animated);
                        //}
                        //radialProgress.setBackground(getDrawableForCurrentState(), true, animated);
                    }
                }
            }
            updateAudioProgress();
        } else if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT && documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT && documentAttachType != DOCUMENT_ATTACH_TYPE_VIDEO) {
            if (currentPhotoObject == null || !drawImageButton) {
                return;
            }
            if (!fileExists) {
                //MediaController.getInstance().addLoadingFileObserver(fileName, currentMessageObject, this);
                float setProgress = 0;
                boolean progressVisible = false;
                if (!FileLoader.getInstance().isLoadingFile(fileName)) {
                    if (!cancelLoading &&
                            (documentAttachType == 0 && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_PHOTO) ||
                                    documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_GIF))) {
                        progressVisible = true;
                        buttonState = 1;
                    } else {
                        buttonState = 0;
                    }
                } else {
                    progressVisible = true;
                    buttonState = 1;
                    Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                    setProgress = progress != null ? progress : 0;
                }
                //radialProgress.setProgress(setProgress, false);
                //radialProgress.setBackground(getDrawableForCurrentState(), progressVisible, animated);
                invalidate();
            } else {
                //MediaController.getInstance().removeLoadingFileObserver(this);
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && !photoImage.isAllowStartAnimation()) {
                    buttonState = 2;
                } else {
                    buttonState = -1;
                }
                //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
                invalidate();
            }
        } else {
            /*if (currentMessageObject.isOut() && currentMessageObject.isSending()) {
                if (currentMessageObject.messageOwner.attachPath != null && currentMessageObject.messageOwner.attachPath.length() > 0) {
                    MediaController.getInstance().addLoadingFileObserver(currentMessageObject.messageOwner.attachPath, currentMessageObject, this);
                    boolean needProgress = currentMessageObject.messageOwner.attachPath == null || !currentMessageObject.messageOwner.attachPath.startsWith("http");
                    HashMap<String, String> params = currentMessageObject.messageOwner.params;
                    if (currentMessageObject.messageOwner.message != null && params != null && (params.containsKey("url") || params.containsKey("bot"))) {
                        needProgress = false;
                        buttonState = -1;
                    } else {
                        buttonState = 1;
                    }
                    radialProgress.setBackground(getDrawableForCurrentState(), needProgress, animated);
                    if (needProgress) {
                        Float progress = ImageLoader.getInstance().getFileProgress(currentMessageObject.messageOwner.attachPath);
                        if (progress == null && SendMessagesHelper.getInstance().isSendingMessage(currentMessageObject.getId())) {
                            progress = 1.0f;
                        }
                        radialProgress.setProgress(progress != null ? progress : 0, false);
                    } else {
                        radialProgress.setProgress(0, false);
                    }
                    invalidate();
                }
            } else*/ {
                /*if (currentMessageObject.messageOwner.attachPath != null && currentMessageObject.messageOwner.attachPath.length() != 0) {
                    MediaController.getInstance().removeLoadingFileObserver(this);
                }*/
                if (!fileExists) {
                    //MediaController.getInstance().addLoadingFileObserver(fileName, currentMessageObject, this);
                    //float setProgress = 0;
                    //boolean progressVisible = false;
                    if (!FileLoader.getInstance().isLoadingFile(fileName)) {
                        if (!cancelLoading &&
                                (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_PHOTO) ||
                                        currentMessageObject.type == MessageObject.MO_TYPE8_GIF && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_GIF) && MessageObject.isNewGifDocument(currentMessageObject.messageOwner.media.document)) ) {
                            //progressVisible = true;
                            buttonState = 1;
                        } else {
                            buttonState = 0;
                        }
                    } else {
                        //progressVisible = true;
                        buttonState = 1;
                        //Float progress = ImageLoader.getInstance().getFileProgress(fileName);
                        //setProgress = progress != null ? progress : 0;
                    }
                    //radialProgress.setBackground(getDrawableForCurrentState(), progressVisible, animated);
                    //radialProgress.setProgress(setProgress, false);
                    invalidate();
                } else {
                    //MediaController.getInstance().removeLoadingFileObserver(this);
                    if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF && !photoImage.isAllowStartAnimation()) {
                        buttonState = 2;
                    } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                        buttonState = 3;
                    } else {
                        buttonState = -1;
                    }
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
                    if (photoNotSet) {
                        setMessageObject(currentMessageObject);
                    }
                    invalidate();
                }
            }
        }
    }

    private void didPressedButton(boolean animated) {
        if (buttonState == 0) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (delegate.needPlayAudio(currentMessageObject)) {
                    buttonState = 1;
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, false);
                    invalidate();
                }
            } else {
                cancelLoading = false;
                //radialProgress.setProgress(0, false);
                if (currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO) {
                    photoImage.setImage(currentPhotoObject.location, currentPhotoFilter, currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null, currentPhotoFilter, currentPhotoObject.size, null, false);
                } else if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF) {
                    currentMessageObject.audioProgress = 2;
                    photoImage.setImage(currentMessageObject.messageOwner.media.document, null, currentPhotoObject != null ? currentPhotoObject.location : null, currentPhotoFilter, currentMessageObject.messageOwner.media.document.size, null, false);
                } else if (currentMessageObject.type == MessageObject.MO_TYPE9_FILE) {
                    FileLoader.getInstance().loadFile(currentMessageObject.messageOwner.media.document, false, false);
                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                    FileLoader.getInstance().loadFile(documentAttach, true, false);
                } else if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT && documentAttachType != DOCUMENT_ATTACH_TYPE_NONE) {
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                        photoImage.setImage(currentMessageObject.messageOwner.media.webpage.document, null, currentPhotoObject.location, currentPhotoFilter, currentMessageObject.messageOwner.media.webpage.document.size, null, false);
                        currentMessageObject.audioProgress = 2;
                    } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        FileLoader.getInstance().loadFile(currentMessageObject.messageOwner.media.webpage.document, false, false);
                    }
                } else {
                    photoImage.setImage(currentPhotoObject.location, currentPhotoFilter, currentPhotoObjectThumb != null ? currentPhotoObjectThumb.location : null, currentPhotoFilterThumb, 0, null, false);
                }
                buttonState = 1;
                //radialProgress.setBackground(getDrawableForCurrentState(), true, animated);
                invalidate();
            }
        } else if (buttonState == 1) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                boolean result = MediaController.getInstance().pauseAudio(currentMessageObject);
                if (result) {
                    buttonState = 0;
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, false);
                    invalidate();
                }
            } else {
                if (currentMessageObject.isOut() && currentMessageObject.isSending()) {
                    delegate.didPressedCancelSendButton(this);
                } else {
                    cancelLoading = true;
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        FileLoader.getInstance().cancelLoadFile(documentAttach);
                    } else if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT || currentMessageObject.type == MessageObject.MO_TYPE1_PHOTO || currentMessageObject.type == MessageObject.MO_TYPE8_GIF) {
                        photoImage.cancelLoadImage();
                    } else if (currentMessageObject.type == MessageObject.MO_TYPE9_FILE) {
                        FileLoader.getInstance().cancelLoadFile(currentMessageObject.messageOwner.media.document);
                    }
                    buttonState = 0;
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
                    invalidate();
                }
            }
        } else if (buttonState == 2) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                //radialProgress.setProgress(0, false);
                FileLoader.getInstance().loadFile(documentAttach, true, false);
                buttonState = 4;
                //radialProgress.setBackground(getDrawableForCurrentState(), true, false);
                invalidate();
            } else {
                photoImage.setAllowStartAnimation(true);
                photoImage.startAnimation();
                currentMessageObject.audioProgress = 0;
                buttonState = -1;
                //radialProgress.setBackground(getDrawableForCurrentState(), false, animated);
            }
        } else if (buttonState == 3) {
            delegate.didPressedImage(this);
        } else if (buttonState == 4) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (currentMessageObject.isOut() && currentMessageObject.isSending() || currentMessageObject.isSendError()) {
                    if (delegate != null) {
                        delegate.didPressedCancelSendButton(this);
                    }
                } else {
                    FileLoader.getInstance().cancelLoadFile(documentAttach);
                    buttonState = 2;
                    //radialProgress.setBackground(getDrawableForCurrentState(), false, false);
                    invalidate();
                }
            }
        }
    }

    /*@Override
    public void onFailedDownload(String fileName) {
        updateButtonState(documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC);
    }

    @Override
    public void onSuccessDownload(String fileName) {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            updateButtonState(true);
            updateWaveform();
        } else {
            //radialProgress.setProgress(1, true);
            if (currentMessageObject.type == MessageObject.MO_TYPE0_TEXT) {
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && currentMessageObject.audioProgress != 1) {
                    buttonState = 2;
                    didPressedButton(true);
                } else if (!photoNotSet) {
                    updateButtonState(true);
                } else {
                    setMessageObject(currentMessageObject);
                }
            } else {
                if (!photoNotSet || currentMessageObject.type == MessageObject.MO_TYPE8_GIF && currentMessageObject.audioProgress != 1) {
                    if (currentMessageObject.type == MessageObject.MO_TYPE8_GIF && currentMessageObject.audioProgress != 1) {
                        photoNotSet = false;
                        buttonState = 2;
                        didPressedButton(true);
                    } else {
                        updateButtonState(true);
                    }
                }
                if (photoNotSet) {
                    setMessageObject(currentMessageObject);
                }
            }
        }
    }*/

    @Override
    public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb) {
        if (currentMessageObject != null && set && !thumb && !currentMessageObject.mediaExists && !currentMessageObject.attachPathExists) {
            currentMessageObject.mediaExists = true;
            updateButtonState(true);
        }
    }

    /*@Override
    public void onProgressDownload(String fileName, float progress) {
        //radialProgress.setProgress(progress, true);
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (buttonState != 4) {
                updateButtonState(false);
            }
        } else {
            if (buttonState != 1) {
                updateButtonState(false);
            }
        }
    }

    @Override
    public void onProgressUpload(String fileName, float progress, boolean isEncrypted) {
        //radialProgress.setProgress(progress, true);
    }*/

    @Override
    public void onProvideStructure(ViewStructure structure) {
        super.onProvideStructure(structure);
        if (allowAssistant && Build.VERSION.SDK_INT >= 23) {
            if (currentMessageObject.messageText != null && currentMessageObject.messageText.length() > 0) {
                structure.setText(currentMessageObject.messageText);
            } else if (currentMessageObject.caption != null && currentMessageObject.caption.length() > 0) {
                structure.setText(currentMessageObject.caption);
            }
        }
    }

    public void setDelegate(ChatMessageCellDelegate chatMessageCellDelegate) {
        delegate = chatMessageCellDelegate;
    }

    public void setAllowAssistant(boolean value) {
        allowAssistant = value;
    }

    private void measureTime(MessageObject messageObject) {
        boolean hasSign = !messageObject.isOutOwner() && messageObject.messageOwner.from_id > 0 && messageObject.messageOwner.post;
        TLRPC.User signUser = MessagesController.getInstance().getUser(messageObject.messageOwner.from_id);
        if (hasSign && signUser == null) {
            hasSign = false;
        }
        String timeString = LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000);
        if (hasSign) {
            currentTimeString = ", " + timeString;
        } else {
            currentTimeString = timeString;
        }
        timeTextWidth = timeWidth = (int) Math.ceil(timePaint.measureText(currentTimeString));

        if (hasSign) {
            if (availableTimeWidth == 0) {
                availableTimeWidth = dp(1000);
            }
            CharSequence name = ContactsController.formatName(signUser.first_name, signUser.last_name).replace('\n', ' ');
            int widthForSign = availableTimeWidth - timeWidth;
            int width = (int) Math.ceil(timePaint.measureText(name, 0, name.length()));
            if (width > widthForSign) {
                name = TextUtils.ellipsize(name, timePaint, widthForSign, TextUtils.TruncateAt.END);
                width = widthForSign;
            }
            currentTimeString = name + currentTimeString;
            timeTextWidth += width;
            timeWidth += width;
        }
    }

    private boolean isDrawSelectedBackground() {
        return isPressed() && isCheckPressed || !isCheckPressed && isPressed || isHighlighted;
    }

    private boolean checkNeedDrawShareButton(MessageObject messageObject) {
        // we use the "Share" button as a reply button: in the "deaddrop" group it is very useful to reply
        // to a message, so we use this as a shortcut to "long press -> reply"
        if( messageObject.getDialogId()== MrChat.MR_CHAT_ID_DEADDROP) {
            return true;
        }
        return false;
    }

    private void setMessageObjectInternal(MessageObject messageObject) {

        if (currentMessageObject.isFromUser()) {
            currentUser = MessagesController.getInstance().getUser(currentMessageObject.messageOwner.from_id);
        } else if (currentMessageObject.messageOwner.from_id < 0) {
            currentChat = MessagesController.getInstance().getChat(-currentMessageObject.messageOwner.from_id);
        } else if (currentMessageObject.messageOwner.post) {
            currentChat = MessagesController.getInstance().getChat(currentMessageObject.messageOwner.to_id.channel_id);
        }

        MrContact mrContact = null;
        String cname = "";
        if(currentUser!=null && isChat) {
            mrContact = MrMailbox.getContact(currentUser.id);
            cname = mrContact.getDisplayName();
        }

        if (isChat && !messageObject.isOutOwner() && messageObject.isFromUser()) {
            isAvatarVisible = true;
            ContactsController.setupAvatar(this, avatarImage, avatarDrawable, mrContact, null);
        }

        measureTime(messageObject);

        namesOffset = 0;

        CharSequence viaString = null;

        boolean authorName = drawName && isChat && !currentMessageObject.isOutOwner();
        if (authorName) {
            drawNameLayout = true;
            nameWidth = getMaxNameWidth();
            if (nameWidth < 0) {
                nameWidth = dp(100);
            }

            if (authorName) {
                if (currentUser != null) {
                    currentNameString = cname;
                } else if (currentChat != null) {
                    currentNameString = currentChat.title;
                } else {
                    currentNameString = "DELETED";
                }
            } else {
                currentNameString = "";
            }
            CharSequence nameStringFinal = TextUtils.ellipsize(currentNameString.replace('\n', ' '), namePaint, nameWidth, TextUtils.TruncateAt.END);
            try {
                nameLayout = new StaticLayout(nameStringFinal, namePaint, nameWidth + dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (nameLayout != null && nameLayout.getLineCount() > 0) {
                    nameWidth = (int) Math.ceil(nameLayout.getLineWidth(0));
                    if (messageObject.type != MessageObject.MO_TYPE13_STICKER) {
                        namesOffset += dp(19);
                    }
                    nameOffsetX = nameLayout.getLineLeft(0);
                } else {
                    nameWidth = 0;
                }
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
            if (currentNameString.length() == 0) {
                currentNameString = null;
            }
        } else {
            currentNameString = null;
            nameLayout = null;
            nameWidth = 0;
        }

        currentForwardNameString = null;
        forwardedNameLayout[0] = null;
        forwardedNameLayout[1] = null;
        forwardedNameWidth = 0;
        if( drawForwardedName && messageObject.isForwarded() && messageObject.messageOwner.fwd_from!=null )
        {
            currentForwardNameString = messageObject.messageOwner.fwd_from.m_name;

            forwardedNameWidth = getMaxNameWidth();
            int fromWidth = (int) Math.ceil(forwardNamePaint.measureText(LocaleController.getString("From", R.string.From) + " "));
            CharSequence name = TextUtils.ellipsize(currentForwardNameString.replace('\n', ' '), replyNamePaint, forwardedNameWidth - fromWidth - viaWidth, TextUtils.TruncateAt.END);
            CharSequence lastLine;
            if (viaString != null) {
                viaNameWidth = (int) Math.ceil(forwardNamePaint.measureText(LocaleController.getString("From", R.string.From) + " " + name));
                lastLine = replaceTags(String.format("%s <b>%s</b> via <b>%s</b>", LocaleController.getString("From", R.string.From), name, null));
            } else {
                lastLine = replaceTags(String.format("%s <b>%s</b>", LocaleController.getString("From", R.string.From), name));
            }
            lastLine = TextUtils.ellipsize(lastLine, forwardNamePaint, forwardedNameWidth, TextUtils.TruncateAt.END);
            try {
                forwardedNameLayout[1] = new StaticLayout(lastLine, forwardNamePaint, forwardedNameWidth + dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                lastLine = TextUtils.ellipsize(replaceTags(LocaleController.getString("ForwardedMessage", R.string.ForwardedMessage)), forwardNamePaint, forwardedNameWidth, TextUtils.TruncateAt.END);
                forwardedNameLayout[0] = new StaticLayout(lastLine, forwardNamePaint, forwardedNameWidth + dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                forwardedNameWidth = Math.max((int) Math.ceil(forwardedNameLayout[0].getLineWidth(0)), (int) Math.ceil(forwardedNameLayout[1].getLineWidth(0)));
                forwardNameOffsetX[0] = forwardedNameLayout[0].getLineLeft(0);
                forwardNameOffsetX[1] = forwardedNameLayout[1].getLineLeft(0);
                namesOffset += dp(36);
            } catch (Exception e) {
                FileLog.e("messenger", e);
            }
        }

        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentMessageObject == null) {
            return;
        }

        if (!wasLayout) {
            requestLayout();
            return;
        }

        if (isAvatarVisible) {
            avatarImage.draw(canvas);
        }

        if (mediaBackground) {
            timePaint.setColor(Theme.MSG_MEDIA_TIME_TEXT_COLOR);
        } else {
            if (currentMessageObject.isOutOwner()) {
                timePaint.setColor(Theme.MSG_OUT_FWD_N_TIME_TEXT_COLOR);
            } else {
                timePaint.setColor(Theme.MSG_IN_FWD_N_TIME_TEXT_COLOR);
            }
        }

        if (currentMessageObject.isOutOwner()) {
            if (isDrawSelectedBackground()) {
                //if (!mediaBackground) {
                    currentBackgroundDrawable = Theme.backgroundDrawableOutSelected;
                //} else {
                //    currentBackgroundDrawable = Theme.backgroundMediaDrawableOutSelected;
                //}
            } else {
                //if (!mediaBackground) {
                    currentBackgroundDrawable = Theme.backgroundDrawableOut;
                //} else {
                //    currentBackgroundDrawable = Theme.backgroundMediaDrawableOut;
                //}
            }
            setDrawableBounds(currentBackgroundDrawable,
                    layoutWidth - backgroundWidth - (!mediaBackground ? 0 : dp(9)),
                    dp(1),
                    backgroundWidth - (mediaBackground ? -dp(6) : dp(3)),
                    layoutHeight - dp(2));
        } else {
            if (isDrawSelectedBackground()) {
                //if (!mediaBackground) {
                    currentBackgroundDrawable = Theme.backgroundDrawableInSelected;
                //} else {
                //    currentBackgroundDrawable = Theme.backgroundMediaDrawableInSelected;
                //}
            } else {
                //if (!mediaBackground) {
                    currentBackgroundDrawable = Theme.backgroundDrawableIn;
                //} else {
                //    currentBackgroundDrawable = Theme.backgroundMediaDrawableIn;
                //}
            }
            if (isChat && currentMessageObject.isFromUser()) {
                setDrawableBounds(currentBackgroundDrawable,
                        dp(48 + (!mediaBackground ? 3 : 3)),
                        dp(1),
                        backgroundWidth - (mediaBackground ? -dp(6) : dp(3)),
                        layoutHeight - dp(2));
            } else {
                setDrawableBounds(currentBackgroundDrawable,
                        (!mediaBackground ? dp(3) : dp(3)),
                        dp(1),
                        backgroundWidth - (mediaBackground ? -dp(6) : dp(3)),
                        layoutHeight - dp(2));
            }
        }
        if (drawBackground && currentBackgroundDrawable != null) {
            currentBackgroundDrawable.draw(canvas);
        }

        drawContent(canvas);

        if (drawShareButton) {
            shareStartX = currentBackgroundDrawable.getBounds().right - dp(34);
            shareStartY = dp(4);
            setDrawableBounds(Theme.shareIconDrawable, shareStartX, shareStartY);
            Theme.shareIconDrawable.draw(canvas);
        }

        if (drawNameLayout && nameLayout != null) {
            canvas.save();

            if (currentMessageObject.type == MessageObject.MO_TYPE13_STICKER) {
                namePaint.setColor(Theme.MSG_STICKER_NAME_TEXT_COLOR);
                if (currentMessageObject.isOutOwner()) {
                    nameX = dp(28);
                } else {
                    nameX = currentBackgroundDrawable.getBounds().right + dp(22);
                }
                nameY = layoutHeight - dp(38);
                Theme.systemDrawable.setColorFilter(Theme.colorFilter);
                Theme.systemDrawable.setBounds((int) nameX - dp(12), (int) nameY - dp(5), (int) nameX + dp(12) + nameWidth, (int) nameY + dp(22));
                Theme.systemDrawable.draw(canvas);
            } else {
                if (mediaBackground || currentMessageObject.isOutOwner()) {
                    nameX = currentBackgroundDrawable.getBounds().left + dp(11) - nameOffsetX;
                } else {
                    nameX = currentBackgroundDrawable.getBounds().left + dp(17) - nameOffsetX;
                }
                namePaint.setColor(AvatarDrawable.getNameColor(currentNameString));
                nameY = dp(10);
            }
            canvas.translate(nameX, nameY);
            nameLayout.draw(canvas);
            canvas.restore();
        }

        if (drawForwardedName && forwardedNameLayout[0] != null && forwardedNameLayout[1] != null) {
            forwardNameY = dp(10 + (drawNameLayout ? 19 : 0));
            if (currentMessageObject.isOutOwner()) {
                forwardNamePaint.setColor(Theme.MSG_OUT_FWD_N_TIME_TEXT_COLOR);
                forwardNameX = currentBackgroundDrawable.getBounds().left + dp(11);
            } else {
                forwardNamePaint.setColor(Theme.MSG_IN_FWD_N_TIME_TEXT_COLOR);
                if (mediaBackground) {
                    forwardNameX = currentBackgroundDrawable.getBounds().left + dp(11);
                } else {
                    forwardNameX = currentBackgroundDrawable.getBounds().left + dp(17);
                }
            }
            for (int a = 0; a < 2; a++) {
                canvas.save();
                canvas.translate(forwardNameX - forwardNameOffsetX[a], forwardNameY + dp(16) * a);
                forwardedNameLayout[a].draw(canvas);
                canvas.restore();
            }
        }

        if (drawTime || !mediaBackground) {
            if (mediaBackground) {
                Drawable drawable;
                if (currentMessageObject.type == MessageObject.MO_TYPE13_STICKER) {
                    drawable = Theme.timeStickerBackgroundDrawable;
                } else {
                    drawable = Theme.timeBackgroundDrawable;
                }
                setDrawableBounds(drawable, timeX - dp(4), layoutHeight - dp(27), timeWidth + dp(8 + (currentMessageObject.isOutOwner() ? 20 : 0)), dp(17));
                drawable.draw(canvas);

                int additionalX = 0;

                canvas.save();
                canvas.translate(timeX + additionalX, layoutHeight - dp(11.3f) - timeLayout.getHeight());
                timeLayout.draw(canvas);
                canvas.restore();
            } else {
                int additionalX = 0;

                canvas.save();
                canvas.translate(timeX + additionalX, layoutHeight - dp(6.5f) - timeLayout.getHeight());
                timeLayout.draw(canvas);
                canvas.restore();
                //canvas.drawRect(timeX, layoutHeight - AndroidUtilities.dp(6.5f) - timeLayout.getHeight(), timeX + availableTimeWidth, layoutHeight - AndroidUtilities.dp(4.5f) - timeLayout.getHeight(), timePaint);
            }

            if (currentMessageObject.isOutOwner()) {
                boolean drawCheck1 = false;
                boolean drawCheck2 = false;
                boolean drawClock = false;
                boolean drawError = false;

                if (currentMessageObject.isSending()) {
                    drawCheck1 = false;
                    drawCheck2 = false;
                    drawClock = true;
                    drawError = false;
                } else if (currentMessageObject.isSendError()) {
                    drawCheck1 = false;
                    drawCheck2 = false;
                    drawClock = false;
                    drawError = true;
                } else if (currentMessageObject.isSent()) {
                    if (!currentMessageObject.isUnread()) {
                        drawCheck1 = true;
                        drawCheck2 = true;
                    } else {
                        drawCheck1 = false;
                        drawCheck2 = true;
                    }
                    drawClock = false;
                    drawError = false;
                }

                if (drawClock) {
                    if (!mediaBackground) {
                        setDrawableBounds(Theme.clockDrawable, layoutWidth - dp(18.5f) - Theme.clockDrawable.getIntrinsicWidth(), layoutHeight - dp(8.5f) - Theme.clockDrawable.getIntrinsicHeight());
                        Theme.clockDrawable.draw(canvas);
                    } else {
                        setDrawableBounds(Theme.clockMediaDrawable, layoutWidth - dp(22.0f) - Theme.clockMediaDrawable.getIntrinsicWidth(), layoutHeight - dp(12.5f) - Theme.clockMediaDrawable.getIntrinsicHeight());
                        Theme.clockMediaDrawable.draw(canvas);
                    }
                }

                {
                    if (drawCheck2) {
                        if (!mediaBackground) {
                            if (drawCheck1) {
                                setDrawableBounds(Theme.checkDrawable, layoutWidth - dp(22.5f) - Theme.checkDrawable.getIntrinsicWidth(), layoutHeight - dp(8.0f) - Theme.checkDrawable.getIntrinsicHeight());
                            } else {
                                setDrawableBounds(Theme.checkDrawable, layoutWidth - dp(18.5f) - Theme.checkDrawable.getIntrinsicWidth(), layoutHeight - dp(8.0f) - Theme.checkDrawable.getIntrinsicHeight());
                            }
                            Theme.checkDrawable.draw(canvas);
                        } else {
                            if (drawCheck1) {
                                setDrawableBounds(Theme.checkMediaDrawable, layoutWidth - dp(26.3f) - Theme.checkMediaDrawable.getIntrinsicWidth(), layoutHeight - dp(12.5f) - Theme.checkMediaDrawable.getIntrinsicHeight());
                            } else {
                                setDrawableBounds(Theme.checkMediaDrawable, layoutWidth - dp(21.5f) - Theme.checkMediaDrawable.getIntrinsicWidth(), layoutHeight - dp(12.5f) - Theme.checkMediaDrawable.getIntrinsicHeight());
                            }
                            Theme.checkMediaDrawable.draw(canvas);
                        }
                    }
                    if (drawCheck1) {
                        if (!mediaBackground) {
                            setDrawableBounds(Theme.halfCheckDrawable, layoutWidth - dp(18) - Theme.halfCheckDrawable.getIntrinsicWidth(), layoutHeight - dp(8.0f) - Theme.halfCheckDrawable.getIntrinsicHeight());
                            Theme.halfCheckDrawable.draw(canvas);
                        } else {
                            setDrawableBounds(Theme.halfCheckMediaDrawable, layoutWidth - dp(21.5f) - Theme.halfCheckMediaDrawable.getIntrinsicWidth(), layoutHeight - dp(12.5f) - Theme.halfCheckMediaDrawable.getIntrinsicHeight());
                            Theme.halfCheckMediaDrawable.draw(canvas);
                        }
                    }
                }
                if (drawError) {
                    if (!mediaBackground) {
                        setDrawableBounds(Theme.errorDrawable, layoutWidth - dp(18) - Theme.errorDrawable.getIntrinsicWidth(), layoutHeight - dp(7) - Theme.errorDrawable.getIntrinsicHeight());
                        Theme.errorDrawable.draw(canvas);
                    } else {
                        setDrawableBounds(Theme.errorDrawable, layoutWidth - dp(20.5f) - Theme.errorDrawable.getIntrinsicWidth(), layoutHeight - dp(11.5f) - Theme.errorDrawable.getIntrinsicHeight());
                        Theme.errorDrawable.draw(canvas);
                    }
                }
            }
        }
    }

    /*@Override
    public int getObserverTag() {
        return TAG;
    }*/

    public MessageObject getMessageObject() {
        return currentMessageObject;
    }
}
