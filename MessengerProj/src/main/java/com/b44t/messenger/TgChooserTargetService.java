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


package com.b44t.messenger;

// ChooserTargetService, see https://developer.android.com/reference/android/service/chooser/ChooserTargetService.html


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import com.b44t.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@TargetApi(Build.VERSION_CODES.M)
public class TgChooserTargetService extends ChooserTargetService {

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter) {
        final List<ChooserTarget> targets = new ArrayList<>();
        if (!UserConfig.isClientActivated()) {
            return targets;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (!preferences.getBoolean("direct_share", true)) {
            return targets;
        }

        ImageLoader imageLoader = ImageLoader.getInstance();
        final Semaphore semaphore = new Semaphore(0);
        final ComponentName componentName = new ComponentName(getPackageName(), LaunchActivity.class.getCanonicalName());
        /*
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            @Override
            public void run() {
                ArrayList<Integer> dialogs = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                ArrayList<TLRPC.User> users = new ArrayList<>();
                try {
                    ArrayList<Integer> usersToLoad = new ArrayList<>();
                    usersToLoad.add(UserConfig.getClientUserId());
                    ArrayList<Integer> chatsToLoad = new ArrayList<>();
                    SQLiteCursor cursor = MessagesStorage.getInstance().getDatabase().queryFinalized(String.format(Locale.US, "SELECT did FROM dialogs ORDER BY date DESC LIMIT %d,%d", 0, 30));
                    while (cursor.next()) {
                        long id = cursor.longValue(0);

                        int lower_id = (int) id;
                        int high_id = (int) (id >> 32);
                        if (lower_id != 0) {
                            if (high_id == 1) {
                                continue;
                            } else {
                                if (lower_id > 0) {
                                    if (!usersToLoad.contains(lower_id)) {
                                        usersToLoad.add(lower_id);
                                    }
                                } else {
                                    if (!chatsToLoad.contains(-lower_id)) {
                                        chatsToLoad.add(-lower_id);
                                    }
                                }
                            }
                        } else {
                            continue;
                        }
                        dialogs.add(lower_id);
                        if (dialogs.size() == 8) {
                            break;
                        }
                    }
                    cursor.dispose();
                    if (!chatsToLoad.isEmpty()) {
                        //MessagesStorage.getInstance().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    if (!usersToLoad.isEmpty()) {
                        //MessagesStorage.getInstance().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                    }
                } catch (Exception e) {
                    FileLog.e("messenger", e);
                }
                for (int a = 0; a < dialogs.size(); a++) {
                    Bundle extras = new Bundle();
                    Icon icon = null;
                    String name = null;
                    int id = dialogs.get(a);
                    if (id > 0) {
                        for (int b = 0; b < users.size(); b++) {
                            TLRPC.User user = users.get(b);
                            if (user.id == id) {
                                if (!user.bot) {
                                    extras.putLong("dialogId", (long) id);
                                    if (user.photo != null && user.photo.photo_small != null) {
                                        icon = createRoundBitmap(FileLoader.getPathToAttach(user.photo.photo_small, true));
                                    }
                                    name = ContactsController.formatName(user.first_name, user.last_name);
                                }
                                break;
                            }
                        }
                    } else {
                        for (int b = 0; b < chats.size(); b++) {
                            TLRPC.Chat chat = chats.get(b);
                            if (chat.id == -id) {
                                if (!ChatObject.isNotInChat(chat) && (!ChatObject.isChannel(chat) || chat.megagroup)) {
                                    extras.putLong("dialogId", (long) id);
                                    if (chat.photo != null && chat.photo.photo_small != null) {
                                        icon = createRoundBitmap(FileLoader.getPathToAttach(chat.photo.photo_small, true));
                                    }
                                    name = chat.title;
                                }
                                break;
                            }
                        }
                    }
                    if (name != null) {
                        if (icon == null) {
                            icon = Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.logo_avatar);
                        }
                        targets.add(new ChooserTarget(name, icon, 1.0f, componentName, extras));
                    }
                }
                semaphore.release();
            }
        });
        */
        try {
            semaphore.acquire();
        } catch (Exception e) {
            FileLog.e("messenger", e);
        }
        return targets;
    }

    /*
    private Icon createRoundBitmap(File path) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path.toString());
            if (bitmap != null) {
                Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                result.eraseColor(Color.TRANSPARENT);
                Canvas canvas = new Canvas(result);
                BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                if (roundPaint == null) {
                    roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    bitmapRect = new RectF();
                }
                roundPaint.setShader(shader);
                bitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                canvas.drawRoundRect(bitmapRect, bitmap.getWidth(), bitmap.getHeight(), roundPaint);
                return Icon.createWithBitmap(result);
            }
        } catch (Throwable e) {
            FileLog.e("messenger", e);
        }
        return null;
    }
    */
}
