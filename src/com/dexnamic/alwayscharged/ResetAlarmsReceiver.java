package com.dexnamic.alwayscharged;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class ResetAlarmsReceiver extends BroadcastReceiver {

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
				Scheduler.resetAllEnabledAlarms(context);
				return;
			}
		}

	}

}
