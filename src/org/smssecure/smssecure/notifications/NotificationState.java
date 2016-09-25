package org.smssecure.smssecure.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import org.smssecure.smssecure.ConversationActivity;
import org.smssecure.smssecure.ConversationPopupActivity;
import org.smssecure.smssecure.database.RecipientPreferenceDatabase.VibrateState;
import org.smssecure.smssecure.recipients.Recipients;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NotificationState {

  private static final String TAG = NotificationState.class.getSimpleName();

  private final LinkedList<NotificationItem> notifications = new LinkedList<>();
  private final Set<Long>                    threads       = new HashSet<>();

  private int notificationCount = 0;

  public void addNotification(NotificationItem item) {
    notifications.addFirst(item);
    threads.add(item.getThreadId());
    notificationCount++;
  }

  public @Nullable Uri getRingtone() {
    if (!notifications.isEmpty()) {
      Recipients recipients = notifications.getFirst().getRecipients();

      if (recipients != null) {
        return recipients.getRingtone();
      }
    }

    return null;
  }

  public VibrateState getVibrate() {
    if (!notifications.isEmpty()) {
      Recipients recipients = notifications.getFirst().getRecipients();

      if (recipients != null) {
        return recipients.getVibrate();
      }
    }

    return VibrateState.DEFAULT;
  }

  public boolean hasMultipleThreads() {
    return threads.size() > 1;
  }

  public int getThreadCount() {
    return threads.size();
  }

  public int getMessageCount() {
    return notificationCount;
  }

  public List<NotificationItem> getNotifications() {
    return notifications;
  }

  public PendingIntent getMarkAsReadIntent(Context context) {
    long[] threadArray = new long[threads.size()];
    int    index       = 0;

    for (long thread : threads) {
      threadArray[index++] = thread;
    }

    Intent intent = new Intent(MarkReadReceiver.CLEAR_ACTION);
    intent.putExtra(MarkReadReceiver.THREAD_IDS_EXTRA, threadArray);
    intent.setPackage(context.getPackageName());

    // XXX : This is an Android bug.  If we don't pull off the extra
    // once before handing off the PendingIntent, the array will be
    // truncated to one element when the PendingIntent fires.  Thanks guys!
    Log.i(TAG, "Pending thread id array of intent length: " +
        intent.getLongArrayExtra(MarkReadReceiver.THREAD_IDS_EXTRA).length);

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getDeleteIntent(Context context){

    long[]    idArray     = new long[notificationCount];
    boolean[] isMmsArray  = new boolean[notificationCount];
    long[]    threadArray = new long[threads.size()];
    int       index       = 0;

    for (NotificationItem notificationItem : notifications) {
      isMmsArray[index] = notificationItem.isMms();
      idArray[index++]  = notificationItem.getMessageId();
    }
    index = 0;
    for (long thread : threads) {
      threadArray[index++] = thread;
    }

    Intent intent = new Intent(DeleteReceiver.DELETE_ACTION);
    intent.putExtra(DeleteReceiver.MSG_IDS_EXTRA, idArray);
    intent.putExtra(DeleteReceiver.MSG_IS_MMS_EXTRA, isMmsArray);
    intent.putExtra(DeleteReceiver.THREAD_IDS_EXTRA, threadArray);
    intent.setPackage(context.getPackageName());

    // XXX : This is an Android bug (same as above)
    Log.i(TAG, "Pending id array of intent length: " +
            intent.getLongArrayExtra(DeleteReceiver.MSG_IDS_EXTRA).length);
    Log.i(TAG, "Pending mms array of intent length: " +
            intent.getBooleanArrayExtra(DeleteReceiver.MSG_IS_MMS_EXTRA).length);
    Log.i(TAG, "Pending thread id array of intent length: " +
            intent.getLongArrayExtra(DeleteReceiver.THREAD_IDS_EXTRA).length);

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getWearableReplyIntent(Context context, Recipients recipients) {
    if (threads.size() != 1) throw new AssertionError("We only support replies to single thread notifications!");

    Intent intent = new Intent(WearReplyReceiver.REPLY_ACTION);
    intent.putExtra(WearReplyReceiver.RECIPIENT_IDS_EXTRA, recipients.getIds());
    intent.setPackage(context.getPackageName());

    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public PendingIntent getQuickReplyIntent(Context context, Recipients recipients) {
    if (threads.size() != 1) throw new AssertionError("We only support replies to single thread notifications!");

    Intent intent = new Intent(context, ConversationPopupActivity.class);
    intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, recipients.getIds());
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, (long)threads.toArray()[0]);
    intent.setData((Uri.parse("custom://"+System.currentTimeMillis())));

    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
