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

package org.waterforpeople.mapping.app.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.waterforpeople.mapping.app.web.dto.RecordDataDto;
import org.waterforpeople.mapping.app.web.dto.RecordDataRequest;
import org.waterforpeople.mapping.app.web.dto.RecordDataResponse;
import org.waterforpeople.mapping.dao.QuestionAnswerStoreDao;
import org.waterforpeople.mapping.dao.SurveyInstanceDAO;
import org.waterforpeople.mapping.domain.QuestionAnswerStore;
import org.waterforpeople.mapping.domain.SurveyInstance;

import com.gallatinsystems.framework.rest.AbstractRestApiServlet;
import com.gallatinsystems.framework.rest.RestRequest;
import com.gallatinsystems.framework.rest.RestResponse;
import com.gallatinsystems.surveyal.dao.SurveyedLocaleDao;
import com.gallatinsystems.surveyal.dao.SurveyedLocaleSummaryDao;
import com.gallatinsystems.surveyal.domain.SurveyedLocale;
import com.gallatinsystems.surveyal.domain.SurveyedLocaleSummary;

/**
 * JSON service for returning the list of records for a specific surveyId
 * 
 * @author Mark Tiele Westra
 */
public class RecordDataServlet extends AbstractRestApiServlet {
	private static final long serialVersionUID = 8748650927754433019L;
	private SurveyedLocaleDao surveyedLocaleDao;

	public RecordDataServlet() {
		setMode(JSON_MODE);
		surveyedLocaleDao = new SurveyedLocaleDao();
	}

	@Override
	protected RestRequest convertRequest() throws Exception {
		HttpServletRequest req = getRequest();
		RestRequest restRequest = new RecordDataRequest();
		restRequest.populateFromHttpRequest(req);
		return restRequest;
	}

	/**
	 * calls the surveyedLocaleDao to get the list of surveyedLocales for a
	 * certain projectId passed in via the request, or the total number of
	 * available records if the checkAvailable flag is set.
	 */
	@Override
	protected RestResponse handleRequest(RestRequest req) throws Exception {
		RecordDataRequest rdReq = (RecordDataRequest) req;

		RecordDataResponse resp = new RecordDataResponse();

		if (rdReq.getCheckAvailable()) {
			SurveyedLocaleSummaryDao SLSdao = new SurveyedLocaleSummaryDao();
			SurveyedLocaleSummary SLSummary = SLSdao.getByProjectId(rdReq.getProjectId());
			if (SLSummary != null && SLSummary.getKey() != null) {
				resp.setResultCount(SLSummary.getCount());
			} else {
				resp.setResultCount(0L);
			}
			return resp;
		}

		List<SurveyedLocale> results = surveyedLocaleDao
				.listLocalesByProjectId(rdReq.getProjectId(),rdReq.getCursor());

		return convertToResponse(results, SurveyedLocaleDao.getCursor(results));
	}

	/**
	 * converts the domain objects to dtos and then installs them in an
	 * RecordDataResponse object
	 */
	protected RecordDataResponse convertToResponse(List<SurveyedLocale> slList,
			String cursor) {
		RecordDataResponse resp = new RecordDataResponse();
		if (slList != null) {
			List<RecordDataDto> dtoList = new ArrayList<RecordDataDto>();
			SurveyInstanceDAO siDao = new SurveyInstanceDAO();
			QuestionAnswerStoreDao qaDao = new QuestionAnswerStoreDao();
			for (SurveyedLocale sl : slList) {

				RecordDataDto dto = new RecordDataDto();
				dto.setIdentifier(sl.getIdentifier());
				dto.setLatitude(sl.getLatitude());
				dto.setLongitude(sl.getLongitude());

				List<SurveyInstance> siList = siDao.listInstancesByLocale(sl
						.getKey().getId(), null, null, null);
				if (siList != null && siList.size() > 0) {
					SurveyInstance si = siList.get(0);
					if (si != null && si.getKey() != null) {
						List<QuestionAnswerStore> qaList = qaDao
								.listBySurveyInstance(si.getKey().getId(),
										null, null);
						if (qaList != null && qaList.size() > 0) {
							for (QuestionAnswerStore qa : qaList) {
								// exclude type GEO and PHOTO
								if (!qa.getType().equals("GEO")
										&& !qa.getType().equals("IMAGE")) {
									dto.addProperty(
											Long.parseLong(qa.getQuestionID()),
											qa.getValue() != null ? qa
													.getValue() : "");
								}
							}
						}
					}
				}
				dtoList.add(dto);
			}
			resp.setRecordData(dtoList);
		}
		resp.setCursor(cursor);
		return resp;
	}

	/**
	 * writes response as a JSON string
	 */
	@Override
	protected void writeOkResponse(RestResponse resp) throws Exception {
		getResponse().setStatus(200);
		RecordDataResponse rdResp = (RecordDataResponse) resp;
		JSONObject result = new JSONObject(rdResp, true);
		JSONArray arr = result.getJSONArray("recordData");
		if (arr != null) {
			for (int i = 0; i < arr.length(); i++) {
				((JSONObject) arr.get(i)).put("questionIds", rdResp
						.getRecordData().get(i).getQuestionIds());

				((JSONObject) arr.get(i)).put("answerValues", rdResp
						.getRecordData().get(i).getAnswerValues());
			}
		}
		getResponse().getWriter().println(result.toString());
	}
}
