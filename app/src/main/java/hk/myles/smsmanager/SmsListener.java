package hk.myles.smsmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;


public class SmsListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                String from = smsMessage.getDisplayOriginatingAddress();
                String message = smsMessage.getDisplayMessageBody();

                Intent i = new Intent("newSmsReceived");
                // Data you need to pass to activity
                i.putExtra("sender",from);
                i.putExtra("body",message);
                context.sendBroadcast(i);
            }
    }
}
