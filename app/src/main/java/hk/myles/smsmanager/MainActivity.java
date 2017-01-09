package hk.myles.smsmanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    EditText serverAddressEditText;
    public static String EXTRA_MESSAGE = "hk.myles.smsmanager.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverAddressEditText = (EditText) findViewById(R.id.serverAddressEditText); //check this point carefully on your program
    }

    /**
     * Called when user clicks Validate button
     **/
    public void validateServer(View view) {

        String server_address = serverAddressEditText.getText().toString();


        // if the address is good
        /*Intent intent = new Intent(this, DisplayMessageActivity.class);

        intent.putExtra(EXTRA_MESSAGE, server_address);
        startActivity(intent);*/

        server_address = "https://apps.myles.hk/sms_manager/";
        doValidate(server_address);
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

    public void doValidate(final String server_address) {
        RequestQueue mRequestQueue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);
        // Start the queue
        mRequestQueue.start();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, server_address, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    //Creating JsonObject from response String
                    JSONObject jsonObject = new JSONObject(response);
                    //extracting json array from response string
                    //get value from jsonRow
                    String resultStr = jsonObject.getString("success");

                    if (Objects.equals(resultStr, "true")) {
                        // TODO: save server address
                        navigateToMessageView(server_address);
                    } else {
                        alert("Validation failed. Try again.");
                    }
                } catch (JSONException e) {
                    alert("Error. Message:" + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("data", server_address);

                return parameters;
            }

        };
        mRequestQueue.add(stringRequest);

    }

    public void navigateToMessageView(String server_address) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        intent.putExtra(EXTRA_MESSAGE, server_address);
        startActivity(intent);
    }
}
