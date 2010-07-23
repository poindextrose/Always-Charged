package com.dexnamic.alwayscharged;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class MyPrefsBackupAgent extends BackupAgentHelper {
	
	static final String PREFS = "com.dexnamic.alwayscharged_preferences";
    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";
    
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
