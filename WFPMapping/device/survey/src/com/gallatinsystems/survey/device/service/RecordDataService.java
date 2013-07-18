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

package com.gallatinsystems.survey.device.service;

import java.util.concurrent.Semaphore;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

/**
 * this service will check for new records in the backend
 * 
 * @author Mark Westra
 * 
 */
public class RecordDataService extends IntentService {
	private int result = Activity.RESULT_CANCELED;
	private static final String TAG = "RECORD_DATA_SERVICE";

	private static final String RECORD_SERVICE_PATH = "/recorddata?projectId=";
	private static final String PHONE_PARAM = "&devicePhoneNumber=";
	private static final String DEV_ID_PARAM = "&devId=";
	private static final String IMEI_PARAM = "&imei=";
	private static final String CHECK_AVAILABLE_PARAM = "&checkAvailable=";

	private SurveyDbAdapter databaseAdaptor;
	private PropertyUtil props;
	private static Semaphore lock = new Semaphore(1);

	public RecordDataService() {
		super("RecordDataService");
	}

	/**
	 * Handles the service call. Depending on the ACTION parameter in the
	 * extras, two things can happen:
	 * action = getcount   : get a count of available records
	 * action = getrecords : get the records themselves
	 * @param intent
	 * @return
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		int result = 0;
		String action = null;

		if (intent != null) {
			String surveyId = null;
			if (intent.getExtras() != null) {
				surveyId = intent.getExtras().getString(
						ConstantUtil.SURVEY_ID_KEY);
				action = intent.getExtras().getString("ACTION");
				// FIXME for now the project id is the same as the survey id.
				String projectId = surveyId;
				if (isAbleToRun() && action != null) {
					try {
						lock.acquire();
						databaseAdaptor = new SurveyDbAdapter(this);
						databaseAdaptor.open();
						// get server URL and device id
						String serverBase = databaseAdaptor
								.findPreference(ConstantUtil.SERVER_SETTING_KEY);
						String deviceId = databaseAdaptor
								.findPreference(ConstantUtil.DEVICE_IDENT_KEY);
						if (serverBase != null
								&& serverBase.trim().length() > 0) {
							serverBase = getResources().getStringArray(
									R.array.servers)[Integer
									.parseInt(serverBase)];
						} else {
							serverBase = props
									.getProperty(ConstantUtil.SERVER_BASE);
						}

						// get record count or complete records, depending on
						// the action
						if (action.equals("getcount")) {
							result = getRecordsAvailable(serverBase, projectId,
									deviceId);
						} else if (action.equals("getrecords")) {
							result = downloadAvailableRecords(serverBase,
									projectId, deviceId);

						}
					} catch (Exception e) {
						Log.e(TAG, "Could not check for records");
						PersistentUncaughtExceptionHandler.recordException(e);
					} finally {
						databaseAdaptor.close();
						lock.release();
					}
				}
			}
		}

		Bundle extras = intent.getExtras();
		if (extras != null) {
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.arg1 = Activity.RESULT_CANCELED;
			msg.arg2 = result;
			if (action != null && action.equals("getcount")) {
				msg.arg1 = ConstantUtil.RECORDS_SERVER;
			} else if (action != null && action.equals("getrecords")) {
				msg.arg1 = ConstantUtil.RECORDS_DEVICE;
			}

			try {
				messenger.send(msg);
			} catch (android.os.RemoteException e1) {
				Log.w(getClass().getName(), "Exception sending message", e1);
			}
		}
	}

	/**
	 * Forms a URL string to request records from the backend. Depending on the
	 * checkAvailable flag, only a count is obtained, or all the records.
	 * @param serverBase
	 * @param projectId
	 * @param deviceId
	 * @param checkAvailable
	 * @return
	 */
	private String formUrl(String serverBase, String projectId,
			String deviceId, boolean checkAvailable)
			throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		builder.append(serverBase);
		builder.append(RECORD_SERVICE_PATH);
		builder.append(URLEncoder.encode(projectId, "UTF-8"));
		builder.append(IMEI_PARAM);
		builder.append(URLEncoder.encode(StatusUtil.getImei(this), "UTF-8"));
		builder.append((deviceId != null ? DEV_ID_PARAM
				+ URLEncoder.encode(deviceId, "UTF-8") : ""));
		builder.append(CHECK_AVAILABLE_PARAM);
		builder.append(checkAvailable ? "true" : "false");
		return builder.toString();
	}

	/**
	 * Checks if there are records available on the backend for the 
	 * projectId passed in, and downloads them. It returns a count of downloaded records.
	 * 
	 * @param serverBase
	 * @param projectId
	 * @param deviceId
	 * @return
	 */
	private int downloadAvailableRecords(String serverBase, String projectId,
			String deviceId) {
		String response = null;
		int responseCount = 0;
		try {
			response = HttpUtil.httpGet(formUrl(serverBase, projectId,
					deviceId, false));
			// get records
			if (response != null) {
				JSONObject jsonObj = new JSONObject(response);

				// save records
				JSONArray recordItemArr = jsonObj.getJSONArray("recordData");
				int recordItemArrLen = recordItemArr.length();
				responseCount = recordItemArrLen;
				for (int i = 0; i < recordItemArrLen; i++) {

					Double lat;
					Double lon;
					String id = recordItemArr.getJSONObject(i).getString("id").toString();
					Log.v(TAG,"processing " + id);
					String lastSDate = recordItemArr.getJSONObject(i).getString("lastSDate").toString();
					Integer projectIdInt = Integer.parseInt(projectId);
					try {
						lat = Double.parseDouble(recordItemArr.getJSONObject(i).getString("lat").toString());
						lon = Double.parseDouble(recordItemArr.getJSONObject(i).getString("lon").toString());
					} catch (NumberFormatException e){
						lat = null;
						lon = null;
					}

					JSONArray questionIdArray = new JSONArray(recordItemArr.getJSONObject(i).getString("questionIds"));
					JSONArray answerValArray = new JSONArray(recordItemArr.getJSONObject(i).getString("answerValues"));

					Long slId = databaseAdaptor.createOrUpdateSurveyedLocale(id, projectIdInt, lastSDate, lat, lon);
					databaseAdaptor.createOrUpdateSurveyalValues(slId, questionIdArray, answerValArray);
				}
			}
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Cannot form URL", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} catch (HttpException e) {
			Log.e(TAG, "Server returned an unexpected response", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} catch (Exception e) {
			Log.e(TAG, "Could not download record data", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		}
		return responseCount;
	}

	/**
	 * Checks if there are records available on the backend for the
	 * projectId passed in. It returns a count of available records.
	 *
	 * @param serverBase
	 * @param projectId
	 * @param deviceId
	 * @return
	 */
	private int getRecordsAvailable(String serverBase, String projectId,
			String deviceId) {
		String response = null;
		int responseCount = 0;
		try {
			response = HttpUtil.httpGet(formUrl(serverBase, projectId,
					deviceId, true));
			// get count and return it
			if (response != null) {
				JSONObject jsonObj = new JSONObject(response);
				String responseCountString = jsonObj.getString("recordCount")
						.toString();
				responseCount = Integer
						.parseInt(responseCountString != null ? responseCountString
								: "0");
			}
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Cannot form URL", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} catch (HttpException e) {
			Log.e(TAG, "Server returned an unexpected response", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} catch (Exception e) {
			Log.e(TAG, "Could not download record data", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		}
		return responseCount;
	}

	/**
	 * Checks if the service can perform the requested operation. If
	 * there is no connectivity, this will return false, otherwise it will
	 * return true
	 *
	 * @param type
	 * @return
	 */
	private boolean isAbleToRun() {
		return StatusUtil.hasDataConnection(this, false);
	}
}
