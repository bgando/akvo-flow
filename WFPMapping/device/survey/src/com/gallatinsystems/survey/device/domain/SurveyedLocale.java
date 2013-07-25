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

package com.gallatinsystems.survey.device.domain;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;

public class SurveyedLocale implements Serializable {
	private static final long serialVersionUID = -545828029643355078L;

	private Long id;
	private Double latitude;
	private Double longitude;
	private String projectId;
	private List<String> metricNames;
	private List<String> metricValues;
	private String localeUUID;
	private String lastSubmittedDate;
	private int status;
	private Double distance;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getLocaleUUID() {
		return localeUUID;
	}

	public void setLocaleUUID(String localeUUID) {
		this.localeUUID = localeUUID;
	}

	public String getLastSubmittedDate() {
		return lastSubmittedDate;
	}

	public void setLastSubmittedDate(String lastSubmittedDate) {
		this.lastSubmittedDate = lastSubmittedDate;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		// this should really be z-base-32 encoding
		if (localeUUID != null) {
			String lastSixOctets = localeUUID.substring(localeUUID.lastIndexOf('-') + 1);
			Long id = Long.parseLong(lastSixOctets,16);
			builder.append("Id: ").append(Long.toString(id,36));
		}	
		if (distance!=null){
			// default: no decimal point, km as unit
			DecimalFormat df = new DecimalFormat("#.#");
			String unit = "km";
			Double factor = 0.001; // convert from meters to km

			// for distances smaller than 1 km, use meters as unit
			if (distance < 1000.0) {
				factor = 1.0;
				unit = "m";
				df = new DecimalFormat("#"); // only whole meters
			} 		
			double dist = distance * factor;
			builder.append(".    dist: ").append(df.format(dist)).append(unit).append("\n");
		} else {
			builder.append(".    dist: unknown\n") ;
		}

		if (getLastSubmittedDate() != null){
			builder.append("Last updated: ").append(getLastSubmittedDate());
		}

		if (metricNames != null && metricNames.size() > 0){
			for (int i = 0 ; i < metricNames.size(); i++){
				builder.append("\n").append(metricNames.get(i)).append(" : ").append(metricValues.get(i));
			}
		}		
		return builder.toString();
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public List<String> getMetricNames() {
		return metricNames;
	}

	public void setMetricNames(List<String> metricNames) {
		this.metricNames = metricNames;
	}

	public List<String> getMetricValues() {
		return metricValues;
	}

	public void setMetricValues(List<String> metricValues) {
		this.metricValues = metricValues;
	}	
}