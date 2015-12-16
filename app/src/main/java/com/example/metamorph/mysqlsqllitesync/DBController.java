package com.example.metamorph.mysqlsqllitesync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by metamorph on 08/12/15.
 */
public class DBController extends SQLiteOpenHelper{

    public DBController(Context context){
        super(context, "user.db", null, 1);
    }
    // create table
    @Override
    public void onCreate(SQLiteDatabase database){
        String query;
        query = "CREATE TABLE users (userId INTEGER, userName TEXT)";
        database.execSQL(query);
    }
    @Override
    public void onUpgrade(SQLiteDatabase database, int version_old, int current_version){
        String query;
        query = "DROP TABLE IF EXISTS users";
        database.execSQL(query);
        onCreate(database);
    }

    /**
     *  Insert user into SQLite DB
     *  @param queryValues
     */
    public void insertUser(HashMap<String, String> queryValues){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", queryValues.get("userId"));
        values.put("userName", queryValues.get("userName"));
        database.insert("users", null, values);
        database.close();
    }
    /**
     * Get list of users from SQLite DB as Array List
     * @return
     */
    public ArrayList<HashMap<String, String>> getAllUsers(){
        ArrayList<HashMap<String, String>> userList;
        userList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM users";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()){
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("userId", cursor.getString(0));
                map.put("userName", cursor.getString(1));
                userList.add(map);
            }while (cursor.moveToNext());
        }
        database.close();
        return userList;
    }

}
