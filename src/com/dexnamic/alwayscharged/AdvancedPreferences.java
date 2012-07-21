package com.dexnamic.alwayscharged;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.dexnamic.alwayscharged.billing.BillingService;
import com.dexnamic.alwayscharged.billing.Consts;
import com.dexnamic.alwayscharged.billing.PurchaseObserver;
import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.BillingService.RequestPurchase;
import com.dexnamic.alwayscharged.billing.BillingService.RestoreTransactions;
import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.billing.Consts.ResponseCode;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class AdvancedPreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener
{
	public final static String KEY_SNOOZE_TIME_MIN = "key_snooze";
	public final static String KEY_DURATION = "key_alarm_duration";
	public final static String KEY_MOTION_TOLERANCE = "key_motion_tolerance";
	

	static final int UPGRADE_NEEDED_FOR_ADVANCED_DIALOG = 5;
	protected static boolean mRequestedPurchase;

	private ListPreference mListPreferenceSnooze;
	private ListPreference mListPreferenceDuration;
	private ListPreference mListPreferenceMotion;
	private ListPreference mListPreferenceBattery;
	private Boolean mHasPurchased;
	private Boolean mSnoozeWasChanged;
	private BillingService mBillingService;
	private Handler mHandler;
	private UpgradePurchaseObserver mUpgradePurchaseObserver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.advanced_preferences);

		configurePreferences();

		mBillingService = new BillingService();
		mBillingService.setContext(this);

		setVolumeControlStream(AudioManager.STREAM_ALARM);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mSnoozeWasChanged = false;

		mHandler = new Handler();
		mUpgradePurchaseObserver = new UpgradePurchaseObserver(mHandler);
		ResponseHandler.register(mUpgradePurchaseObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		mHasPurchased = ResponseHandler.hasPurchased(this);
		if (mHasPurchased) {
			upgradeToPro();
		} else {
			if(!mRequestedPurchase)
				showDialog(UPGRADE_NEEDED_FOR_ADVANCED_DIALOG);
		}
		mRequestedPurchase = false;
	}

	private void upgradeToPro() {
		mListPreferenceDuration.setEnabled(true);
		mListPreferenceMotion.setEnabled(true);
		mListPreferenceSnooze.setEnabled(true);
		mListPreferenceBattery.setEnabled(true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);

		ResponseHandler.unregister(mUpgradePurchaseObserver);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if(mSnoozeWasChanged)
			Scheduler.resetAllEnabledAlarms(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBillingService.unbind();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;
		switch (id) {
		case UPGRADE_NEEDED_FOR_ADVANCED_DIALOG:
			return createUpgradeDialog(R.string.advanced_upgrade_dialog);
		}
		return null;
	}
	
	AlertDialog createUpgradeDialog(int resid) {
		AlertDialog.Builder upGradebuilder = new AlertDialog.Builder(this);
		upGradebuilder
				.setMessage(getString(resid))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// startActivity(new
								// Intent(ListAlarmsActivity.this,
								// UpgradeProActivity.class));
								mBillingService.requestPurchase(Consts.mProductID,
										Consts.ITEM_TYPE_INAPP, null);
								AdvancedPreferences.mRequestedPurchase = true;
							}
						})
				.setNegativeButton(getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return upGradebuilder.create();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// Toast.makeText(MainActivity.this, (String) newValue,
		// Toast.LENGTH_LONG).show();
		// Log.d("dexnamic", "newValue=" + (String)newValue);
		if (preference == mListPreferenceSnooze) {
			String minutes = AdvancedPreferences.this.getString(R.string.minutes);
			mListPreferenceSnooze.setSummary((String) newValue + " " + minutes);
			/* we need to update all alarms when snooze time is changed
			 * but the preference hasn't changed until after this method
			 * completes so lets flag it so we reset the alarms in onStop()
			 */
			mSnoozeWasChanged = true;
		} else if (preference == mListPreferenceDuration) {
			String seconds = AdvancedPreferences.this.getString(R.string.seconds);
			mListPreferenceDuration.setSummary((String) newValue + " " + seconds);
		} else if (preference == mListPreferenceMotion) {
			setMotionToleranceSummary((String) newValue);
		} else if (preference == mListPreferenceBattery) {
			setSkipBatterySummary((String) newValue);
		}
		return true;
	}

	private void setMotionToleranceSummary(String newSummary) {
		int motionTolerance = Integer.parseInt(newSummary);
		String summary = getString(R.string.disabled);
		if (motionTolerance > 0) {
			String degrees = AdvancedPreferences.this.getString(R.string.degrees);
			summary = (String) newSummary + " " + degrees;
		}
		mListPreferenceMotion.setSummary(summary);
	}

	private void setSkipBatterySummary(String value) {
		String[] entries = getResources().getStringArray(R.array.cancel_battery_entries);
		String[] entryValues = getResources().getStringArray(R.array.cancel_battery_entryValues);
		for (int i = 0; i < entries.length; i++) {
			if (entryValues[i].contentEquals(value)) {
				mListPreferenceBattery.setSummary(entries[i]);
				return;
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		try {
			// try to backup preferences to Google if supported on this device
			Class<?> _BackupManager = Class.forName("android.app.backup.BackupManager");
			Constructor<?> constructor = _BackupManager
					.getConstructor(new Class[] { Context.class });
			// reflection: BackupManager bm = new BackupManager(this);
			Object backupManager = constructor.newInstance(this);
			Method dataChanged = _BackupManager.getMethod("dataChanged", (Class[]) null);
			// reflection: backupManager.dataChanged();
			dataChanged.invoke(backupManager, (Object[]) null);
		} catch (Exception e) {
		}
	}

	private void configurePreferences() {
	
		PreferenceScreen ps = getPreferenceScreen();
		SharedPreferences settings = ps.getSharedPreferences();
		mListPreferenceSnooze = (ListPreference) ps
				.findPreference(KEY_SNOOZE_TIME_MIN);
		mListPreferenceSnooze.setSummary(settings.getString(
				KEY_SNOOZE_TIME_MIN, "***")
				+ " "
				+ getString(R.string.minutes));
		mListPreferenceSnooze.setOnPreferenceChangeListener(this);
	
		mListPreferenceDuration = (ListPreference) ps
				.findPreference(KEY_DURATION);
		mListPreferenceDuration.setSummary(settings.getString(KEY_DURATION,
				"***") + " " + getString(R.string.seconds));
		mListPreferenceDuration.setOnPreferenceChangeListener(this);
	
		mListPreferenceMotion = (ListPreference) ps
				.findPreference(KEY_MOTION_TOLERANCE);
		setMotionToleranceSummary(settings.getString(KEY_MOTION_TOLERANCE,
				"***"));
		mListPreferenceMotion.setOnPreferenceChangeListener(this);
	
		String key = getString(R.string.key_skip_battery);
		mListPreferenceBattery = (ListPreference) ps.findPreference(key);
		String defaultBattery = getResources().getStringArray(
				R.array.cancel_battery_entries)[0];
		setSkipBatterySummary(settings.getString(key, defaultBattery));
		mListPreferenceBattery.setOnPreferenceChangeListener(this);
	}

	// @Override
	// public boolean onPreferenceClick(Preference preference) {
	//
	// if(preference == mTestPref) {
	// Intent intent = new Intent(this, AlarmDetailPreferenceActivity.class);
	// startActivity(intent);
	// }
	// return false;
	// }

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	private class UpgradePurchaseObserver extends PurchaseObserver {
		public UpgradePurchaseObserver(Handler handler) {
			super(AdvancedPreferences.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState, String itemId, int quantity,
				long purchaseTime, String developerPayload) {

			if (purchaseState == PurchaseState.PURCHASED) {
				upgradeToPro();
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver", request.mProductId + ": " + responseCode);
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver", "onRestoreTranscationReponse() responseCode="
					+ responseCode);
		}
	}

}
