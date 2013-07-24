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
import org.waterforpeople.mapping.app.web.dto.RecordsMetaDto;

import com.gallatinsystems.framework.rest.AbstractRestApiServlet;
import com.gallatinsystems.framework.rest.RestRequest;
import com.gallatinsystems.framework.rest.RestResponse;
import com.gallatinsystems.metric.dao.MetricDao;
import com.gallatinsystems.metric.domain.Metric;
import com.gallatinsystems.survey.dao.QuestionDao;
import com.gallatinsystems.survey.dao.SurveyDAO;
import com.gallatinsystems.survey.domain.Question;
import com.gallatinsystems.survey.domain.Survey;
import com.gallatinsystems.surveyal.dao.SurveyedLocaleDao;
import com.gallatinsystems.surveyal.dao.SurveyedLocaleSummaryDao;
import com.gallatinsystems.surveyal.domain.SurveyalValue;
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
				resp.setRecordCount(SLSummary.getCount());
			} else {
				resp.setRecordCount(0L);
			}
			return resp;
		}

		List<SurveyedLocale> results = surveyedLocaleDao
				.listLocalesByProjectId(rdReq.getProjectId(),rdReq.getCursor());

		return convertToResponse(results, rdReq.getProjectId(), SurveyedLocaleDao.getCursor(results));
	}

	/**
	 * converts the domain objects to dtos and then installs them in a
	 * RecordDataResponse object
	 */
	protected RecordDataResponse convertToResponse(List<SurveyedLocale> slList, Long projectId,
			String cursor) {
		RecordDataResponse resp = new RecordDataResponse();
		if (slList != null) {
			List<RecordDataDto> dtoList = new ArrayList<RecordDataDto>();
			SurveyDAO sDao = new SurveyDAO();
			QuestionDao qDao = new QuestionDao();
			List<Survey> surveyList = sDao.listSurveysByProject(projectId);
			List<Question> QuestionList= new ArrayList<Question>();
			// get all the questions from all the surveys, and add them to surveyQuestionList
			if (surveyList != null && surveyList.size() > 0){
				for (Survey s : surveyList){
					List<Question> questions = qDao.listQuestionsBySurvey(s.getKey().getId());
					if (questions !=null){
						QuestionList.addAll(questions);
					}
				}
			}

			RecordsMetaDto metaDto = new RecordsMetaDto();
			MetricDao mDao = new MetricDao();
			if (QuestionList.size() > 0){
				for (Question q : QuestionList){
					if (q.getMetricId() != null){
						Metric m = mDao.getByKey(q.getMetricId());
						String mName = m != null ? m.getName() : "";
						metaDto.addItem(q.getKey().getId(), mName, q.getIncludeInList());
					}
				}
			}

			SurveyedLocaleDao slDao = new SurveyedLocaleDao();
			for (SurveyedLocale sl : slList) {
				RecordDataDto dto = new RecordDataDto();
				dto.setId(sl.getIdentifier());
				dto.setLastSDate(sl.getLastSurveyedDate());
				dto.setLat(sl.getLatitude());
				dto.setLon(sl.getLongitude());

				// for each question which has a metric, get the latest surveyalValue
				// with that surveyedLocaleId and questionId and order by time desc
				// FIXME this should be done through metrics as well, as we want to include
				// FIXME the latest value for a certain metric, regardless of which question made it
				if (QuestionList.size() > 0){
					for (Question q : QuestionList){
						// only include questions which have a metric
						if (q.getMetricId() != null){
							List<SurveyalValue> sv = slDao.listValuesByLocaleAndQuestion(sl.getKey().getId(),q.getKey().getId());
							if (sv != null && sv.size() > 0) {
								if (!sv.get(0).getQuestionType().equals("GEO")
										&& !sv.get(0).getQuestionType().equals("IMAGE")) {
									dto.addProperty(
										sv.get(0).getSurveyQuestionId(),
										sv.get(0).getStringValue() != null ? sv.get(0).getStringValue() : "");
								}
							}
						}
					}
				}
				dtoList.add(dto);
			}
			resp.setRecordData(dtoList);
			resp.setRecordsMeta(metaDto);
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
		JSONObject result = new JSONObject(rdResp, false);

		if (rdResp.getRecordCount() == null) {

			JSONArray arr = result.getJSONArray("recordData");
			if (arr != null) {
				for (int i = 0; i < arr.length(); i++) {
					((JSONObject) arr.get(i)).put("questionIds", rdResp
							.getRecordData().get(i).getQuestionIds());

					((JSONObject) arr.get(i)).put("answerValues", rdResp
							.getRecordData().get(i).getAnswerValues());
				}
			}
			JSONObject meta = result.getJSONObject("recordsMeta");
			if (meta != null){
				meta.put("questionIds", rdResp.getRecordsMeta().getQuestionIds());
				meta.put("metricNames", rdResp.getRecordsMeta().getMetricNames());
				meta.put("includeInList", rdResp.getRecordsMeta().getIncludeInList());
			}
		}
		getResponse().getWriter().println(result.toString());
	}
}
