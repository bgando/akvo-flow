/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.activity;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.service.RecordDataService;
import com.gallatinsystems.survey.device.util.ConstantUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

/**
 * Activity to cache records on phone
 * 
 * @author Mark Tiele Westra
 */
public class DownloadRecordsActivity extends Activity {

	public static final int SURVEY_ACTIVITY = 1;
	private static final String TAG = "Download Records Activity";

	private TextView surveyField;
	private TextView totalRecordsOnServerField;
	private TextView cachedOnPhoneField;
	private SurveyDbAdapter databaseAdapter;
	private String userId;
	private String surveyId;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			if (message.arg1 == ConstantUtil.RECORDS_SERVER) {
				totalRecordsOnServerField.setText(Integer
						.toString(message.arg2));
			} else if (message.arg1 == ConstantUtil.RECORDS_DEVICE){
				cachedOnPhoneField.setText(Integer
						.toString(message.arg2));
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.downloadrecords);

		surveyField = (TextView) findViewById(R.id.surveyField);
		totalRecordsOnServerField = (TextView) findViewById(R.id.totalonserver);
		cachedOnPhoneField = (TextView) findViewById(R.id.cachedonphone);

		databaseAdapter = new SurveyDbAdapter(this);
		databaseAdapter.open();

		Bundle extras = getIntent().getExtras();

		// we don't need the user now, but might come in handy later
		// if we want to make access to data user-dependent
		userId = extras != null ? extras.getString(ConstantUtil.USER_ID_KEY)
				: null;
		if (userId == null) {
			userId = savedInstanceState != null ? savedInstanceState
					.getString(ConstantUtil.USER_ID_KEY) : null;
		}

		surveyId = extras != null ? extras
				.getString(ConstantUtil.SURVEY_ID_KEY) : null;
		if (surveyId == null) {
			surveyId = savedInstanceState != null ? savedInstanceState
					.getString(ConstantUtil.SURVEY_ID_KEY) : "1";
		}

		populateFields();
	}

	/**
	 * put loaded data into the views for display
	 */
	private void populateFields() {

		Survey surveyFromDb = databaseAdapter.findSurvey(surveyId);
		surveyField.setText(surveyFromDb.getName());
		
		cachedOnPhoneField.setText(Integer.toString(databaseAdapter.countSurveyedLocale()));
		
		Intent intent = new Intent(this, RecordDataService.class);
		Messenger messenger = new Messenger(handler);
		intent.putExtra("MESSENGER", messenger);
		intent.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
		intent.putExtra("ACTION", "getcount");
		startService(intent);

	}

	public void downloadRecords(View view) {
		Intent intent = new Intent(this, RecordDataService.class);
		Messenger messenger = new Messenger(handler);
		intent.putExtra("MESSENGER", messenger);
		intent.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
		intent.putExtra("ACTION", "getrecords");
		startService(intent);
	}

	public void deleteCachedRecords(View view) {
		databaseAdapter.deleteAllSurveyedLocale();
		databaseAdapter.deleteAllSurveyalValue();
		cachedOnPhoneField.setText(Integer
				.toString(0));
	}

	public void onResume() {
		super.onResume();
		databaseAdapter.open();

	}

	protected void onPause() {
		super.onPause();
	}

	protected void onDestroy() {
		if (databaseAdapter != null) {
			databaseAdapter.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

}