package org.smssecure.smssecure.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;

public class DeleteReceiver extends MasterSecretBroadcastReceiver {

  private static final String TAG              = DeleteReceiver.class.getSimpleName();
  public static final  String DELETE_ACTION    = "org.smssecure.smssecure.notifications.DELETE_MSGS";
  public static final  String MSG_IDS_EXTRA    = "msg_ids";
  public static final  String MSG_IS_MMS_EXTRA = "msg_is_mms";
  public static final  String THREAD_IDS_EXTRA = "thread_ids";

  @Override
  protected void onReceive(final Context context, Intent intent,
                           @Nullable final MasterSecret masterSecret)
  {
    if (!DELETE_ACTION.equals(intent.getAction()))
      return;

    final long[]    messageIds   = intent.getLongArrayExtra(MSG_IDS_EXTRA);
    final boolean[] messageIsMms = intent.getBooleanArrayExtra(MSG_IS_MMS_EXTRA);
    final long[]    threadIds    = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

    if (messageIds == null || messageIsMms == null || threadIds == null ||
            messageIds.length != messageIsMms.length){
      Log.e(TAG, "Bad extras received");
      return;
    }

    MessageNotifier.cancelNotification(context);

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        for (int i = 0; i < messageIds.length; i++) {
          if (messageIsMms[i]) {
            DatabaseFactory.getMmsDatabase(context).delete(messageIds[i]);
          } else {
            DatabaseFactory.getSmsDatabase(context).deleteMessage(messageIds[i]);
          }
        }
        for (long threadId : threadIds) {
          DatabaseFactory.getThreadDatabase(context).setRead(threadId);
        }

        MessageNotifier.updateNotification(context, masterSecret);
        return null;
      }
    }.execute();
  }
}
