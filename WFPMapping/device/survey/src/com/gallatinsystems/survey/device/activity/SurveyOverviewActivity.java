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
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.TextView;

/**
 * Activity to list all "nearby" data records
 * 
 * @author Mark Tiele Westra
 */
public class SurveyOverviewActivity extends Activity {

	public static final int SURVEY_ACTIVITY = 1;
	public static final int DOWNLOAD_RECORDS_ACTIVITY = 10;
	private static final int RESULT_OK = 1;
	private static final String TAG = "Survey Home Activity";

	private TextView surveyField;
	private TextView currentUserField;
	private TextView recordsCollectedField;
	private TextView readyForUploadField;
	private TextView savedSurveysField;
	private SurveyDbAdapter databaseAdapter;
	private String userId;
	private String surveyId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.surveyoverview);

		surveyField = (TextView) findViewById(R.id.surveyField);
		currentUserField = (TextView) findViewById(R.id.currentUserField);
		recordsCollectedField = (TextView) findViewById(R.id.recordsCollectedField);
		readyForUploadField = (TextView) findViewById(R.id.readyForUploadField);
		savedSurveysField = (TextView) findViewById(R.id.savedSurveysField);
		// fixme: how many need resending because of failed media upload?

		databaseAdapter = new SurveyDbAdapter(this);
		databaseAdapter.open();

		Bundle extras = getIntent().getExtras();
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

		Cursor user = databaseAdapter.findUser(Long.parseLong(userId));
		startManagingCursor(user);
		if (user.getCount() > 0) {
			currentUserField.setText(user.getString(user
					.getColumnIndexOrThrow(SurveyDbAdapter.DISP_NAME_COL)));
		}

		surveyField.setText(surveyFromDb.getName());
		savedSurveysField.setText(Integer.toString(databaseAdapter
				.countSurveyRespondents(ConstantUtil.SAVED_STATUS)));
		recordsCollectedField.setText(Integer.toString(databaseAdapter
				.countSurveyRespondents(ConstantUtil.SUBMITTED_STATUS)));
		readyForUploadField.setText(Integer.toString(databaseAdapter
				.countUnsentSurveyRespondents(surveyId)));
	}

	public void startNewRecord(View view) {
		Survey survey = databaseAdapter.findSurvey(surveyId);

		if (survey != null) {
			Intent i = new Intent(view.getContext(), SurveyViewActivity.class);
			i.putExtra(ConstantUtil.USER_ID_KEY, userId);
			i.putExtra(ConstantUtil.SURVEY_ID_KEY, survey.getId());
			startActivityForResult(i, SURVEY_ACTIVITY);

		} else {
			Log.e(TAG, "Survey for selection is null");
		}
	}

	public void updateExistingRecord(View view) {
		// TODO
	}

	public void uploadData(View view) {
		// TODO
	}

	public void showSurveyList(View view) {
		// TODO
	}

	public void downloadData(View view) {
		Survey survey = databaseAdapter.findSurvey(surveyId);

		if (survey != null) {

			 Intent i = new Intent(view.getContext(), DownloadRecordsActivity.class);
			 i.putExtra(ConstantUtil.USER_ID_KEY, userId);
			 i.putExtra(ConstantUtil.SURVEY_ID_KEY, survey.getId());
			 startActivityForResult(i, DOWNLOAD_RECORDS_ACTIVITY);

		} else {
			Log.e(TAG, "Survey for selection is null");
		}
	}

	// handle completed survey
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == SURVEY_ACTIVITY) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				ViewUtil.showConfirmDialog(R.string.submitcompletetitle,
						R.string.submitcompletetext, this);
			}
		}
	}

	public void onResume() {
		super.onResume();
		databaseAdapter.open();

		// Update the survey counts
		savedSurveysField.setText(Integer.toString(databaseAdapter
				.countSurveyRespondents(ConstantUtil.SAVED_STATUS)));
		recordsCollectedField.setText(Integer.toString(databaseAdapter
				.countSurveyRespondents(ConstantUtil.SUBMITTED_STATUS)));
		readyForUploadField.setText(Integer.toString(databaseAdapter
				.countUnsentSurveyRespondents(surveyId)));
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