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

package org.waterforpeople.mapping.app.web.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;


/**
 * dto that can hold record data
 * 
 * @author Mark Westra
 * 
 */
public class RecordDataDto implements Serializable {

	private static final long serialVersionUID = -850583183416882347L;

	private String id;
	private Double lat;
	private Double lon;
	private Date lastSDate;
	private List<Long> questionIds;
	private List<String> answerValues;

	public RecordDataDto() {
		questionIds = new ArrayList<Long>();
		answerValues = new ArrayList<String>();
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}
	
	public List<Long> getQuestionIds() {
		return questionIds;
	}

	public void setQuestionIds(List<Long> questionIds) {
		this.questionIds = questionIds;
	}

	public List<String> getAnswerValues() {
		return answerValues;
	}

	public void setAnswerValues(List<String> answerValues) {
		this.answerValues = answerValues;
	}
	
	public void addProperty(Long questionId, String answerValue) {
		questionIds.add(questionId);
		answerValues.add(answerValue != null ? answerValue : "");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getLastSDate() {
		return lastSDate;
	}

	public void setLastSDate(Date lastSDate) {
		this.lastSDate = lastSDate;
	}
}
