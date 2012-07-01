package com.dexnamic.alwayscharged;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public class AlwaysCharged extends Application {

	// private static Context context;

	public static boolean isDebuggable;

	// public static final boolean DEBUG = false;

	public void onCreate() {
		super.onCreate();
		// AlwaysCharged.context = getApplicationContext();

		isDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		if (isDebuggable)
			Log.d("AlwaysCharged", "isDebuggabled=" + isDebuggable);
	}

	// public static Context getAppContext() {
	// return AlwaysCharged.context;
	// }

}
