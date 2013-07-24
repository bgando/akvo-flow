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

import java.util.ArrayList;
import java.util.List;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.PointOfInterest;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.util.ConstantUtil;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity to create a list of filtered surveyed locales
 * 
 * @author Mark Tiele Westra
 */
public class FilterLocaleActivity extends ListActivity implements LocationListener {
	
	private static final int NEARBY_LOCALE_DETAIL_ACTIVITY = 0;
	private SurveyDbAdapter databaseAdapter;
	private LocationManager locMgr;
	private Criteria locationCriteria;
	private Cursor slCursor;
	private ArrayList<SurveyedLocale> slArray;
	private String projectId;
	private String userId;
	private String surveyId;
	double nearbyRadius;
	private EditText searchField;
	int skip = 0;
	Double lat = null;
	Double lon = null;
	String TAG = "Filter locale activity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.filterlocale);
		searchField = (EditText) findViewById(R.id.searchField);
		databaseAdapter = new SurveyDbAdapter(this);

		locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationCriteria = new Criteria();
		locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);

		Bundle extras = getIntent().getExtras();
		projectId = extras != null ? extras.getString(ConstantUtil.PROJECT_ID_KEY) : null;
		if (projectId == null) {
			projectId = savedInstanceState != null ? savedInstanceState.getString(ConstantUtil.PROJECT_ID_KEY) : "1";
		}
		
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
	}
	
	public void onResume() {
		super.onResume();
		databaseAdapter.open();
		if (slCursor != null) {
			slCursor.close();
		}

		String val = databaseAdapter.findPreference(ConstantUtil.NEARBY_LOCALE_RADIUS);
		if (val != null) {
			try{
				nearbyRadius = Double.parseDouble(val);
				}
			catch (NumberFormatException e){
				nearbyRadius = ConstantUtil.DEFAULT_MAX_DISTANCE;
				}
		} else {
			nearbyRadius = ConstantUtil.DEFAULT_MAX_DISTANCE;
		}

		// try to find out where we are
		String provider = locMgr.getBestProvider(locationCriteria, true);
		if (provider != null) {
			Location loc = locMgr.getLastKnownLocation(provider);
			if (loc != null) {
				lat = loc.getLatitude();
				lon = loc.getLongitude();
				locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
			} else {
				// if we don't know where we are, ask for updates
				locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
			}
		}
		// filter surveyedLocales by location and
		slCursor = databaseAdapter.listFilteredSurveyedLocales(projectId, lat, lon, null, nearbyRadius);
		slArray = databaseAdapter.listSurveyedLocalesByCursor(slCursor, skip, 10, lat, lon);
		updateUi(slArray);

		// take care of search
		searchField.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 2){
					slCursor = databaseAdapter.listFilteredSurveyedLocales(projectId, lat, lon, s.toString(), nearbyRadius);
				} else {
					slCursor = databaseAdapter.listFilteredSurveyedLocales(projectId, lat, lon, null, nearbyRadius);
				}
				slArray = databaseAdapter.listSurveyedLocalesByCursor(slCursor, skip, 10, lat, lon);
				updateUi(slArray);
	        }
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		});
	}


	/**
	 * updates the ui with the data passed in
	 */
	private void updateUi(List<SurveyedLocale> slArray) {	
		if(slArray != null){
			ArrayAdapter<SurveyedLocale> aa = new ArrayAdapter<SurveyedLocale>(this, R.layout.localelistrow, slArray);
			setListAdapter(aa);
		}
	}
	
	public void showLocaleMap(View view) {
		Intent mapsIntent = new Intent(this, SurveyedLocaleMapActivity.class);
		mapsIntent.putExtra(ConstantUtil.SL_KEY, slArray);
		startActivity(mapsIntent);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		Intent i = new Intent(this, NearbyLocaleDetailActivity.class);
		i.putExtra(ConstantUtil.SL_KEY, slArray.get(position));
		i.putExtra(ConstantUtil.USER_ID_KEY, userId);
		i.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
		startActivityForResult(i, NEARBY_LOCALE_DETAIL_ACTIVITY);
	}

	protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
		if (resultCode == RESULT_OK){
			Intent intent = this.getIntent();
			this.setResult(RESULT_OK, intent);
			this.finish();
		}
	}
	
	protected void onPause() {
		if (locMgr != null) {
			locMgr.removeUpdates(this);
		}
		
		if (slCursor != null) {
			slCursor.close();
		}
		super.onPause();
	}

	@Override
	public void onLocationChanged(Location location) {
		// a single location is all we need
		locMgr.removeUpdates(this);
		lat = location.getLatitude();
		lon = location.getLongitude();
		slArray = databaseAdapter.listSurveyedLocalesByCursor(slCursor, skip, 10, lat, lon);
		updateUi(slArray);	
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}