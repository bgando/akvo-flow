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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.domain.SurveyedLocaleValue;
import com.gallatinsystems.survey.device.util.ConstantUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * displays the detail information for a single nearby locale
 * 
 * @author Christopher Fagiani, Mark Tiele Westra
 */
public class NearbyLocaleDetailActivity extends Activity implements
		LocationListener, SensorEventListener {

	private static final int NAVIGATE_ID = Menu.FIRST;
	private static final int SURVEY_EDIT_ACTIVITY = 1;
	private LocationManager locMgr;
	private Criteria locationCriteria;
	private TextView nameField;
	private TextView distanceField;
	private SurveyDbAdapter databaseAdapter;
	private ScrollView scrollView;
	private ImageView arrowView;
	private SurveyedLocale sl;
	private Bitmap arrowBitmap;
	private Location slLocation;
	private float lastBearing;
	private float lastOrientation;
	private Sensor orientSensor;
	private Resources res;
	private SensorManager sensorMgr;
	private static final float MIN_CHANGE = 2f;
	private String userId;
	private String surveyId;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		lastBearing = -999f;
		lastOrientation = 0f;
		locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		sensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		orientSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		res = getResources();
		databaseAdapter = new SurveyDbAdapter(this);
		locationCriteria = new Criteria();
		locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
		setContentView(R.layout.localedetail);

		nameField = (TextView) findViewById(R.id.pointNameField);
		distanceField = (TextView) findViewById(R.id.distanceField);
		scrollView = (ScrollView) findViewById(R.id.pointscroll);
		arrowView = (ImageView) findViewById(R.id.arrowView);
		arrowBitmap = BitmapFactory.decodeResource(res, R.drawable.uparrow);

		sl = savedInstanceState != null ? (SurveyedLocale) savedInstanceState
				.getSerializable(ConstantUtil.SL_KEY) : null;
		if (sl == null) {
			Bundle extras = getIntent().getExtras();
			sl = extras != null ? (SurveyedLocale) extras
					.getSerializable(ConstantUtil.SL_KEY) : null;
		}
		slLocation = new Location(LocationManager.GPS_PROVIDER);
		slLocation.setLatitude(sl.getLatitude());
		slLocation.setLongitude(sl.getLongitude());

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
		
		databaseAdapter.open();
		populateFields();
	}

	
	public void updateLocale(View view) {
		// create new survey respondent
		Long respId = databaseAdapter.createSurveyRespondent(surveyId, userId);
		
		// add answers to the appropriate values
		List<SurveyedLocaleValue> slvList = databaseAdapter.listSurveyedLocaleValuesByLocaleId(sl.getId());
		QuestionResponse resp = new QuestionResponse(sl.getLocaleUUID(),"IDENT", "0");
		resp.setRespondentId(respId);
		databaseAdapter.createOrUpdateSurveyResponse(resp);
		
		for (SurveyedLocaleValue slv : slvList){
			resp = new QuestionResponse(slv.getAnswerValue(),ConstantUtil.VALUE_RESPONSE_TYPE, slv.getQuestionId());
			resp.setRespondentId(respId);
			databaseAdapter.createOrUpdateSurveyResponse(resp);
		}
		
		// initiate the survey view activity
		Intent i = new Intent(view.getContext(), SurveyViewActivity.class);
		i.putExtra(ConstantUtil.USER_ID_KEY, userId);
		i.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
		i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, respId);
		startActivityForResult(i,SURVEY_EDIT_ACTIVITY);
	}
	
	protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		if (resultCode == RESULT_OK){
			Intent intent = this.getIntent();
			this.setResult(RESULT_OK, intent);
			this.finish();
		}
	}
	
	
	/**
	 * when this activity is done, stop listening for location updates
	 */
	@Override
	public void onPause() {
		super.onPause();
		if (databaseAdapter != null) {
			databaseAdapter.close();
		}
		if (locMgr != null) {
			locMgr.removeUpdates(this);
		}
		if (sensorMgr != null) {
			sensorMgr.unregisterListener(this);
		}
	}

	public void onResume() {
		super.onResume();
		
		if (locMgr != null) {
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
					0, this);
		}
		if (sensorMgr != null) {
			sensorMgr.registerListener(this, orientSensor,
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	/**
	 * put loaded data into the views for display
	 */
	private void populateFields() {
		if (sl != null) {
			String lastSixOctets = sl.getLocaleUUID().substring(sl.getLocaleUUID().lastIndexOf('-') + 1);
			Long id = Long.parseLong(lastSixOctets,16);
			nameField.setText(Long.toString(id,36));
			Double dist=sl.getDistance();
			if (dist != null){
				DecimalFormat df = new DecimalFormat("#");
				// default: no decimal point, km as unit
				df = new DecimalFormat("#.#");
				String unit = "km";
				Double factor = 0.001; // convert from meters to km
				
				// for distances smaller than 1 km, use meters as unit
				if (dist < 1000.0) {
					factor = 1.0;
					unit = "m";
					df = new DecimalFormat("#"); // only whole meters
				} 
				dist = dist * factor;
				distanceField.setText(" " + df.format(dist) + unit);//show whole meters
			}

			List<SurveyedLocaleValue> slvList = databaseAdapter.listSurveyedLocaleValuesByLocaleId(sl.getId());
			if (slvList != null && slvList.size() > 0){
				LinearLayout l = new LinearLayout(this);
				l.setOrientation(LinearLayout.VERTICAL);
				for (SurveyedLocaleValue slv : slvList){
					LinearLayout subl = new LinearLayout(this);
					subl.setOrientation(LinearLayout.HORIZONTAL);

					// get metric name for this questions
					String metricName = databaseAdapter.getMetricNameByQuestionId(slv.getQuestionId());
					TextView labelView = new TextView(this);
					labelView.setText(metricName + ": ");
					labelView.setTextSize(20);
					subl.addView(labelView);
					TextView valView = new TextView(this);
					valView.setText(slv.getAnswerValue());
					valView.setTextSize(20);
					subl.addView(valView);
					l.addView(subl);
				}	
				scrollView.addView(l);
			}
		}
	}

	
	/**
	 * sets the selected point (if there is one) in the Bundle
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outState != null && sl != null) {
			outState.putSerializable(ConstantUtil.SL_KEY, sl);
		}
	}

	/**
	 * when the location is updated, update the distance field and change the
	 * rotation of the directional arrow. The units of the distance may change
	 * based on the distance (i.e. the point is more than 1 KM away, the
	 * distance will be shown in kilometers. Otherwise, meters are used.
	 */
	@Override
	public void onLocationChanged(Location loc) {
		DecimalFormat df = new DecimalFormat("#");
		// set the distance value
		float distance = slLocation.distanceTo(loc);
		// FIXME create function for displaying this, so we can use it above as well
		distanceField.setText(" " + df.format(distance) + "m");//show whole meters
		
		// only update the bearing and the corresponding image representation of
		// it if it changed more than MIN_CHANGE degrees
		// so we're not always manipulating the image
		float newBearing = loc.bearingTo(slLocation);
		if (Math.abs(newBearing - lastBearing) >= MIN_CHANGE) {
			lastBearing = newBearing;
			updateArrow();
		}
	}

	/**
	 * Rotates the direction arrow so it always points at the item
	 */
	private void updateArrow() {
		Matrix matrix = new Matrix();
		matrix.postRotate(lastBearing - lastOrientation);
		Bitmap resizedBitmap = Bitmap.createBitmap(arrowBitmap, 0, 0, 30, 30,
				matrix, true);
		arrowView.setImageDrawable(new BitmapDrawable(res, resizedBitmap));
	}

	/**
	 * launches map view and zooms in on surveyed Locale
	 */
	public void showLocaleMap(View view) {
		Intent mapsIntent = new Intent(this, PointOfInterestMapActivity.class);
		ArrayList<SurveyedLocale> slList = new ArrayList<SurveyedLocale>();
		slList.add(sl);
		mapsIntent.putExtra(ConstantUtil.SL_KEY, slList);
		startActivity(mapsIntent);
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/**
	 * update the directional arrow since we detected a change in orientation
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor == orientSensor) {
			lastOrientation = event.values[0];
			updateArrow();

		}
	}	
}