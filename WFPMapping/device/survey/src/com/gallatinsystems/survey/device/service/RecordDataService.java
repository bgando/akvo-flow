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
 * this activity will check for new surveys on the device and install as needed
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

	private SurveyDbAdapter databaseAdaptor;
	private PropertyUtil props;
	private static Semaphore lock = new Semaphore(1);

	public RecordDataService() {
		super("RecordDataService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int recordsAvailable = 0;
		Log.d(TAG, "trying..... on handle intent");
		if (intent != null) {
			String surveyId = null;
			if (intent.getExtras() != null) {
				surveyId = intent.getExtras().getString(
						ConstantUtil.SURVEY_ID_KEY);
				// FIXME for now the project id is the same as the survey id.
				String projectId = surveyId;
			
				if (isAbleToRun()) {
					try {
						lock.acquire();
						databaseAdaptor = new SurveyDbAdapter(this);
						databaseAdaptor.open();
						props = new PropertyUtil(getResources());
						String serverBase = databaseAdaptor
								.findPreference(ConstantUtil.SERVER_SETTING_KEY);
						String deviceId = databaseAdaptor
								.findPreference(ConstantUtil.DEVICE_IDENT_KEY);
						if (serverBase != null && serverBase.trim().length() > 0) {
							serverBase = getResources().getStringArray(R.array.servers)[Integer
									.parseInt(serverBase)];
						} else {
							serverBase = props.getProperty(ConstantUtil.SERVER_BASE);
						}
						recordsAvailable = checkBackendForRecords(serverBase,
								projectId, deviceId);

						// Sucessful finished
						result = Activity.RESULT_OK;


					} catch (Exception e) {
						Log.e(TAG, "Could not check for available records", e);
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
			msg.arg1 = result;
			msg.arg2 = recordsAvailable;
			try {
				messenger.send(msg);
			} catch (android.os.RemoteException e1) {
				Log.w(getClass().getName(), "Exception sending message", e1);
			}

		}

	}

	/**
	 * this method checks if the service can perform the requested operation. If
	 * there is no connectivity, this will return false, otherwise it will
	 * return true
	 * 
	 * 
	 * @param type
	 * @return
	 */
	private boolean isAbleToRun() {
		return StatusUtil.hasDataConnection(this, false);
	}

	/**
	 * invokes a service call to get the number of records available for this
	 * survey.
	 * 
	 * @return - the count of the number of available records for that survey.
	 */
	private int checkBackendForRecords(String serverBase, String projectId,
			String deviceId) {
		String response = null;
		Log.d(TAG, "trying..... check backend for records");
		try {
			response = HttpUtil.httpGet(serverBase
					+ RECORD_SERVICE_PATH
					+ URLEncoder.encode(projectId, "UTF-8")
					+ PHONE_PARAM
					+ URLEncoder.encode(StatusUtil.getPhoneNumber(this),
							"UTF-8")
					+ IMEI_PARAM
					+ URLEncoder.encode(StatusUtil.getImei(this), "UTF-8")
					+ (deviceId != null ? DEV_ID_PARAM
							+ URLEncoder.encode(deviceId, "UTF-8") : ""));
			if (response != null) {
				
				Log.i(TAG, response);
			
				JSONObject jsonObj = new JSONObject(response); 
				JSONArray recordItemArr = jsonObj.getJSONArray("recordData");
				int recordItemArrLen = recordItemArr.length();
				for (int i = 0; i < recordItemArrLen; i++) { 
					// printing the values to the logcat 
					Log.v(TAG,recordItemArr.getJSONObject(i).getString("identifier").toString()); 
				}
			}
		} catch (HttpException e) {
			Log.e(TAG, "Server returned an unexpected response", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} catch (Exception e) {
			Log.e(TAG, "Could not download record data", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		}
		return 0;
	}

}


	