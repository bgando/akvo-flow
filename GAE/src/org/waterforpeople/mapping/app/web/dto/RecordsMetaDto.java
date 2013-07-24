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
 * dto that can hold record meta data
 * 
 * @author Mark Westra
 * 
 */
public class RecordsMetaDto implements Serializable {

	private static final long serialVersionUID = -850583983416882347L;

	private String id;
	private List<Long> questionIds;
	private List<String> metricNames;
	private List<Boolean> includeInList;

	public RecordsMetaDto() {
		setQuestionIds(new ArrayList<Long>());
		setMetricNames(new ArrayList<String>());
		setIncludeInList(new ArrayList<Boolean>());
	}

	public void addItem(Long questionId, String metricName, Boolean includeIn) {
		questionIds.add(questionId);
		metricNames.add(metricName != null ? metricName : "");
		includeInList.add(includeIn != null ? includeIn.booleanValue() : false);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Long> getQuestionIds() {
		return questionIds;
	}

	public void setQuestionIds(List<Long> questionIds) {
		this.questionIds = questionIds;
	}

	public List<String> getMetricNames() {
		return metricNames;
	}

	public void setMetricNames(List<String> metricNames) {
		this.metricNames = metricNames;
	}

	public List<Boolean> getIncludeInList() {
		return includeInList;
	}

	public void setIncludeInList(List<Boolean> includeInList) {
		this.includeInList = includeInList;
	}
}
