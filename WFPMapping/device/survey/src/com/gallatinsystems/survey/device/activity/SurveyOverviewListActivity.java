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

import android.app.TabActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.view.adapter.SubmittedSurveyReviewCursorAdaptor;

/**
 * Displays two tabs: Pending + Completed
 * 
 * @author Stellan Lagerstrom, Mark Westra
 * 
 */
public class SurveyOverviewListActivity extends TabActivity implements OnTabChangeListener {

	private static final String TAG = "SurveyOverviewListActivity";
    private static final String PENDING_TAB_TAG = "Pending";
    private static final String COMPLETED_TAB_TAG = "Completed";

	private static final int UPDATE_INTERVAL_MS = 5000; //every five seconds
	private SurveyDbAdapter databaseAdapter;
	private Cursor dataCursor;
	ListView listViewPending;
	ListView listViewCompleted;
	
	private Handler mHandler = new Handler();
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			getData();
			mHandler.postDelayed(this,UPDATE_INTERVAL_MS);
			}
		};

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.surveyoverviewlists);
        TabHost tabHost = getTabHost();

        listViewPending = (ListView) findViewById(R.id.pendinglist);
     	listViewCompleted = (ListView) findViewById(R.id.completedlist);
        tabHost.setOnTabChangedListener(this);

        TabSpec spec1=tabHost.newTabSpec("Pending");
        spec1.setIndicator(PENDING_TAB_TAG);
        spec1.setContent(R.id.pendinglist);
        TabSpec spec2=tabHost.newTabSpec("Completed");
        spec2.setIndicator(COMPLETED_TAB_TAG);
        spec2.setContent(R.id.completedlist);
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);

        //Used to reduce the height of the Tabs
        //tabHost.getTabWidget().getChildAt(0).getLayoutParams().height *= 0.75;
        //tabHost.getTabWidget().getChildAt(1).getLayoutParams().height *= 0.75; 
        tabHost.setCurrentTab(0);    

        databaseAdapter = new SurveyDbAdapter(this);
        databaseAdapter.open();
        getData();
	}

	/**
	 * loads the survey instances from the database. this will load
	 * only submitted surveys.
	 */
	private void getData() {
		try{
			if(dataCursor != null){
				dataCursor.close();
			}
		}catch(Exception e){
			Log.w(TAG, "Could not close old cursor before reloading list",e);
		}
		dataCursor = databaseAdapter.listSurveyRespondent(ConstantUtil.SUBMITTED_STATUS, true);		

		SubmittedSurveyReviewCursorAdaptor surveys = new SubmittedSurveyReviewCursorAdaptor(this, dataCursor);
		listViewPending.setAdapter(surveys);
		listViewCompleted.setAdapter(surveys);
	}

	  @Override
	    public void onResume() {
	        super.onResume();  // Always call the superclass method first
			databaseAdapter.open();
			getData();
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, UPDATE_INTERVAL_MS);
	    }

	  protected void onDestroy() {
			if (databaseAdapter != null) {
				databaseAdapter.close();
			}
			super.onDestroy();
		}

	@Override
	public void onTabChanged(String tabId) {
		 if(tabId.equals(PENDING_TAB_TAG)) {
			 //FIXME update data
			 Log.v(TAG,"now on tab 1");
		 } else if(tabId.equals(COMPLETED_TAB_TAG)) {	
			 //FIXME update data	
			 Log.v(TAG,"now on tab 2");
		 }		
	}
}