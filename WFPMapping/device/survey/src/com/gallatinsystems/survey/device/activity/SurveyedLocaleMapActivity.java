/*  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import android.os.Bundle;
import android.view.Window;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.GeoUtil;
import com.gallatinsystems.survey.device.view.SurveyedLocaleOverlay;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.gallatinsystems.survey.device.view.EnumeratorLocationOverlay;

/**
 * This activity is used for displaying 1 or more SurveyedLocales on a map
 * with info-bubble overlays that show the point information.
 * 
 * @author Christopher Fagiani, Mark Westra
 * 
 */
public class SurveyedLocaleMapActivity extends MapActivity {

	private MapView mapView;
	private MapController mapController;
	private static final int INITIAL_ZOOM_LEVEL = 14;
	private ArrayList<SurveyedLocale> points;
	private SurveyedLocaleOverlay overlay;

	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.pointmap);

		mapView = (MapView) findViewById(R.id.pointmapview);
		mapController = mapView.getController();
		mapController.setZoom(INITIAL_ZOOM_LEVEL);
		// turn on zoom controls
		mapView.setBuiltInZoomControls(true);
		points = (ArrayList<SurveyedLocale>) getIntent().getExtras()
				.getSerializable(ConstantUtil.SL_KEY);
		initializeOverlay();
	}

	/**
	 * initializes and installs the point overlay using the data loaded into the
	 * points arrayList
	 */
	private void initializeOverlay() {
		GeoPoint firstPoint = null;
		if (points != null) {
			overlay = new SurveyedLocaleOverlay(this);
			for (int i = 0; i < points.size(); i++) {
				SurveyedLocale sl = points.get(i);
				if (sl.getLatitude() != null && sl.getLongitude() != null) {
					if (firstPoint == null) {
						firstPoint = GeoUtil.convertToPoint(sl.getLatitude(),
								sl.getLongitude());
					}
					overlay.addLocation(GeoUtil.convertToPoint(sl
							.getLatitude(), sl.getLongitude()));
				}
			}
			List<Overlay> overlays = mapView.getOverlays();
			overlays.add(overlay);

			// add 'where am I' dot
			EnumeratorLocationOverlay myLocationOverlay = new EnumeratorLocationOverlay(this, mapView);
			myLocationOverlay.enableMyLocation();
			myLocationOverlay.enableCompass();
			overlays.add(myLocationOverlay);
		}	
		if (firstPoint != null) {
			mapController.setCenter(firstPoint);
		}
	}

	/**
	 * returns the point stored at the index passed in. Null is returned if the
	 * index is invalid
	 * 
	 * @param idx
	 * @return
	 */
	public SurveyedLocale getPoint(int idx) {
		if (idx > -1 && points != null && points.size() > idx) {
			return points.get(idx);
		} else {
			return null;
		}
	}

	protected boolean isRouteDisplayed() {
		return false;
	}
}