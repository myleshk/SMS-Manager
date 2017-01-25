package hk.myles.smsmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
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
        // get stored server address
        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPreferences.getString("server_address", getString(R.string.default_server));
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

    public void navigateToConfig(View view) {
        ServerConfigFragment serverConfigFragment = new ServerConfigFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.fragmentContainer, serverConfigFragment, "CONFIG_FRAGMENT").commit();
    }

    public void showCode(View view) {
        final String server_address = getServerAddress();
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, server_address,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Creating JsonObject from response String
                        JSONObject jsonObject = null;
                        Log.e("server_response", response);
                        try {
                            jsonObject = new JSONObject(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //extracting json array from response string
                        //get value from jsonRow
                        String resultStr = null;
                        String simple_id;
                        try {
                            resultStr = jsonObject.getString("success");
                            simple_id = jsonObject.getString("simple_id");

                            // show simple code if ok
                            if (simple_id.length() > 0) {
                                toast("Verification Code", simple_id);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // finished parsing JSON response
                        updateStatus(server_address, objEquals(resultStr, "true"));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateStatus(server_address, false);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("action", "get_simple_id");
                parameters.put("uuid", getDeviceId());

                return parameters;
            }
        };

        requestQueue.add(stringRequest);
    }

    /**
     * Called when user clicks Validate button
     **/
    public void validateServer(View view) {
        EditText serverAddressEditText = (EditText) findViewById(R.id.serverAddressEditText);

        String server_address = serverAddressEditText.getText().toString();

        if (!validateURL(server_address)) {
            alert("Incorrect URL! Please check");
            return;
        }

        // Create new fragment and transaction
        Fragment statusFragment = new StatusFragment();
        Bundle bundle = new Bundle();
        bundle.putString("address", server_address);
        statusFragment.setArguments(bundle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.fragmentContainer, statusFragment, "STATUS_FRAGMENT").commit();
    }

    public boolean validateURL(String server_address) {
        //Invalid
        return Patterns.WEB_URL.matcher(server_address).matches();
    }

    public void tryConnect(final String server_address) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, server_address,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Creating JsonObject from response String
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //extracting json array from response string
                        //get value from jsonRow
                        String resultStr = null;
                        try {
                            resultStr = jsonObject.getString("success");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // finished parsing JSON response
                        updateStatus(server_address, objEquals(resultStr, "true"));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateStatus(server_address, false);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("action", "connect");

                return parameters;
            }
        };

        requestQueue.add(stringRequest);
    }

    public void updateStatus(String server_address, boolean success) {
        // update display
        setServerAddress(server_address);
        setLastConnection(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));

        if (success) {
            // save data
            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("server_address", server_address);
            editor.apply();

            //update display
            setServerStatus("Connected");
        } else {
            setServerStatus("Error");
        }

    }

    public void setServerAddress(String address) {
        TextView textView = (TextView) findViewById(R.id.server_address_show);
        textView.setText("");
        textView.setText(address);
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

    public void alert(String alert_text) {
        new AlertDialog.Builder(this)
                .setTitle("Server Address")
                .setMessage(alert_text)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
        final String server_address = getServerAddress();
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JSONObject reportJson = new JSONObject();
        String reportString = "";
        try {
            reportJson.put("sender", sender);
            reportJson.put("body", body);
            reportJson.put("slot", slot);
            reportJson.put("timestamp", timestamp);
            reportJson.put("uuid", getDeviceId());
            reportString = reportJson.toString();
            Log.e("reportStr", reportString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String finalReportString = reportString;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, server_address,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Creating JsonObject from response String
                        JSONObject jsonObject = null;
                        Log.e("server_response", response);
                        try {
                            jsonObject = new JSONObject(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //extracting json array from response string
                        //get value from jsonRow
                        String resultStr = null;
                        try {
                            resultStr = jsonObject.getString("success");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // finished parsing JSON response
                        updateStatus(server_address, objEquals(resultStr, "true"));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        updateStatus(server_address, false);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("action", "report");
                parameters.put("data", finalReportString);

                return parameters;
            }
        };
        requestQueue.add(stringRequest);
    }

    public static boolean objEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else return a.equals(b);
    }
}
