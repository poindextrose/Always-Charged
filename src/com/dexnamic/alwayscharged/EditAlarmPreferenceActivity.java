package com.dexnamic.alwayscharged;

import java.util.ArrayList;

import com.dexnamic.alwayscharged.billing.BillingService;
import com.dexnamic.alwayscharged.billing.Consts;
import com.dexnamic.alwayscharged.billing.PurchaseObserver;
import com.dexnamic.alwayscharged.billing.ResponseHandler;
import com.dexnamic.alwayscharged.billing.BillingService.RequestPurchase;
import com.dexnamic.alwayscharged.billing.BillingService.RestoreTransactions;
import com.dexnamic.alwayscharged.billing.Consts.PurchaseState;
import com.dexnamic.alwayscharged.billing.Consts.ResponseCode;
import com.dexnamic.android.preference.ListPreferenceMultiSelect;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class EditAlarmPreferenceActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener, OnClickListener,
		TimePickerDialog.OnTimeSetListener {

	static final int TIME_DIALOG_ID = 0;
	static final int UPGRADE_NEEDED_TO_SET_REPEAT_DIALOG = 1;
	private static final int REPEAT_MULTISELECT_DIALOG = 2;

	private int mId;

	private Alarm mAlarm;
	// private Alarm mAlarmOriginal;
	private DatabaseHelper database;

	private CheckBoxPreference mEnabledCheckBox;
	private Preference mTimePreference;
	private Preference mRepeatPreference;
	private CheckBoxPreference mVibrateCheckBox;
	private Button cancelButton, deleteButton, okButton;

	private RingtonePreference mRingtonePreference;
	private EditTextPreference mLabelPreference;

	private Vibrator mVibrator;

	private BillingService mBillingService;

	private UpgradePurchaseObserver mUpgradePurchaseObserver;

	private Handler mHandler;

	boolean[] mCheckedItems;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.alarm_detail);
		addPreferencesFromResource(R.xml.alarm_detail);

		Bundle extras = getIntent().getExtras();
		mId = extras.getInt("id");

		PreferenceScreen ps = getPreferenceScreen();

		mEnabledCheckBox = (CheckBoxPreference) ps
				.findPreference("key_checkbox");
		mEnabledCheckBox.setOnPreferenceClickListener(this);

		mTimePreference = ps.findPreference("key_time");
		mTimePreference.setOnPreferenceClickListener(this);

		mRepeatPreference = ps.findPreference("key_repeat");
		mRepeatPreference.setOnPreferenceClickListener(this);

		mRingtonePreference = (RingtonePreference) ps
				.findPreference("key_ringtone");
		mRingtonePreference.setOnPreferenceChangeListener(this);

		mLabelPreference = (EditTextPreference) ps.findPreference("key_label");
		mLabelPreference.setOnPreferenceChangeListener(this);

		mVibrateCheckBox = (CheckBoxPreference) ps
				.findPreference("key_vibrate");
		mVibrateCheckBox.setOnPreferenceClickListener(this);
		mVibrateCheckBox.setOnPreferenceChangeListener(this);

		cancelButton = (Button) findViewById(R.id.buttonCancel);
		cancelButton.setOnClickListener(this);
		deleteButton = (Button) findViewById(R.id.buttonDelete);
		deleteButton.setOnClickListener(this);
		okButton = (Button) findViewById(R.id.buttonOK);
		okButton.setOnClickListener(this);

		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ViewGroup viewGroup = (ViewGroup) cancelButton.getParent();
			viewGroup.removeView(cancelButton);
			viewGroup.removeView(deleteButton);
			viewGroup.removeView(okButton);
		}

		if (ResponseHandler.hasPurchased(this) == false) {
			mBillingService = new BillingService();
			mBillingService.setContext(this);
		}

		setVolumeControlStream(AudioManager.STREAM_ALARM);

		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		mHandler = new Handler();
		mUpgradePurchaseObserver = new UpgradePurchaseObserver(mHandler);
		ResponseHandler.register(mUpgradePurchaseObserver);

		mCheckedItems = new boolean[7];

		setupActionBar_API11();
		setupActionBar_API14();
	}

	TextView mTextView;

	@TargetApi(11)
	void setupActionBar_API11() {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
			// mTextView = new TextView(this);
			// mTextView.setText("DONE");
			// mTextView.setClickable(true);
			// mTextView.setFocusable(true);
			// mTextView.setOnClickListener(this);
			// actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
			// actionBar.setCustomView(mTextView);
		}
	}

	@TargetApi(14)
	void setupActionBar_API14() {
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			ActionBar actionBar = getActionBar();
			actionBar.setHomeButtonEnabled(true);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		database = new DatabaseHelper(this);

		if (mId >= 0)
			mAlarm = database.getAlarm(mId);
		else
			mAlarm = new Alarm();
		// mAlarmOriginal = (Alarm) mAlarm.clone();
	}

	@Override
	protected void onResume() {
		super.onResume();

		setPreferences();
	}

	@Override
	protected void onPause() {
		super.onPause();

		ResponseHandler.unregister(mUpgradePurchaseObserver);
		if (database != null)
			database.close();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mBillingService != null)
			mBillingService.unbind();
	}

	void upgradeToPro() {


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			saveAlarm();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			deleteAlarm();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private void setPreferences() {

		mEnabledCheckBox.setChecked(mAlarm.getEnabled());

		mTimePreference.setSummary(mAlarm.getTime(this));

		mRepeatPreference.setSummary(Alarm.repeatToString(this,
				mAlarm.getRepeats()));
		StringBuffer checked = new StringBuffer();
		// List<Integer> checkedIndicies = new ArrayList<Integer>();
		for (Integer i = 0; i < 7; i++) {
			if (mAlarm.getRepeats(i))
				checked.append(i.toString() + ",");
		}
		// mRepeatPreferenceMultiSelect.setValue(checked.toString());

		mRingtonePreference.setSummary(mAlarm.getRingerName(this));
		getPreferenceManager().getSharedPreferences().edit()
				.putString("key_ringtone", mAlarm.getRingtone()).commit();

		mVibrateCheckBox.setChecked(mAlarm.getVibrate());

		mLabelPreference.setSummary(mAlarm.getLabel());
		mLabelPreference.setText(mAlarm.getLabel());

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		if (preference == mEnabledCheckBox) {
			mAlarm.setEnabled(mEnabledCheckBox.isChecked());
		} else if (preference == mTimePreference) {
			showDialog(TIME_DIALOG_ID);
		} else if (preference == mVibrateCheckBox) {
			if (mVibrateCheckBox.isChecked()) {
				mVibrator.vibrate(500);
			}
			mAlarm.setVibrate(mVibrateCheckBox.isChecked());
		} else if (preference == mRepeatPreference) {
			if (ResponseHandler.hasPurchased(this) == false) {
				showDialog(UPGRADE_NEEDED_TO_SET_REPEAT_DIALOG);
			} else {
				// if(mRepeatPreferenceMultiSelect != null)
				// mRepeatPreferenceMultiSelect.showDialog();
				for (int i = 0; i < 7; i++)
					mCheckedItems[i] = mAlarm.getRepeats(i);
				showDialog(REPEAT_MULTISELECT_DIALOG);
			}
		}

		return true;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		enableAlarm();

		if (preference == mRingtonePreference) {
			mAlarm.setRingtone((String) newValue);
			mRingtonePreference.setSummary(mAlarm.getRingerName(this));
			checkVolume();
			return true;
		} else if (preference == mLabelPreference) {
			mAlarm.setLabel((String) newValue);
			mLabelPreference.setSummary((String) newValue);
			return true;
		}
		return true;
	}

	private void checkVolume() {
		String chosenRingtone = mAlarm.getRingtone();
		if (chosenRingtone.length() > 0) {
			AudioManager audioManager;
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
				Toast.makeText(this, getString(R.string.checkVolume),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void cancelEdit() {
		finish();
	}

	private void deleteAlarm() {
		if (mId >= 0)
			database.deleteAlarm(mAlarm);
		finish();
	}

	private void saveAlarm() {
		if (mId >= 0) {
			database.updateAlarm(mAlarm);
		} else
			database.addAlarm(mAlarm);
		finish();
	}

	@Override
	public void onClick(View view) {
		if (view == cancelButton) {
			cancelEdit();
		} else if (view == deleteButton) {
			deleteAlarm();
		} else if (view == okButton) {
			saveAlarm();
		} else if (mTextView != null && view == mTextView) {
			saveAlarm();
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			// Take care of calling this method on earlier versions of
			// the platform where it doesn't exist.
			onBackPressed();
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		// This will be called either automatically for you on 2.0
		// or later, or by the code above on earlier versions of the
		// platform.

		// if alarm is not new and nothing change then no need to save again
		// if(mId >= 0 && mAlarm.equals(mAlarmOriginal))
		// finish();
		saveAlarm();
		return;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mAlarm.setHour(hourOfDay);
		mAlarm.setMinute(minute);
		mTimePreference.setSummary(mAlarm.getTime(this));
		enableAlarm();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, this, mAlarm.getHour(),
					mAlarm.getMinute(), false);
		case UPGRADE_NEEDED_TO_SET_REPEAT_DIALOG:
			AlertDialog.Builder upGradebuilder = new AlertDialog.Builder(this);
			upGradebuilder
					.setMessage(getString(R.string.change_repeat_dialog))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// startActivity(new
									// Intent(ListAlarmsActivity.this,
									// UpgradeProActivity.class));
									mBillingService.requestPurchase(
											Consts.mProductID,
											Consts.ITEM_TYPE_INAPP, null);
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			return upGradebuilder.create();
		case REPEAT_MULTISELECT_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMultiChoiceItems(R.array.pref_days_of_week,
					mCheckedItems, new OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which,
								boolean isChecked) {
							mCheckedItems[which] = isChecked;
						}
					})
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									for (int i = 0; i < 7; i++)
										mAlarm.setRepeats(i, mCheckedItems[i]);
									mRepeatPreference.setSummary(Alarm
											.repeatToString(
													EditAlarmPreferenceActivity.this,
													mAlarm.getRepeats()));
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									for (int i = 0; i < 7; i++) {
										mCheckedItems[i] = mAlarm.getRepeats(i);
										((AlertDialog) dialog).getListView().setItemChecked(i, mAlarm.getRepeats(i));
									}
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = builder.create();
			return alertDialog;
		}
		return null;
	}

	private void enableAlarm() {
		/* if the user changes anything then enable the alarm */
		mAlarm.setEnabled(true);
		mEnabledCheckBox.setChecked(mAlarm.getEnabled());
	}

	/**
	 * A {@link PurchaseObserver} is used to get callbacks when Android Market
	 * sends messages to this application so that we can update the UI.
	 */
	private class UpgradePurchaseObserver extends PurchaseObserver {
		public UpgradePurchaseObserver(Handler handler) {
			super(EditAlarmPreferenceActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {

			if (purchaseState == PurchaseState.PURCHASED) {
				upgradeToPro();
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver", request.mProductId + ": "
					+ responseCode);
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			Log.v("UpgradePurchaseObserver",
					"onRestoreTranscationReponse() responseCode="
							+ responseCode);
		}
	}
}
