package hk.myles.smsmanager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.wilddog.client.DataSnapshot;
import com.wilddog.client.SyncError;
import com.wilddog.client.SyncReference;
import com.wilddog.client.ValueEventListener;
import com.wilddog.client.WilddogSync;
import com.wilddog.wilddogcore.WilddogApp;
import com.wilddog.wilddogcore.WilddogOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;


public class MainActivity extends FragmentActivity
        implements OnFragmentInteractionListener {

    private SmsListener smsListener = new SmsListener() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle b = intent.getExtras();

            String sender = b.getString("sender");
            String message = b.getString("body");
            String slot = b.getString("slot");
            String timestamp = b.getString("timestamp");
            reportMessage(sender, message, slot, timestamp);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the xml file
        setContentView(R.layout.activity_main);

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragmentContainer) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            StatusFragment statusFragment = new StatusFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            statusFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragmentContainer' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, statusFragment).commit();
        }

        registerReceiver(smsListener, new IntentFilter("newSmsReceived"));

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(smsListener);
        Log.e("", "Unregistered");
        super.onDestroy();
    }

    public String getServerAddress() {
        return getString(R.string.default_server);
    }

    private String getDeviceId() {
        // check stored device id
        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String device_id = sharedPreferences.getString("device_id", null);

        // generate id if not exists
        if (device_id == null) {
            device_id = generateDeviceId();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("device_id", device_id);
            editor.apply();
        }

        return device_id;
    }

    public String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    public void onFragmentInteraction(Uri uri) {
        //TODO: do something with uri
    }

    public void getCode(View view) {
        // it is a self-callable function and we don't need view here
        getCode();
    }

    public void getCode() {
        // generate random code
        Random r = new Random();
        final String simple_code = Integer.toString(r.nextInt(10000 - 1000) + 1000);

        final SyncReference codeRef = WilddogSync.getInstance().getReference("pairing_code").child(simple_code);

        HashMap<String, Object> code_obj = new HashMap<>();
        code_obj.put("uuid", getDeviceId());
        code_obj.put("expire", System.currentTimeMillis() / 1000L + 30L);// expire in 30s

        codeRef.setValue(code_obj);
        showCode(simple_code);
    }

    public void showCode(String simple_code) {
        // show simple code if ok
        if (simple_code.length() > 0) {
            toast("Verification Code (valid for 30 seconds):", simple_code);
        }
    }

    public void tryConnect() {
        WilddogOptions options = new WilddogOptions.Builder().setSyncUrl(this.getServerAddress()).build();
        WilddogApp.initializeApp(this, options);

        SyncReference connectedRef = WilddogSync.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (boolean) dataSnapshot.getValue(Boolean.class);
                if (connected) {
                    System.out.println("connected");
                    updateStatus(true);
                } else {
                    System.out.println("not connected");
                    updateStatus(false);
                }
            }

            @Override
            public void onCancelled(SyncError syncError) {
            }
        });
    }

    public void updateLastConnection() {
        // update display
        setLastConnection(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    public void updateStatus(boolean success) {
        if (success) {
            //update display
            setServerStatus("Connected");
            updateLastConnection();
        } else {
            setServerStatus("Error");
        }
    }

    public void setServerStatus(String status) {
        TextView textView = (TextView) findViewById(R.id.server_status_show);
        textView.setText("");
        textView.setText(status);
    }

    public void setLastConnection(String time) {
        TextView textView = (TextView) findViewById(R.id.last_connection_show);
        textView.setText("");
        textView.setText(time);
    }

    public void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void toast(String sender, String toast_text) {
        int duration = Toast.LENGTH_LONG;
        if (sender.length() != 0) {
            toast_text = sender + ": " + toast_text;
        }
        Toast toast = Toast.makeText(this,
                toast_text, duration);
        toast.show();
    }

    public void reportMessage(final String sender, final String body, final String slot, final String timestamp) {
        // display to user
        toast(sender, body);

        /**
         * report to server
         */
        SyncReference reportRef = WilddogSync.getInstance().getReference("message").child(getDeviceId());

        HashMap<String, Object> message = new HashMap<>();
        message.put("message_body", body);
        message.put("sender", sender);
        message.put("slot", slot);
        message.put("timestamp", timestamp);
        reportRef.push().setValue(message);
    }

    public static boolean objEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else return a.equals(b);
    }
}
