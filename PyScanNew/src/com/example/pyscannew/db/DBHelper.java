package com.example.pyscannew.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.widget.Toast;

import com.example.pyscannew.MainActivity;
import com.example.pyscannew.model.Data;


public class DBHelper {
	private static final String TAG = "Database";
	private static final String DATABASE_NAME = "pyconindia.db";
	private static final int DATABASE_VERSION = 1;

	private static final String TABLE_DATA = "data";
	private Context context;

	private SQLiteDatabase db;

	public DBHelper(Context context) {
		CustomSQLiteOpenHelper helper = new CustomSQLiteOpenHelper(context);
		this.db = helper.getWritableDatabase();
		this.context = context;
	}

	public void close() {
		if(db.isOpen()) {
			db.close();
		}
	}

	/**
	 * Delete All records from data table
	 */
	public void dropAllData() {
		db.delete(TABLE_DATA, null, null);
	}


	/**
	 * inserts the list of data into data table
	 * @param dataList Array of data
	 * @return success or failure boolean value
	 */
	public boolean insertDataFromList(List<Data> dataList) {
		boolean val = true;
		try {
			String sql = "INSERT INTO " + TABLE_DATA + " (" +
		    		Data.VALUE+
		    		") VALUES (?)";
			db.beginTransaction();
		    SQLiteStatement insert = db.compileStatement(sql);

			for(int i = 0; i < dataList.size(); i++) {

		    	insert.bindString(1, dataList.get(i).getValue());

		        insert.execute();
				
			}

			db.setTransactionSuccessful();
		} catch(Exception e) {
			val = false;
			Log.e(TAG, e.getMessage());
		} finally {
			db.endTransaction();
		}
		return val;
	}
	
	/**
	 * inserts the data into data table
	 * @param data Data object to insert
	 * @return success or failure boolean value
	 */
	public boolean insertData(Data data) {
		ArrayList<Data> dataList = new ArrayList<Data>();
		dataList.add(data);
		return insertDataFromList(dataList);
	}

	public ArrayList<Data> getData() {
	    ArrayList<Data> dataList = new ArrayList<Data>();

	    try {
	        Cursor cursor = db.query(TABLE_DATA, null, null, null, null, null, null, null);
	        cursor.moveToFirst();
	        while (!cursor.isAfterLast()) {
	        	Data data = cursorToData(cursor);
	            dataList.add(data);
	            cursor.moveToNext();
	        }
	        cursor.close();
	    } catch (Exception e) {
	    	Log.e(TAG, e.getMessage());
	    	Toast.makeText(context, "Snap!! Something Broke!!\n"+e.getMessage(), Toast.LENGTH_LONG).show();
	    }

	    return dataList;
	}

	private Data cursorToData(Cursor cursor) {
		Data data  = new Data();
		data.setId(cursor.getInt(cursor.getColumnIndex(Data.ID)));
		data.setValue(cursor.getString(cursor.getColumnIndex(Data.VALUE)));
        return data;
    }

	private class CustomSQLiteOpenHelper extends SQLiteOpenHelper {

		private String mCreateTable;

	    public CustomSQLiteOpenHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);

	        mCreateTable = "CREATE TABLE " + TABLE_DATA + " ( " +
	        		Data.ID + " INTEGER PRIMARY KEY NOT NULL," +
	        		Data.VALUE + " TEXT)";
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL(mCreateTable);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			switch (oldVersion) {
			case 1:
				String mCreateTable = "CREATE TABLE " + TABLE_DATA + " ( " + Data.ID + 
										" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + Data.VALUE + " TEXT," + " )";
				Log.i(TAG, "Upgrading Database: " + mCreateTable);
				db.execSQL(mCreateTable);

			}
		}
	}
};