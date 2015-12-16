package com.example.metamorph.mysqlsqllitesync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    // DB Clas to perform DB related operations
    DBController controller = new DBController(this);
    // Progress dialog object
    ProgressDialog progressDialog;
    HashMap<String, String> queryValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get user records from SQLite DB
        ArrayList<HashMap<String, String>> userList = controller.getAllUsers();
        // If users exists in SQLite DB
        if (userList.size() != 0){
            // Set the user array list in ListView
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, userList, R.layout.view_user_entry, new String[]{
                "userId", "userName" }, new int[]{ R.id.userId, R.id.userName});
            ListView myList = (ListView) findViewById(android.R.id.list);
            myList.setAdapter(adapter);
        }
        // Initialize progress dialog properties
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Transferring data from remote MySQL DB and syncing SQLite.  Please wait!");
        progressDialog.setCancelable(false);
        // Broadcast receiver intent object
        Intent alarmIntent = new Intent(getApplicationContext(), SampleBC.class);
        // Pending intent object
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        // Alarm Manager calls broadcast for every 10 seconds (10 * 1000), broadcast further call service to check if new recored are inserted in Remote MySQL DB
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 5000, 10 * 1000, pendingIntent);
        // Prevent wifi sleep
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        wifiLock.acquire();
    }
    // Options menu (action bar menu)
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu, this item will be shown to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // When option menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item, click here
        int id = item.getItemId();
        // When sync action button is clicked
        if (id == R.id.refresh){
            // Transfer data from remote MySQL DB to SQLite on Android and perform sync
            syncSQLiteMySQLDB();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Method to sync MySQL to SQLite DB
    public void syncSQLiteMySQLDB(){
        // Create AsyncHttpClient object
        AsyncHttpClient client = new AsyncHttpClient();
        // Http request params object
        RequestParams requestParams = new RequestParams();
        // Show progress bar
        progressDialog.show();
        // Make http call to get users from php
        client.post("http://10.100.44.69:80/android-sync/get_users.php", requestParams, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // Hide progress bar
                progressDialog.hide();
                // Update SQLite DB with response sent by get users php
                updateSQLite(response);
            }

            @Override
            public void onFailure(int statusCode,  Header[] headers, byte[] errorResponse, Throwable  error) {
                // Hide progress bar
                progressDialog.hide();
                if (statusCode == 404){
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                } else if (statusCode == 500) {
                    Toast.makeText(getApplicationContext(), "Something may be wrong at server", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Unexpected error occured!  Most common error: device might not be connected to the Internet", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

//    public  void updateSQLite(byte[] response){
    public  void updateSQLite(byte[] response){
        String string = null;
        try {
            string = new String(response, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ArrayList<HashMap<String, String>> userSyncList;
        userSyncList = new ArrayList<HashMap<String, String>>();
        // Create GSON object
        Gson gson = new GsonBuilder().create();
        try{
            // Extract JSON array from the response
            JSONArray jsonArray = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                jsonArray = new JSONArray(string);
            }
            System.out.println(jsonArray.length());
            // if array elements are not zero
            if (jsonArray.length() != 0){
                // Loop through each array element, get JSON object which has userid and username
                for (int i = 0; i < jsonArray.length(); i++){
                    // Get JSON object
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    System.out.println(jsonObject.get("userId"));
                    System.out.println(jsonObject.get("userName"));
                    // DB QueryValues Object to insert into SQLite
                    queryValues = new HashMap<String, String>();
                    // Add userID extracted from object
                    queryValues.put("userId", jsonObject.get("userId").toString());
                    // Add userName extracted from object
                    queryValues.put("userName", jsonObject.get("userName").toString());
                    // Insert user into SQLite DB
                    controller.insertUser(queryValues);
                    HashMap<String, String> map = new HashMap<String, String>();
                    // Add status for each user in hashmap
                    map.put("Id", jsonObject.get("userId").toString());
                    map.put("Status", "1");
                    userSyncList.add(map);
                }
                // Inform remote MySQL DB about the completion of sync activity
                updateMySQLSyncSts(gson.toJson(userSyncList));
                // Reload main activity
                reloadActivity();
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    // Method to inform remote MySQL DB about completion of sync activity
    public  void updateMySQLSyncSts(String json){
        System.out.println(json);
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("syncsts", json);
        // Make http call to updatesyncsts.php with JSON paramater
        asyncHttpClient.post("http://10.100.44.69:80/android-sync/update_sync_status.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Toast.makeText(getApplicationContext(), "MySQL DB has been informed about sync activity", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int statusCode,  Header[] headers, byte[] errorResponse, Throwable error) {
                Toast.makeText(getApplicationContext(), "Error occured", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Reload main activity
    public void reloadActivity(){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

}

