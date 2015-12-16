package com.example.metamorph.mysqlsqllitesync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

/**
 * Created by metamorph on 08/12/15.
 */
public class SampleBC extends BroadcastReceiver {
    static int noOfTimes = 0;

    // Method will be called when broadcast is run from MainActivity
    @Override
    public void onReceive(final Context context, Intent intent){
        noOfTimes++;
        Toast.makeText(context, "BC Service Running for " + noOfTimes +" times", Toast.LENGTH_SHORT).show();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        // Check if new records are inserted in Remote MySQL DB to proceed with sync operation
        client.post("http://10.100.44.69:80/android-sync/get_db_row_count.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                System.out.println(response);
                try {
                    // Convert byte to String
                    String s = null;
                    try {
                        s = new String(response,"UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // Create JSON object to response sent by get_db_row_count php
                    JSONObject jsonObject = new JSONObject(s);
                    System.out.println(jsonObject.get("count"));
                    // If the count result is not zero
                    if (jsonObject.getInt("count") != 0){
                        final Intent intentl = new Intent(context, MyService.class);
                        //  Set unsynced count in intent data
                        intentl.putExtra("intentl", "Unsynced rows count " + jsonObject.getInt("count"));
                        // Call MyService
                        context.startService(intentl);
                    }else {
                        Toast.makeText(context, "Sync is not needed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode,  Header[] headers, byte[] errorResponse, Throwable error) {
                if (statusCode == 404){
                    Toast.makeText(context, "404", Toast.LENGTH_SHORT).show();
                }else if (statusCode == 500){
                    Toast.makeText(context, "500", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(context, "Error occured!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
