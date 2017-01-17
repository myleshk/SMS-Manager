package hk.myles.smsmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;


public class SmsListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {

            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                String from = smsMessage.getDisplayOriginatingAddress();
                String message = smsMessage.getDisplayMessageBody();
                Long timestamp = smsMessage.getTimestampMillis() / 1000;

                // Retrieves a map of extended data from the intent.
                Bundle bundle = intent.getExtras();

                int slot = bundle.getInt("slot", -1);

                Intent i = new Intent("newSmsReceived");
                // Data you need to pass to activity
                i.putExtra("sender", from);
                i.putExtra("body", message);
                i.putExtra("slot", String.valueOf(slot));
                i.putExtra("timestamp", String.valueOf(timestamp));
                context.sendBroadcast(i);
            }
        }
    }
}
