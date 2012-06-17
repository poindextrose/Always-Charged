package com.dexnamic.alwayscharged;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AlarmListCursorAdaptor extends SimpleCursorAdapter {

	interface OnListClickListener {
		abstract void alarmChecked(int id, boolean isChecked);
		abstract void alarmSelected(int id);
	}
	
	private OnListClickListener onListClickListener;
	
	public AlarmListCursorAdaptor(Context context, int layout, Cursor c, OnListClickListener onListClickListener) {
		super(context, layout, c, new String[] { }, new int[] {});
		this.onListClickListener = onListClickListener;
	}

	@Override
    public void bindView(View view, Context context, Cursor cursor) {

		OnItemClickListener onItemClickListener = new OnItemClickListener(cursor.getInt(cursor
				.getColumnIndex(DatabaseHelper.KEY_ID)));
    	
    	View summary = (View) view.findViewById(R.id.alarm_summary);
    	summary.setOnClickListener(onItemClickListener);
    	
    	int isEnabled = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_ENABLED));
        CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkBox);
        checkbox.setChecked((isEnabled == 1));
        checkbox.setOnClickListener(onItemClickListener);

        int hour = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_HOUR));
        int minute = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_MINUTE));
        TextView timeTextView = (TextView) view.findViewById(R.id.alarm_time);
        if (timeTextView != null)
        	timeTextView.setText("" + hour + ":" + minute);

        String label = cursor.getString(cursor.getColumnIndex(DatabaseHelper.KEY_LABEL));        
        TextView labelTextView = (TextView) view.findViewById(R.id.alarm_label);
        if (labelTextView != null) {
            labelTextView.setText(label);
        }
        
        int repeat = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.KEY_REPEATS));
        TextView repeatTextView = (TextView) view.findViewById(R.id.alarm_repeat);
        if (repeatTextView != null) {
        	StringBuffer repeatString = new StringBuffer();
        	if((repeat & 1) == 1) repeatString.append("Mon,");
        	if((repeat>>1 & 1) == 1) repeatString.append("Tue,");
        	if((repeat>>2 & 1) == 1) repeatString.append("Wed,");
        	if((repeat>>3 & 1) == 1) repeatString.append("Thu,");
        	if((repeat>>4 & 1) == 1) repeatString.append("Fri,");
        	if((repeat>>5 & 1) == 1) repeatString.append("Sat,");
        	if((repeat>>6 & 1) == 1) repeatString.append("Sun,");
        	if(repeatString.length() == 0)
        		repeatString.append("No repeat");
        	else
        		repeatString.deleteCharAt(repeatString.length()-1);
        	repeatTextView.setText(repeatString);
        }
    }

	private class OnItemClickListener implements OnClickListener {
		private int id;

		OnItemClickListener(int id) {
			this.id = id;
		}

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.alarm_summary) {
				if(onListClickListener != null)
					onListClickListener.alarmSelected(id);
			}
			if (v.getId() == R.id.checkBox) {
				if(onListClickListener != null)
					onListClickListener.alarmChecked(id, ((CheckBox)v).isChecked());
			}
		}
	}

}
