package com.dexnamic.alwayscharged;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	/* sqlite3 /data/data/com.dexnamic.alwayscharged/databases/alwayscharged.db */

	private static final String DATABASE_NAME = "alwayscharged.db";
	private static final int DATABASE_VERSION = 7;

	private static final String TABLE_ALARMS = "alarms";

	// Contacts Table Columns names
	static final String KEY_ID = "_id";
	static final String KEY_ENABLED = "enabled";
	static final String KEY_LABEL = "label";
	static final String KEY_HOUR = "hour";
	static final String KEY_MINUTE = "minute";
	static final String KEY_REPEATS = "repeats";
	static final String KEY_RINGTONE = "ringtone";
	static final String KEY_VIBRATE = "vibrate";

	String CREATE_TABLE_ALARMS = "CREATE TABLE " + TABLE_ALARMS + "(" + KEY_ID
			+ " INTEGER PRIMARY KEY," + KEY_ENABLED + " INTEGER," + KEY_LABEL + " TEXT," + KEY_HOUR
			+ " INTEGER," + KEY_MINUTE + " INTEGER," + KEY_REPEATS + " INTEGER," + KEY_RINGTONE
			+ " TEXT," + KEY_VIBRATE + " INTEGER" + ")";

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_ALARMS);

		Alarm alarm = new Alarm();
		// TODO: read from preferences for first time
		alarm.setEnabled(false);
		alarm.setLabel("my first alarm");
		alarm.setHour(21);
		alarm.setMinute(30);
		alarm.setRepeats(3); 
		alarm.setRingtone("Default");
		alarm.setVibrate(true);

		db.insert(TABLE_ALARMS, null, putValues(alarm));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);

		// Create tables again
		onCreate(db);
	}

	public void addAlarm(Alarm alarm) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.insert(TABLE_ALARMS, null, putValues(alarm));
		db.close();
	}

	public int updateAlarm(Alarm alarm) {
		SQLiteDatabase db = this.getWritableDatabase();
		return db.update(TABLE_ALARMS, putValues(alarm), KEY_ID + " = ?",
				new String[] { String.valueOf(alarm.getID()) });
	}

	private ContentValues putValues(Alarm alarm) {
		ContentValues values = new ContentValues();
		values.put(KEY_ENABLED, alarm.getEnabled());
		values.put(KEY_LABEL, alarm.getLabel());
		values.put(KEY_HOUR, alarm.getHour());
		values.put(KEY_MINUTE, alarm.getMinute());
		values.put(KEY_REPEATS, alarm.getRepeats());
		values.put(KEY_RINGTONE, alarm.getRingtone());
		values.put(KEY_VIBRATE, alarm.getVibrate());
		return values;
	}

	public Alarm getAlarm(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_ALARMS, null, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();

		Alarm alarm = new Alarm();
		alarm.setID(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
		alarm.setEnabled(cursor.getInt(cursor.getColumnIndex(KEY_ENABLED))==1);
		alarm.setLabel(cursor.getString(cursor.getColumnIndex(KEY_LABEL)));
		alarm.setHour(cursor.getInt(cursor.getColumnIndex(KEY_HOUR)));
		alarm.setMinute(cursor.getInt(cursor.getColumnIndex(KEY_MINUTE)));
		alarm.setRepeats(cursor.getInt(cursor.getColumnIndex(KEY_REPEATS)));
		alarm.setRingtone(cursor.getString(cursor.getColumnIndex(KEY_RINGTONE)));
		alarm.setVibrate(cursor.getInt(cursor.getColumnIndex(KEY_VIBRATE))==1);
		return alarm;
	}

	public void deleteAlarm(Alarm alarm) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_ALARMS, KEY_ID + " = ?", new String[] { String.valueOf(alarm.getID()) });
		db.close();
	}

	public Cursor getAllAlarms() {
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_ALARMS;
		SQLiteDatabase db = this.getWritableDatabase();
		return db.rawQuery(selectQuery, null);
	}
}
