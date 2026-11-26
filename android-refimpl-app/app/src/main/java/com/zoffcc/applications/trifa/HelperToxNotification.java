/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2020 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.zoffcc.applications.trifa.MainActivity.PREF__orbot_enabled;
import static com.zoffcc.applications.trifa.MainActivity.context_s;
import static com.zoffcc.applications.trifa.MainActivity.nmn3;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONNECTION_STATUS_MANUAL_LOGOUT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.bootstrapping;
import static com.zoffcc.applications.trifa.TrifaToxService.manually_logged_out;

public class HelperToxNotification
{
    private static final String TAG = "trifa.Hlp.ToxNoti";
    static int ONGOING_NOTIFICATION_ID = 1030;

    static Notification tox_notification_setup(Context c, NotificationManager nmn2)
    {
        Log.i(TAG, "tox_notification_setup:start");

        Intent notificationIntent = new Intent(c, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String title;
        if (PREF__orbot_enabled) {
            title = "Tox Service: OFFLINE  [Tor Proxy]";
        } else {
            title = "Tox Service: OFFLINE";
        }

        Notification notification2 = build_notification(c, pendingIntent, 0, title);

        Log.i(TAG, "tox_notification_setup:end");
        return notification2;
    }

    static void tox_notification_cancel(Context c)
    {
        Log.i(TAG, "tox_notification_cancel:start");

        try
        {
            // remove the notification
            NotificationManager nmn2 = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
            nmn2.cancel(ONGOING_NOTIFICATION_ID);
            Log.i(TAG, "tox_notification_cancel:OK");
        }
        catch (Exception e3)
        {
            e3.printStackTrace();
        }

        Log.i(TAG, "tox_notification_cancel:end");
    }

    static void tox_notification_change(Context c, NotificationManager nmn2, int a_TOXCONNECTION, String message, final boolean battery_saving_mode_active)
    {
        Log.i(TAG, "tox_notification_change:start");

        Intent notificationIntent = new Intent(c, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String title;
        if ((manually_logged_out) || (a_TOXCONNECTION == CONNECTION_STATUS_MANUAL_LOGOUT))
        {
            title = "Tox Service: OFFLINE manually";
        }
        else if (bootstrapping)
        {
            Log.i(TrifaToxService.TAG, "change_notification_fg:bootstrapping=true");
            if (PREF__orbot_enabled) {
                title = "Tox Service: Bootstrapping [Tor Proxy] " + message;
            } else {
                title = "Tox Service: Bootstrapping " + message;
            }
        }
        else if (a_TOXCONNECTION == 0)
        {
            String offline_text = "OFFLINE";
            if (battery_saving_mode_active) {
                offline_text = "Battery Saving";
            }
            if (PREF__orbot_enabled) {
                title = "Tox Service: " + offline_text + " [Tor Proxy] " + message;
            } else {
                title = "Tox Service: " + offline_text + " " + message;
            }
        }
        else if (PREF__orbot_enabled)
        {
            title = "Tox Service: ONLINE [Tor Proxy] " + message;
        }
        else if (a_TOXCONNECTION == 1)
        {
            title = "Tox Service: ONLINE [TCP] " + message;
        }
        else
        {
            title = "Tox Service: ONLINE [UDP] " + message;
        }

        try {
            nmn2.notify(ONGOING_NOTIFICATION_ID, build_notification(c, pendingIntent, a_TOXCONNECTION, title));
        } catch(Exception ignored) {
        }
        Log.i(TAG, "tox_notification_change:end");
    }

    private static Notification build_notification(Context c, PendingIntent pendingIntent, int a_TOXCONNECTION, String title)
    {
        // Use the standard system notification template instead of a custom
        // RemoteViews layout. The OS supplies the correct background and text
        // colors for light/dark mode and for vendor notification styles.
        return new NotificationCompat.Builder(c, MainActivity.channelId_toxservice)
                .setSmallIcon(getSmallIconForState(a_TOXCONNECTION))
                .setContentTitle(title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();
    }

    // Helper to keep the switch statement clean
    private static int getSmallIconForState(int a_TOXCONNECTION) {
        if (manually_logged_out || a_TOXCONNECTION == CONNECTION_STATUS_MANUAL_LOGOUT) return R.drawable.circle_manuallyoffline_notification;
        if (bootstrapping) return R.drawable.circle_orange_notification;
        if (a_TOXCONNECTION == 0) return R.drawable.circle_red_notification;
        if (PREF__orbot_enabled) return R.drawable.circle_torproxy_notification;
        return R.drawable.circle_green_notification; // TCP or UDP
    }

    static void tox_notification_change_wrapper(int a_TOXCONNECTION, final String message)
    {
        tox_notification_change_wrapper(a_TOXCONNECTION, message, false);
    }

    static void tox_notification_change_wrapper(int a_TOXCONNECTION, final String message, final boolean battery_saving_mode_active)
    {
        Log.i(TAG, "tox_notification_change_wrapper:start");
        final int a_TOXCONNECTION_f = a_TOXCONNECTION;
        final Context static_context = context_s;

        try {
            Thread t = new Thread() {
                @Override
                public void run() {
                    long counter = 0;
                    while (MainActivity.tox_service_fg == null) {
                        counter++;
                        if (counter > 10) break;
                        try { Thread.sleep(100); } catch (Exception ignored) {}
                    }

                    try
                    {
                        tox_notification_change(static_context, nmn3, a_TOXCONNECTION_f, message, battery_saving_mode_active);
                        Log.i(TAG, "tox_notification_change_wrapper:DONE");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "tox_notification_change_wrapper:EE01:" + e.getMessage());
        }

        Log.i(TAG, "tox_notification_change_wrapper:end");
    }
}
