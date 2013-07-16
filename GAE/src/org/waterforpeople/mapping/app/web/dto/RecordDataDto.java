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

/**
 * dto that can hold record data
 * 
 * @author Mark Westra
 * 
 */
public class RecordDataDto implements Serializable {

	private static final long serialVersionUID = -850583183416882347L;

	private String identifier;
	private Double latitude;
	private Double longitude;

	private List<Long> questionIds;
	private List<String> answerValues;

	public RecordDataDto() {
		questionIds = new ArrayList<Long>();
		answerValues = new ArrayList<String>();
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

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
