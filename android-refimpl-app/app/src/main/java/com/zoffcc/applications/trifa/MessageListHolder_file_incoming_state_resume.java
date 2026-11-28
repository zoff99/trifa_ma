/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
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

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zoffcc.applications.sorm.Filetransfer;
import com.zoffcc.applications.sorm.Message;

import java.net.URLConnection;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import static com.zoffcc.applications.trifa.HelperFiletransfer.format_speed;
import static com.zoffcc.applications.trifa.HelperFiletransfer.get_filetransfer_filenum_from_id;
import static com.zoffcc.applications.trifa.HelperFiletransfer.remove_vfs_ft_from_cache;
import static com.zoffcc.applications.trifa.HelperFiletransfer.set_filetransfer_state_from_id;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.long_date_time_format;
import static com.zoffcc.applications.trifa.HelperMessage.set_message_state_from_id;
import static com.zoffcc.applications.trifa.HelperMessage.update_single_message_from_messge_id;
import static com.zoffcc.applications.trifa.MainActivity.tox_file_control;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_CANCEL;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class MessageListHolder_file_incoming_state_resume extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "trifa.MessageListHolder";

    private Context context;

    ImageButton button_cancel;
    ProgressBar ft_progressbar;
    ImageView ft_file_icon;
    TextView textView;
    TextView ft_file_meta;
    TextView ft_percent;
    TextView ft_status;
    TextView date_time;
    de.hdodenhof.circleimageview.CircleImageView img_avatar;

    public MessageListHolder_file_incoming_state_resume(View itemView, Context c)
    {
        super(itemView);

        this.context = c;

        button_cancel = (ImageButton) itemView.findViewById(R.id.ft_button_cancel);
        ft_progressbar = (ProgressBar) itemView.findViewById(R.id.ft_progressbar);
        ft_file_icon = (ImageView) itemView.findViewById(R.id.ft_file_icon);
        textView = (TextView) itemView.findViewById(R.id.m_text);
        ft_file_meta = (TextView) itemView.findViewById(R.id.ft_file_meta);
        ft_percent = (TextView) itemView.findViewById(R.id.ft_percent);
        ft_status = (TextView) itemView.findViewById(R.id.ft_status);
        date_time = (TextView) itemView.findViewById(R.id.date_time);
        img_avatar = (de.hdodenhof.circleimageview.CircleImageView) itemView.findViewById(R.id.img_avatar);
    }

    public void bindMessageList(Message m)
    {
        if (m == null)
        {
            // TODO: should never be null!!
            // only afer a crash
            m = new Message();
        }

        final Message message = m;

        date_time.setText(long_date_time_format(message.rcvd_timestamp));

        // --------- file name and type ---------
        // The file is still arriving, so there is no local thumbnail yet:
        // always show the original line-art placeholder icon tile.
        final String filename = "" + message.text;
        String mimeType = null;
        try
        {
            mimeType = URLConnection.guessContentTypeFromName(filename.toLowerCase());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        textView.setText(filename);
        ft_file_icon.setImageResource(ChatUI.ftTypeIcon(mimeType, filename));

        // --------- progress, percent, speed ---------
        long filesize = 0;
        int percent = 0;
        String status = context.getString(R.string.chat_ft_receiving);
        long ft_id = message.filetransfer_id;
        if (ft_id != -1)
        {
            try
            {
                final Filetransfer ft_ = (Filetransfer) orma.selectFromFiletransfer().idEq(ft_id).get(0);
                filesize = ft_.filesize;
                if (filesize > 0)
                {
                    percent = (int) (100f * (float) ft_.current_position / (float) filesize);
                }
                percent = Math.max(0, Math.min(100, percent));
                long delta_ts_ms = System.currentTimeMillis() - ft_.transfer_start_ts;
                if (delta_ts_ms < 1)
                {
                    delta_ts_ms = 1;
                }
                float speed_bytes_per_second = (((float) ft_.current_position) / (float) delta_ts_ms) * 1000.0f;
                status = status + " " + format_speed(speed_bytes_per_second);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        ft_progressbar.setMax(100);
        ft_progressbar.setProgress(percent);
        ft_percent.setText(percent + "%");
        ft_file_meta.setText(ChatUI.ftTypeLabel(mimeType, filename) + " \u2022 " + ChatUI.ftSize(filesize));
        ft_status.setText(status);

        button_cancel.setVisibility(View.VISIBLE);

        button_cancel.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle(
                            v.getContext().getString(R.string.MessageListHolder_file_incoming_cancel_ft_title));
                    builder.setMessage(
                            v.getContext().getString(R.string.MessageListHolder_file_incoming_cancel_ft_message));

                    builder.setNegativeButton(v.getContext().getString(R.string.MainActivity_no_button), null);
                    builder.setPositiveButton(v.getContext().getString(R.string.MainActivity_yes_button),
                                              new DialogInterface.OnClickListener()
                                              {
                                                  @Override
                                                  public void onClick(DialogInterface dialog, int which)
                                                  {
                                                      cancel_incoming_filetransfer(message);
                                                  }
                                              });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                return true;
            }
        });

        HelperGeneric.fill_friend_avatar_icon(m, context, img_avatar);
        HelperGeneric.set_avatar_img_height_in_chat(img_avatar);
    }

    private void cancel_incoming_filetransfer(final Message message)
    {
        try
        {
            // cancel FT
            Log.i(TAG, "button_cancel:OnTouch:001");
            tox_file_control(tox_friend_by_public_key__wrapper(message.tox_friendpubkey),
                             get_filetransfer_filenum_from_id(message.filetransfer_id), TOX_FILE_CONTROL_CANCEL.value);
            set_filetransfer_state_from_id(message.filetransfer_id, TOX_FILE_CONTROL_CANCEL.value);
            set_message_state_from_id(message.id, TOX_FILE_CONTROL_CANCEL.value);

            remove_vfs_ft_from_cache(message);

            button_cancel.setVisibility(View.GONE);
            ft_progressbar.setVisibility(View.GONE);

            // update message view
            update_single_message_from_messge_id(message.id, true);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void onClick(View v)
    {
        Log.i(TAG, "onClick");
    }

    @Override
    public boolean onLongClick(final View v)
    {
        Log.i(TAG, "onLongClick");
        return true;
    }
}
