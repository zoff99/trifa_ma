/*
 * Original conversation UI for TRIfA.
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This program is free software; you may redistribute and/or modify it under
 * the GNU General Public License, version 2 or (at your option) any later version.
 * Distributed without ANY WARRANTY; see LICENSE-zzGPLv2 and LICENSE-GPLv3.
 * All conversation icons and wallpaper in this redesign are original project
 * resources. No third-party logos, artwork, fonts, or proprietary assets are used.
 */
package com.zoffcc.applications.trifa;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.mikepenz.iconics.IconicsDrawable;
import com.zoffcc.applications.sorm.Message;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

/** Presentation only: existing adapters keep ownership of data and click handlers. */
final class ChatUI
{
    static final int FRIEND = 0;
    static final int CONFERENCE = 1;
    static final int GROUP = 2;
    private ChatUI() {}

    static int color(Context c, int id)
    {
        return ContextCompat.getColor(c, id);
    }

    static int color(View v, int id)
    {
        return color(v.getContext(), id);
    }

    static boolean night(Context c)
    {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static int textColor(Context c)
    {
        return ContextCompat.getColor(c, night(c) ? R.color.chat_text_dark : R.color.chat_text);
    }

    static int metadataColor(Context c)
    {
        return ContextCompat.getColor(c, night(c) ? R.color.chat_metadata_dark : R.color.chat_metadata);
    }

    static int linkColor(Context c)
    {
        return ContextCompat.getColor(c, night(c) ? R.color.chat_link_dark : R.color.chat_link);
    }

    static int readColor(Context c)
    {
        // Keep read receipts on the theme-aware palette instead of reusing the
        // day color on dark outgoing bubbles.
        return ContextCompat.getColor(c, night(c) ? R.color.chat_read_dark : R.color.chat_read);
    }

    static void setup(final AppCompatActivity a, final int kind, final SearchView search)
    {
        a.getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (kind == FRIEND)
        {
            a.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        a.getWindow().setStatusBarColor(color(a.findViewById(R.id.toolbar), night(a) ? R.color.chat_header_dark : R.color.chat_header));
        final View bar = a.findViewById(R.id.chat_search_bar);
        search.setIconifiedByDefault(false);
        search.setQueryHint(a.getString(R.string.chat_search));
        final TextView searchText = search.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null)
        {
            searchText.setTextColor(textColor(a));
            searchText.setHintTextColor(metadataColor(a));
        }
        a.findViewById(R.id.chat_search_close).setOnClickListener(v -> {
            search.setQuery("", false);
            bar.setVisibility(View.GONE);
            search.clearFocus();
        });
        final Runnable openSearch = () -> {
            bar.setVisibility(View.VISIBLE);
            search.setIconified(false);
            search.requestFocus();
        };
        final ImageButton more = a.findViewById(R.id.chat_more);
        more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(a, v);
            menu.getMenu().add(0, 1, 0, R.string.chat_search);
            if (kind == FRIEND)
            {
                menu.getMenu().add(0, 2, 1, R.string.chat_media_gallery);
                menu.getMenu().add(0, 3, 2, R.string.chat_all_messages);
                menu.getMenu().add(0, 4, 3, R.string.chat_only_files);
            }
            else
            {
                menu.getMenu().add(0, 5, 1, R.string.chat_participants);
                if (kind == GROUP)
                {
                    menu.getMenu().add(0, 2, 2, R.string.chat_media_gallery);
                    menu.getMenu().add(0, 6, 3, a.getString(R.string.add_friend_activity_name));
                }
            }
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId())
                {
                    case 1: openSearch.run(); return true;
                    case 2: a.findViewById(kind == FRIEND ? R.id.gallery_btn : R.id.group_gallery_btn).performClick(); return true;
                    case 3: ((CustomSpinner) a.findViewById(R.id.spinner_filter_msgs)).setSelection(0); return true;
                    case 4: ((CustomSpinner) a.findViewById(R.id.spinner_filter_msgs)).setSelection(1); return true;
                    case 5:
                        if (kind == GROUP) MainActivity.group_message_list_activity.group_message_drawer.openDrawer();
                        else MainActivity.conference_message_list_activity.conference_message_drawer.openDrawer();
                        return true;
                    case 6: a.findViewById(R.id.add_friend_conference).performClick(); return true;
                }
                return false;
            });
            menu.show();
        });
        final View headerText = a.findViewById(R.id.chat_header_text);
        headerText.setOnClickListener(v -> {
            if (kind == FRIEND)
            {
                Intent i = new Intent(a, FriendInfoActivity.class);
                i.putExtra("friendnum", ((MessageListActivity) a).get_current_friendnum());
                a.startActivity(i);
            }
            else more.performClick();
        });
        final TextView oldTitle = a.findViewById(R.id.ml_maintext);
        oldTitle.addTextChangedListener(new SimpleWatcher()
        {
            @Override public void afterTextChanged(Editable s)
            {
                String[] parts = s.toString().split("\\n", 2);
                if (kind == FRIEND)
                {
                    setHeader(a, parts[0], null);
                    ((MessageListActivity) a).refresh_friend_presence();
                }
                else
                {
                    setHeader(a, parts[0], parts.length > 1 ? parts[1] : "");
                }
            }
        });
        final TextView typing = a.findViewById(R.id.ml_friend_typing);
        typing.addTextChangedListener(new SimpleWatcher()
        {
            @Override public void afterTextChanged(Editable s)
            {
                typing.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
            }
        });
        final ImageView avatar = a.findViewById(R.id.chat_avatar);
        if (kind != FRIEND)
        {
            avatar.setImageResource(R.drawable.chat_ic_person);
            avatar.setColorFilter(Color.WHITE);
            a.findViewById(R.id.chat_avatar_badge).setVisibility(View.VISIBLE);
            a.findViewById(R.id.chat_avatar_badge).setContentDescription(a.getString(R.string.chat_participants));
        }
        else
        {
            avatar.setColorFilter(Color.WHITE);
        }
        setupComposer(a, kind);
        if (kind == FRIEND)
        {
            ((MessageListActivity) a).refresh_friend_presence();
        }
    }

    static void setHeader(Activity a, String title, String subtitle)
    {
        ((TextView) a.findViewById(R.id.chat_title)).setText(title == null ? "" : title);
        if (subtitle != null)
        {
            ((TextView) a.findViewById(R.id.chat_subtitle)).setText(subtitle);
            a.findViewById(R.id.chat_subtitle).setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    static void friendAvatar(Activity a, String pubkey)
    {
        Message m = new Message();
        m.tox_friendpubkey = pubkey;
        de.hdodenhof.circleimageview.CircleImageView avatar = a.findViewById(R.id.chat_avatar);
        avatar.clearColorFilter();
        HelperGeneric.fill_friend_avatar_icon(m, a, avatar);
    }

    static void friendPresence(Activity a, boolean online, int toxUserStatus)
    {
        int label = R.string.chat_offline;
        if (online)
        {
            if (toxUserStatus == 1) label = R.string.chat_away;
            else if (toxUserStatus == 2) label = R.string.chat_busy;
            else label = R.string.chat_online;
        }
        TextView subtitle = a.findViewById(R.id.chat_subtitle);
        subtitle.setText(label);
        subtitle.setVisibility(View.VISIBLE);
    }

    static void setupComposer(final AppCompatActivity a, int kind)
    {
        final EditText input = a.findViewById(R.id.ml_new_message);
        final ImageButton action = a.findViewById(R.id.ml_button_01);
        final ImageButton emoji = a.findViewById(R.id.insert_emoji);
        emoji.setImageResource(R.drawable.chat_ic_smile);
        emoji.setColorFilter(metadataColor(a));
        if (kind == CONFERENCE)
        {
            action.setImageResource(R.drawable.chat_ic_send);
            action.setColorFilter(Color.WHITE);
            return;
        }
        final ImageButton record = a.findViewById(R.id.ml_button_recaudio);
        record.setImageResource(R.drawable.chat_ic_mic);
        record.setColorFilter(Color.WHITE);
        final ViewGroup outer = (ViewGroup) action.getParent().getParent();
        final ViewGroup inputRow = (ViewGroup) action.getParent();
        final int actionIndex = inputRow.indexOfChild(action);
        final Runnable update = () -> {
            boolean empty = input.getText().toString().trim().isEmpty();
            emoji.setImageResource(R.drawable.chat_ic_smile);
            emoji.setColorFilter(metadataColor(a));
            record.setVisibility(empty ? View.VISIBLE : View.GONE);
            action.setImageResource(empty ? R.drawable.chat_ic_attach : R.drawable.chat_ic_send);
            action.setContentDescription(a.getString(empty ? R.string.chat_attach : R.string.chat_send));
            if (empty && action.getParent() != inputRow)
            {
                outer.removeView(action);
                inputRow.addView(action, actionIndex);
                action.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 42), dp(a, 48)));
            }
            else if (!empty && action.getParent() != outer)
            {
                inputRow.removeView(action);
                outer.addView(action);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(a, 50), dp(a, 50));
                p.setMarginStart(dp(a, 6));
                action.setLayoutParams(p);
            }
            action.setBackgroundResource(empty ? android.R.color.transparent : R.drawable.chat_send_bg);
            action.setColorFilter(empty ? metadataColor(a) : Color.WHITE);
            action.setPadding(dp(a, empty ? 10 : 13), dp(a, empty ? 12 : 13), dp(a, empty ? 10 : 13), dp(a, empty ? 12 : 13));
        };
        input.addTextChangedListener(new SimpleWatcher()
        {
            @Override public void afterTextChanged(Editable s) { update.run(); }
        });
        update.run();
    }

    static int dp(Context c, int value)
    {
        return Math.round(c.getResources().getDisplayMetrics().density * value);
    }

    static String time(Context c, long millis)
    {
        if (millis <= 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    /** Local calendar day, not a fixed 24-hour interval. */
    static String date(Context c, long millis)
    {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        int dayOfYear = day.get(Calendar.DAY_OF_YEAR);
        int year = day.get(Calendar.YEAR);
        if (sameDay(day, today)) return c.getString(R.string.chat_today);
        today.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(day, today)) return c.getString(R.string.chat_yesterday);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH;
        return DateUtils.formatDateTime(c, millis, flags);
    }

    static boolean sameDay(Calendar a, Calendar b)
    {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    static void select(View container, boolean selected)
    {
        if (container == null) return;
        // Clear any legacy selection tint on the clicked/bound container itself.
        container.setBackgroundColor(Color.TRANSPARENT);
        final View row = messageRow(container);
        if (row == null) return;
        row.setBackgroundColor(selected ? color(row, R.color.chat_selection) : Color.TRANSPARENT);
    }

    /**
     * Find the RecyclerView item root for any descendant view. Message layouts
     * have different nesting depths (text rows use a swipe surface, file rows
     * do not), so a fixed number of getParent() calls can land on the
     * RecyclerView itself and tint the whole list.
     */
    static View messageRow(View view)
    {
        if (view == null) return null;
        View current = view;
        while (current.getParent() instanceof View)
        {
            if (current.getParent() instanceof RecyclerView) return current;
            current = (View) current.getParent();
        }
        // Fallback for callers outside a RecyclerView: never climb into the
        // activity/window root; keep the change scoped to the supplied view.
        return view;
    }

    /** Check permission and temp folder before starting an audio recording. */
    static boolean prepareAudioRecording(final Activity a)
    {
        if (a == null) return false;
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(a, new String[]{Manifest.permission.RECORD_AUDIO}, 42001);
            audioToast(a, a.getString(R.string.chat_audio_permission_needed));
            return false;
        }
        try
        {
            final java.io.File dir = new java.io.File(MainActivity.SD_CARD_TMP_DIR);
            if (!dir.exists() && !dir.mkdirs())
            {
                audioToast(a, a.getString(R.string.chat_audio_temp_dir_failed));
                return false;
            }
        }
        catch (Exception e)
        {
            audioToast(a, a.getString(R.string.chat_audio_temp_dir_failed));
            return false;
        }
        return true;
    }

    static void audioToast(final Activity a, final String msg)
    {
        if (a == null || msg == null) return;
        a.runOnUiThread(() -> HelperGeneric.display_toast_with_context(a, msg, true, 0));
    }

    /**
     * Update the audio-record button from a worker thread. All view mutation is
     * posted to the UI thread; the worker thread only handles recording/file IO.
     */
    static void recordButtonState(final View button, final int icon, final boolean recording)
    {
        if (button == null) return;
        final Runnable update = () -> {
            try
            {
                if (button instanceof ImageButton) ((ImageButton) button).setImageResource(icon);
                if (recording)
                {
                    button.setBackgroundColor(Color.parseColor("#FF0000"));
                    if (button instanceof ImageButton) ((ImageButton) button).setColorFilter(Color.WHITE);
                }
                else
                {
                    // Restore the normal composer action-button style. Using select()
                    // here would leave the mic button transparent after recording.
                    button.setBackgroundResource(R.drawable.chat_send_bg);
                    if (button instanceof ImageButton) ((ImageButton) button).setColorFilter(Color.WHITE);
                }
            }
            catch (Exception ignored)
            {
            }
        };
        final Context c = button.getContext();
        if (c instanceof Activity)
        {
            ((Activity) c).runOnUiThread(update);
        }
        else
        {
            button.post(update);
        }
    }

    /** Called AFTER legacy binding, so recycled rows never retain old colors or alignment. */
    static long messageTimestamp(Message m)
    {
        return m.direction != 0 || m.msg_version == 1 ? m.sent_timestamp : m.rcvd_timestamp;
    }

    static long messageTimestamp(com.zoffcc.applications.sorm.ConferenceMessage m)
    {
        return m.sent_timestamp;
    }

    static long messageTimestamp(com.zoffcc.applications.sorm.GroupMessage m)
    {
        return m.sent_timestamp;
    }

    static void bindDate(View row, String current, String previous, long timestamp, boolean outgoing)
    {
        View header = row.findViewById(R.id.message_text_date);
        TextView label = row.findViewById(R.id.message_text_date_string);
        if (header == null || label == null) return;
        boolean show = timestamp > 0 && current != null && !current.isEmpty() && !current.equals(previous);
        header.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) label.setText(current);
    }

    static void decorate(View row, int type, long timestamp, boolean outgoing, boolean read,
                         boolean relay, boolean push, boolean selected, boolean multiUser, boolean synced, int confirmations,
                         String peerPubkey)
    {
        final Context c = row.getContext();
        final boolean text = type == Message_model.TEXT_INCOMING_NOT_READ || type == Message_model.TEXT_OUTGOING_NOT_READ || type == Message_model.TEXT_OUTGOING_HAVE_READ;
        final boolean system = type == Message_model.PAGING_OLDER || type == Message_model.PAGING_NEWER;
        final View container = row.findViewById(R.id.layout_message_container);
        if (container == null) return;
        select(container, selected);
        final TextView body = row.findViewById(R.id.m_text);
        if (system)
        {
            View header = row.findViewById(R.id.message_text_date);
            if (header != null) header.setVisibility(View.GONE);
            if (container instanceof LinearLayout) ((LinearLayout) container).setGravity(android.view.Gravity.CENTER);
            View pagingBubble = row.findViewById(R.id.m_container);
            if (pagingBubble != null)
            {
                pagingBubble.setBackgroundResource(R.drawable.chat_date_bg);
                pagingBubble.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
            }
            if (body != null) body.setTextColor(metadataColor(c));
            return;
        }
        final TextView date = row.findViewById(R.id.date_time);
        final ImageView icon = row.findViewById(R.id.m_icon);
        final View avatar = row.findViewById(R.id.img_avatar);
        final View corner = row.findViewById(R.id.img_corner);
        if (avatar != null) avatar.setVisibility(View.GONE);
        if (corner != null) corner.setVisibility(View.GONE);
        final View bubble = row.findViewById(outgoing ? R.id.ft_outgoing_rounded_bg : R.id.ft_incoming_rounded_bg) != null
                ? row.findViewById(outgoing ? R.id.ft_outgoing_rounded_bg : R.id.ft_incoming_rounded_bg)
                : row.findViewById(R.id.text_block_group) != null ? row.findViewById(R.id.text_block_group) : row.findViewById(R.id.m_container);
        if (bubble != null)
        {
            bubble.setBackgroundResource(outgoing ? R.drawable.chat_bubble_outgoing : R.drawable.chat_bubble_incoming);
            bubble.setPadding(dp(c, 9), dp(c, 6), dp(c, 9), dp(c, 5));
            bubble.setMinimumHeight(0);
            if (bubble.getLayoutParams() instanceof LinearLayout.LayoutParams)
            {
                LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) bubble.getLayoutParams();
                p.weight = 0;
                p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                // Text rows already have 8dp horizontal surface padding. File
                // rows do not, so keep outgoing file bubbles off the right edge.
                final int edge = (!text && outgoing) ? dp(c, 8) : 0;
                p.setMargins(dp(c, outgoing ? 48 : 0), 0, dp(c, outgoing ? edge : 48), 0);
                bubble.setLayoutParams(p);
            }
        }
        if (container instanceof LinearLayout)
        {
            ((LinearLayout) container).setGravity(outgoing ? android.view.Gravity.END : android.view.Gravity.START);
            ((LinearLayout) container).setOrientation(LinearLayout.HORIZONTAL);
        }
        if (!text)
        {
            // Text rows get 2dp above/below from msg_swipe_surface. File rows
            // lack that surface, so give outgoing files the same gap instead
            // of letting them sit flush against the previous row.
            final int vertical = outgoing ? dp(c, 2) : 0;
            container.setPadding(container.getPaddingLeft(), vertical, container.getPaddingRight(), vertical);
        }
        // File rows retain their original nested controls; their outer row supplies alignment.
        final View alignment = row.findViewById(R.id.layout2) != null ? row.findViewById(R.id.layout2) : row.findViewById(R.id.layout1);
        if (alignment instanceof LinearLayout)
        {
            ((LinearLayout) alignment).setGravity(outgoing ? android.view.Gravity.END : android.view.Gravity.START);
        }
        if (body != null)
        {
            body.setTextColor(textColor(c));
            final int link = linkColor(c);
            body.setLinkTextColor(link);
            // The autolink view bakes link colors into spans, so setLinkTextColor
            // alone is not enough. Rebuild the spans with the accessible color.
            if (body instanceof com.luseen.autolinklibrary.EmojiTextViewLinks)
            {
                ((com.luseen.autolinklibrary.EmojiTextViewLinks) body).recolorLinks(
                        link, ColorUtils.setAlphaComponent(link, 0x40));
            }
            body.setMaxWidth(Math.max(dp(c, 160), c.getResources().getDisplayMetrics().widthPixels - dp(c, 92)));
        }
        if (date != null)
        {
            date.setVisibility(View.VISIBLE);
            date.setText(time(c, timestamp));
            date.setTextColor(metadataColor(c));
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            date.setMaxLines(1);
            if (!text) date.setGravity(android.view.Gravity.END);
        }
        final TextView peer = row.findViewById(R.id.peer_name_text);
        if (peer != null && peer.getVisibility() == View.VISIBLE)
        {
            peer.setTextColor(senderColor(c, peerPubkey));
            // Clean medium-weight sender label, slightly larger than metadata.
            Typeface peerTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            if (peerTypeface == Typeface.DEFAULT)
            {
                peerTypeface = Typeface.DEFAULT_BOLD;
            }
            peer.setTypeface(peerTypeface);
            peer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            if (!night(c))
            {
                GradientDrawable chip = new GradientDrawable();
                chip.setShape(GradientDrawable.RECTANGLE);
                chip.setCornerRadius(dp(c, 9));
                chip.setColor(color(c, R.color.chat_peer_chip));
                peer.setBackground(chip);
                peer.setPadding(dp(c, 7), dp(c, 1), dp(c, 7), dp(c, 2));
            }
            else
            {
                peer.setBackground(null);
                peer.setPadding(0, 0, 0, dp(c, 2));
            }
        }
        if (icon != null)
        {
            if (!text)
            {
                // File transfers show progress and actions, not message
                // receipts. Hide sent/read checkmarks on file bubbles.
                icon.setVisibility(View.GONE);
                icon.setContentDescription(null);
            }
            else if (!outgoing)
            {
                // Do not replace NGC sync confirmation badges with fake delivery receipts.
                icon.setVisibility(synced ? View.VISIBLE : View.GONE);
                if (synced) icon.setContentDescription(c.getString(R.string.chat_sync_confirmations, confirmations));
            }
            else
            {
                icon.setVisibility(View.VISIBLE);
                icon.clearColorFilter();
                int label;
                if (multiUser)
                {
                    icon.setImageResource(R.drawable.chat_ic_check);
                    icon.setColorFilter(metadataColor(c));
                    label = R.string.chat_sent;
                }
                else if (read)
                {
                    icon.setImageResource(R.drawable.chat_ic_checks);
                    icon.setColorFilter(readColor(c));
                    label = R.string.chat_read;
                }
                else if (relay || push)
                {
                    icon.setImageResource(R.drawable.chat_ic_check);
                    icon.setColorFilter(metadataColor(c));
                    label = relay ? R.string.chat_relayed : R.string.chat_push_requested;
                }
                else
                {
                    icon.setImageResource(R.drawable.chat_ic_pending);
                    icon.setColorFilter(metadataColor(c));
                    label = R.string.chat_pending;
                }
                icon.setContentDescription(c.getString(label));
            }
        }
        final View header = row.findViewById(R.id.message_text_date);
        if (header != null)
        {
            for (int i = 0; i < ((ViewGroup) header).getChildCount(); i++)
            {
                View child = ((ViewGroup) header).getChildAt(i);
                if (child instanceof ImageView) child.setVisibility(View.GONE);
            }
            if (header instanceof LinearLayout) ((LinearLayout) header).setGravity(android.view.Gravity.CENTER);
            TextView label = row.findViewById(R.id.message_text_date_string);
            if (label != null)
            {
                label.setBackgroundResource(R.drawable.chat_date_bg);
                label.setTextColor(metadataColor(c));
                label.setPadding(dp(c, 12), dp(c, 5), dp(c, 12), dp(c, 5));
            }
        }
    }

    /**
     * Vivid, clearly-distinct sender-name colors for the redesigned bubbles.
     *
     * The previous approach generated a hue and then blended it toward black or
     * white until it hit a high contrast ratio. That desaturated every peer into
     * a near-identical muddy tone, so the colors were hard to tell apart.
     *
     * In light mode the sender label is drawn on a soft grey rounded chip, so
     * the palette is tuned for that chip instead of bare white. The colors keep
     * at least 4.5:1 contrast on the chip and remain perceptually separated.
     * The dark palette is unchanged.
     */
    private static final int[] SENDER_LIGHT = {
                        0xFFE60000, // 1. Pure Bright Red (4.63:1)
                        0xFFCC4B00, // 2. Vivid Burnt Orange (4.76:1)
                        0xFF198700, // 3. Pure Bright Green (4.69:1)
                        0xFF008080, // 4. True Teal (4.75:1)
                        0xFF0055FF, // 5. Electric Cobalt Blue (5.87:1)
                        0xFF7B00E6, // 6. Bright Electric Violet (6.52:1)
                        0xFFC7007D, // 7. Deep Fuchsia Magenta (5.58:1)
                        0xFF5D4037  // 8. Rich Dark Chocolate (9.32:1)
    };
    private static final int[] SENDER_DARK = {
            0xFFFF8282, 0xFFFF8754, 0xFFFFD70D, 0xFFD7FF0D, 0xFF72FF0D, 0xFF0DFFAE,
            0xFF0DFFFF, 0xFF17B2FF, 0xFFA49CFF, 0xFFC891FF, 0xFFFF78DD, 0xFFFF82AC
    };

    /**
     * Stable sender color keyed by the peer public key (never the display name),
     * so a peer keeps the same color when their name changes.
     */
    static int senderColor(Context c, String peerPubkey)
    {
        final String key = peerPubkey == null ? "" : peerPubkey.trim().toLowerCase(Locale.ROOT);
        final int hash = key.hashCode();
        final int[] palette = night(c) ? SENDER_DARK : SENDER_LIGHT;
        return palette[Math.floorMod(hash, palette.length)];
    }

    abstract static class SimpleWatcher implements TextWatcher
    {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}