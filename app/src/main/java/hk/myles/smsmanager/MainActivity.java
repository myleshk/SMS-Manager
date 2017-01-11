package hk.myles.smsmanager;

import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends FragmentActivity
        implements OnFragmentInteractionListener {

    private EditText serverAddressEditText;

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
            ServerConfigFragment serverConfigFragment = new ServerConfigFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            serverConfigFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragmentContainer' FrameLayout

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, serverConfigFragment).commit();
        }
        /*RequestQueue mRequestQueue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);
        // Start the queue
        mRequestQueue.start();*/
    }

    public void onFragmentInteraction(Uri uri) {
        //TODO: do something with uri
    }

    /**
     * Called when user clicks Validate button
     **/
    public void validateServer(View view) {
        serverAddressEditText = (EditText) findViewById(R.id.serverAddressEditText); //check this point carefully on your program

        String server_address = serverAddressEditText.getText().toString();

        server_address = "https://apps.myles.hk/sms_manager/";
        doValidate(server_address);
    }

    public void doValidate(final String server_address) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

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
                        // Create new fragment and transaction
                        Fragment statusFragment = new StatusFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("address", server_address);
                        statusFragment.setArguments(bundle);

                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                        // Replace whatever is in the fragment_container view with this fragment,
                        // and add the transaction to the back stack if needed
                        transaction.replace(R.id.fragmentContainer, statusFragment).commit();
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
        requestQueue.add(stringRequest);

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
}
