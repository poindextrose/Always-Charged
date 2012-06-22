package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

class ResetAlarmsReceiver extends BroadcastReceiver {

	/**
	 * If the device is rebooted, application reinstalled, or time zone changed,
	 * set the alarm.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)
					|| (action.equals(Intent.ACTION_PACKAGE_REPLACED)
							&& intent.getDataString() != null && intent.getDataString().contains(
							context.getPackageName()))
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				DatabaseHelper database = new DatabaseHelper(context);
				Cursor cursor = database.getAllActiveAlarms();
				if (cursor != null && cursor.moveToFirst()) {
					cursor.moveToFirst();
					do {
						int _id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ID));
						Alarm alarm = database.getAlarm(_id);
						Scheduler.setDailyAlarm(context, alarm, false);
					} while (cursor.moveToNext());
					cursor.close();
					database.close();
				}
				return;
			}
		}

	}

}
