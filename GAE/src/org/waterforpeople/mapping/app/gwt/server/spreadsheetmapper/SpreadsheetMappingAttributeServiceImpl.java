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

package org.waterforpeople.mapping.app.gwt.server.spreadsheetmapper;



import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import org.waterforpeople.mapping.adapter.SpreadsheetAccessPointAdapter;
import org.waterforpeople.mapping.app.gwt.client.spreadsheetmapper.MappingDefinitionColumnContainer;
import org.waterforpeople.mapping.app.gwt.client.spreadsheetmapper.MappingSpreadsheetColumnToAttribute;
import org.waterforpeople.mapping.app.gwt.client.spreadsheetmapper.MappingSpreadsheetDefinition;
import org.waterforpeople.mapping.app.gwt.client.spreadsheetmapper.SpreadsheetMappingAttributeService;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionDto.QuestionType;
import org.waterforpeople.mapping.helper.SpreadsheetMappingAttributeHelper;

import com.gallatinsystems.common.data.spreadsheet.GoogleSpreadsheetAdapter;
import com.gallatinsystems.common.data.spreadsheet.domain.ColumnContainer;
import com.gallatinsystems.common.data.spreadsheet.domain.RowContainer;
import com.gallatinsystems.common.data.spreadsheet.domain.SpreadsheetContainer;
import com.gallatinsystems.survey.dao.QuestionDao;
import com.gallatinsystems.survey.dao.QuestionGroupDao;
import com.gallatinsystems.survey.dao.SurveyGroupDAO;
import com.gallatinsystems.survey.domain.Question;
import com.gallatinsystems.survey.domain.QuestionGroup;
import com.gallatinsystems.survey.domain.QuestionOption;
import com.gallatinsystems.survey.domain.Survey;
import com.gallatinsystems.survey.domain.SurveyGroup;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class SpreadsheetMappingAttributeServiceImpl extends
		RemoteServiceServlet implements SpreadsheetMappingAttributeService {
	private static final Logger log = Logger
			.getLogger(SpreadsheetMappingAttributeServiceImpl.class.getName());
	private QuestionGroupDao questionGroupDao;

	private static final long serialVersionUID = 7708378583408245812L;

	public SpreadsheetMappingAttributeServiceImpl() {
		questionGroupDao = new QuestionGroupDao();
	}

	private String getSessionTokenFromSession() throws Exception {
		HttpSession session = this.getThreadLocalRequest().getSession();

		String token = (String) session.getAttribute("sessionToken");
		if (token != null)
			return token;
		else
			throw new Exception(
					"Invalid or missing Google Spreadsheet Session Token");
	}

	private PrivateKey getPrivateKeyFromSession() throws Exception {
		HttpSession session = this.getThreadLocalRequest().getSession();
		PrivateKey key = (PrivateKey) session.getAttribute("privateKey");
		if (key != null)
			return key;
		else
			throw new Exception("Invalid or missing Private Key");
	}

	@Override
	public ArrayList<String> listObjectAttributes(String objectNames) {
		return SpreadsheetMappingAttributeHelper.listObjectAttributes();
	}

	@Override
	public ArrayList<String> listSpreadsheetColumns(String spreadsheetName) {

		log.info("listingSpreadsheetColumns");

		try {
			SpreadsheetMappingAttributeHelper helper = new SpreadsheetMappingAttributeHelper(
					getSessionTokenFromSession(), getPrivateKeyFromSession());
			return helper.listSpreadsheetColumns(spreadsheetName);
		} catch (Exception e) {
			// need to reauth
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ArrayList<String> listSpreadsheets() throws Exception {

		return this.listSpreadsheetsFromFeed(null);
	}

	@Override
	public void saveSpreadsheetMapping(MappingSpreadsheetDefinition mapDef) {

		// TODO change to return status of save or errors if there are any
		SpreadsheetMappingAttributeHelper helper;
		try {
			helper = new SpreadsheetMappingAttributeHelper(
					getSessionTokenFromSession(), getPrivateKeyFromSession());
			helper.saveSpreadsheetMapping(copyToCanonicalObject(mapDef));
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not save spreadsheet mapping");
		}
		// convert to domain object from dto

	}

	@Override
	public String processSpreadsheet(MappingSpreadsheetDefinition mapDef) {
		try {
			new SpreadsheetAccessPointAdapter(getSessionTokenFromSession(),
					getPrivateKeyFromSession())
					.processSpreadsheetOfAccessPoints(mapDef
							.getSpreadsheetURL());
			return new String("Processed Successfully");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not save spreadsheet", e);
			return "Could not save spreadsheet : " + e.getMessage();
		}
	}

	private org.waterforpeople.mapping.domain.MappingSpreadsheetDefinition copyToCanonicalObject(
			MappingSpreadsheetDefinition mapDef) {
		org.waterforpeople.mapping.domain.MappingSpreadsheetDefinition canonicalMapDef = new org.waterforpeople.mapping.domain.MappingSpreadsheetDefinition();
		canonicalMapDef.setSpreadsheetURL(mapDef.getSpreadsheetURL());
		for (MappingSpreadsheetColumnToAttribute entry : mapDef.getColumnMap()) {
			MappingSpreadsheetColumnToAttribute attribute = entry;
			org.waterforpeople.mapping.domain.MappingSpreadsheetColumnToAttribute canonicalAttribute = new org.waterforpeople.mapping.domain.MappingSpreadsheetColumnToAttribute();
			canonicalAttribute.setSpreadsheetColumn(attribute
					.getSpreadsheetColumn());
			canonicalAttribute.setObjectAttribute(attribute
					.getObjectAttribute());
			canonicalAttribute.setFormattingRule(attribute.getFormattingRule());
			canonicalMapDef.addColumnToMap(canonicalAttribute);
		}
		return canonicalMapDef;
	}

	private MappingSpreadsheetDefinition copyToDTOObject(
			org.waterforpeople.mapping.domain.MappingSpreadsheetDefinition canonicalMapDef) {

		MappingSpreadsheetDefinition mapSpreadsheetDTO = new MappingSpreadsheetDefinition();
		if (canonicalMapDef.getKey() != null)
			mapSpreadsheetDTO.setKeyId(canonicalMapDef.getKey().getId());
		mapSpreadsheetDTO
				.setSpreadsheetURL(canonicalMapDef.getSpreadsheetURL());
		if (canonicalMapDef.getColumnMap() != null) {
			for (org.waterforpeople.mapping.domain.MappingSpreadsheetColumnToAttribute entry : canonicalMapDef
					.getColumnMap()) {
				org.waterforpeople.mapping.domain.MappingSpreadsheetColumnToAttribute colAttr = entry;
				MappingSpreadsheetColumnToAttribute colAttrDTO = new MappingSpreadsheetColumnToAttribute();
				// colAttrDTO.setKeyId(colAttr.getKey().getId());
				colAttrDTO.setSpreadsheetColumn(colAttr.getSpreadsheetColumn());
				colAttrDTO.setObjectAttribute(colAttr.getObjectAttribute());
				log.info(colAttr.getSpreadsheetColumn() + "|"
						+ colAttr.getObjectAttribute());
				mapSpreadsheetDTO.addColumnToMap(colAttrDTO);
			}
		}
		return mapSpreadsheetDTO;
	}

	@Override
	public ArrayList<String> listSpreadsheetsFromFeed(String feedURL)
			throws Exception {

		if (feedURL == null) {
			return new SpreadsheetMappingAttributeHelper(
					getSessionTokenFromSession(), getPrivateKeyFromSession())
					.listSpreadsheets("http://spreadsheets.google.com/feeds/spreadsheets/private/full");
		}
		return null;
	}

	@Override
	public MappingDefinitionColumnContainer getMappingSpreadsheetDefinition(
			String spreadsheetName) {
		MappingDefinitionColumnContainer mapdefCC = new MappingDefinitionColumnContainer();
		org.waterforpeople.mapping.domain.MappingSpreadsheetDefinition mapDef;
		try {
			mapDef = new SpreadsheetMappingAttributeHelper(
					getSessionTokenFromSession(), getPrivateKeyFromSession())
					.getMappingSpreadsheetDefinition(spreadsheetName);
			if (mapDef != null) {
				mapdefCC.setMapDef(copyToDTOObject(mapDef));
				mapdefCC
						.setSpreadsheetColsList(listSpreadsheetColumns(spreadsheetName));
				return mapdefCC;

			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not list mapping definitions", e);
		}

		return null;
	}

	@Deprecated
	public void processSurveySpreadsheetAsync(String tokenString,
			PrivateKey key, String spreadsheetName, int startRow, Long groupId) {

		try {
			org.apache.commons.codec.binary.Base64 b64encoder = new org.apache.commons.codec.binary.Base64();
			log
					.info("Inside processSurveySpreadsheetAsync prior to GoogleSpreadsheet Adapter call");
			byte[] encodedKey = b64encoder.encode(key.getEncoded());
			GoogleSpreadsheetAdapter gsa = new GoogleSpreadsheetAdapter(
					tokenString, key);
			log.info("Just made spreadsheet call");
			SpreadsheetContainer sc = gsa
					.getSpreadsheetContents(spreadsheetName);
			log.info("Just got spreadsheet contents");

			sc.setSpreadsheetName(spreadsheetName);
			RowContainer rowTitle = sc.getRowContainerList().get(1);
			String sgName = rowTitle.getColumnContainersList().get(0)
					.getColContents();
			if (sgName == null) {
				sgName = "Default";
			} else
				log.info("Begining to process: " + sgName);

			if (startRow == -1) {
				setDependencies(sc);
			} else if (startRow == -2) {
				log.info("Getting the question groups");
				// create the survey and all groups. We'll take care of the
				// questions in another iteration
				SurveyGroupDAO sgDao = new SurveyGroupDAO();
				SurveyGroup sg = null;
				SurveyGroup sgFound = sgDao.findBySurveyGroupName(sgName);
				HashMap<String, QuestionGroup> groupMap = new HashMap<String, QuestionGroup>();
				if (sgFound != null) {
					sg = sgFound;
				} else {
					sg = new SurveyGroup();
				}

				sg.setCode(sgName);
				Survey survey = new Survey();
				sg.addSurvey(survey);
				// iterate over entire sheet to get the group names
				for (int i = 0; i < sc.getRowContainerList().size(); i++) {
					RowContainer row = sc.getRowContainerList().get(i);
					ArrayList<ColumnContainer> ccl = row
							.getColumnContainersList();
					for (ColumnContainer cc : ccl) {
						String colName = cc.getColName();
						String colContents = cc.getColContents();
						if (colContents != null) {
							if (colName.toLowerCase().equals("survey")) {
								survey.setName(colContents.trim());
							} else if (colName.toLowerCase().equals(
									"questiongroup")) {

								QuestionGroup group = groupMap.get(colContents
										.trim());
								if (group == null) {
									group = new QuestionGroup();
									group.setCode(colContents.trim());
									group.setOrder(i);
									survey.addQuestionGroup(i, group);
									groupMap.put(colContents.trim(), group);
								}
							}
						}
					}
				}
				sgDao.save(sg);
				log.info("Done getting the question groups");
				// send the message to start question processing
				log.info("Dispatching async calls for: " + spreadsheetName);
				sendSurveyProcessingMessage(spreadsheetName, 0, tokenString,
						encodedKey, key.getAlgorithm());
			} else {
				// now process the questions
				String currentPath = null;
				HashMap<String, QuestionGroup> groupMap = new HashMap<String, QuestionGroup>();

				int count = 0;
				int i = 0;

				for (count = startRow; count < sc.getRowContainerList().size()
						&& i < 10; count++) {
					i++;
					RowContainer row = sc.getRowContainerList().get(count);
					TreeMap<Integer, QuestionOption> optionMap = new TreeMap<Integer, QuestionOption>();
					QuestionGroup targetQG = null;
					ArrayList<ColumnContainer> ccl = row
							.getColumnContainersList();
					Question q = new Question();
					for (ColumnContainer cc : ccl) {
						String colName = cc.getColName();
						String colContents = cc.getColContents();
						if (colContents != null) {
							if (colName.toLowerCase().equals("survey")) {
								if (currentPath == null) {
									currentPath = sgName + "/" + colContents;
								}
							} else if (colName.toLowerCase().equals(
									"questiongroup")) {
								String groupName = colContents.trim();
								targetQG = groupMap.get(groupName);
								if (targetQG == null) {
									targetQG = questionGroupDao.getByPath(
											groupName, currentPath);
									if (targetQG != null) {
										groupMap.put(groupName, targetQG);
									}
								}
							} else if (colName.toLowerCase().equals("question")) {
								if (colContents.trim().length() > 500)
									q.setText(colContents.trim().substring(0,
											500));
								else
									q.setText(colContents.trim());
							} else if (colName.toLowerCase().equals(
									"questiontype")) {
								if (colContents.toLowerCase().equals(
										"FREE".toLowerCase())) {
									q.setType(Question.Type.FREE_TEXT);
								} else if (colContents.toLowerCase().equals(
										"GEO".toLowerCase())) {
									q.setType(Question.Type.GEO);
								} else if (colContents.toLowerCase().equals(
										"NUMBER".toLowerCase())) {
									q.setType(Question.Type.NUMBER);
								} else if (colContents.toLowerCase().equals(
										"OPTION".toLowerCase())) {
									q.setType(Question.Type.OPTION);
								} else if (colContents.toLowerCase().equals(
										"PHOTO".toLowerCase())) {
									q.setType(Question.Type.PHOTO);
								} else if (colContents.toLowerCase().equals(
										"SCAN".toLowerCase())) {
									q.setType(Question.Type.SCAN);
								} else if (colContents.toLowerCase().equals(
										"VIDEO".toLowerCase())) {
									q.setType(Question.Type.VIDEO);
								} else if (colContents
										.equalsIgnoreCase("STRENGTH")) {
									q.setType(Question.Type.STRENGTH);
								}
							} else if (colName.toLowerCase().equals(
									"Options".toLowerCase())
									&& (q.getType().toString().equals(QuestionType.OPTION.toString()) ||
										q.getType().toString().equals(QuestionType.STRENGTH.toString()))) {
								String[] splitColContents = colContents.trim()
										.split(";");
								int optCount = 1;
								for (String item : splitColContents) {
									String[] optionParts = item.trim().split(
											"\\|");
									if (optionParts.length == 2) {
										String optionVal = optionParts[0]
												.trim();
										String text = optionParts[1].trim();
										text = text.replaceAll("\\n", " ");
										QuestionOption qo = new QuestionOption();
										qo.setCode(optionVal);
										qo.setText(text);
										optionMap.put(optCount++, qo);
									}
								}
							} else if ((colName.equals("AllowOther") || colName
									.equals("AllowMultiple"))
									&& (q.getType().toString().equals(QuestionType.OPTION.toString()) ||
										q.getType().toString().equals(QuestionType.STRENGTH.toString()))) {
								if (colName.equals("AllowOther")) {
									q.setAllowOtherFlag(new Boolean(colContents
											.toLowerCase()));
								}
								if (colName.equals("AllowMultiple")) {
									q.setAllowMultipleFlag(new Boolean(
											colContents.toLowerCase()));
								}
							} else if (colName.equalsIgnoreCase("QuestionID")) {
								q.setReferenceId(colContents.trim());
							} else if (colName.equalsIgnoreCase("Order")) {
								if (colContents.trim().length() > 0) {
									try {
										q.setOrder(Integer.parseInt(colContents
												.trim()));
									} catch (Exception e) {
										log
												.warning("Order column contains non integer value");
									}
								}
							}
						}
					}
					if (q.getType().toString().equals(QuestionType.OPTION.toString())
							|| q.getType().toString().equals(QuestionType.STRENGTH.toString())) {
						q.setQuestionOptionMap(optionMap);

					}
					if (q.getOrder() == null) {
						q.setOrder(count);
					}
					targetQG.addQuestion(q.getOrder(), q);
				}

				QuestionDao qDao = new QuestionDao();
				for (QuestionGroup group : groupMap.values()) {
					Long curGroupId = group.getKey().getId();
					for (Entry<Integer, Question> qEntry : group
							.getQuestionMap().entrySet()) {
						qDao.save(qEntry.getValue(), curGroupId);
					}
				}

				if (count < sc.getRowContainerList().size()) {

					sendSurveyProcessingMessage(sc.getSpreadsheetName(), count,
							tokenString, encodedKey, key.getAlgorithm());
				} else {
					sendSurveyProcessingMessage(sc.getSpreadsheetName(), -1,
							tokenString, encodedKey, key.getAlgorithm());
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not process spreadsheet", e);
		}
	}

	/**
	 * 
	 * sends a survey processing message to the task queue
	 * 
	 */
	private void sendSurveyProcessingMessage(String spreadsheetName,
			int startRow, String token, byte[] key, String keySpec) {
		Queue importQueue = QueueFactory.getQueue("spreadsheetImport");
		importQueue.add(TaskOptions.Builder.withUrl("/app_worker/sheetimport").param("identifier",
				spreadsheetName).param("type", "Survey").param("action",
				"processFile").param("startRow", startRow + "").param(
				"sessionToken", token).param("privateKey", key).param(
				"keySpec", keySpec));
	}

	@Override
	public void processSurveySpreadsheet(String spreadsheetName, int startRow,
			Long groupId) {
		try {
			processSurveySpreadsheetAsync(getSessionTokenFromSession(),
					getPrivateKeyFromSession(), spreadsheetName, startRow,
					groupId);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not process sheet: " + e, e);
		}
	}

	@Deprecated
	private void setDependencies(SpreadsheetContainer sc) {
		// HashMap<Question, QuestionDependency> dependencyMap = new
		// HashMap<Question, QuestionDependency>();
		List<Question> questionsWithDependencies = new ArrayList<Question>();
		ArrayList<Question> spreadsheetQuestions = new ArrayList<Question>();
		for (RowContainer row : sc.getRowContainerList()) {
			Question q = new Question();
			for (ColumnContainer cc : row.getColumnContainersList()) {
				String colName = cc.getColName();
				String colContents = cc.getColContents();
				if (colContents != null) {
					if (colName.toLowerCase().equals("question")) {
						if (colContents.trim().length() > 500)
							q.setText(colContents.trim().substring(0, 500));
						else
							q.setText(colContents.trim());
					} else if ("QuestionID".equalsIgnoreCase(colName)) {
						q.setReferenceId(colContents);
					} else if (colName.equalsIgnoreCase("DependQuestion")) {
						if (colContents != null
								&& colContents.trim().length() > 0) {
							String[] parts = colContents.trim().split("\\|");
							if (parts != null && parts.length >= 2) {
								try {
									q.setDependentQuestionId(Long
											.parseLong(parts[0].trim()));
									q.setDependentQuestionAnswer(parts[1]);
									questionsWithDependencies.add(q);
								} catch (Exception e) {
									log
											.log(
													Level.SEVERE,
													"Can't set dependency. The question number in the dependency column isn't an integer");
								}
							}
						}
					}
				}
			}
			spreadsheetQuestions.add(q);
		}
		// now interate over the dependencies and update the keys from the
		// reference id to the datastore id
		if (questionsWithDependencies.size() > 0) {
			QuestionDao qDao = new QuestionDao();
			for (Question q : questionsWithDependencies) {

				Question savedParent = qDao.findByReferenceId(q
						.getReferenceId());
				Question savedChild = qDao.findByReferenceId(q
						.getDependentQuestionId().toString());
				if (savedParent != null) {
					if (savedParent != null && savedChild != null) {
						q.setDependentQuestionId(savedChild.getKey().getId());
						qDao.save(q);
					}
				} else {
					log.log(Level.SEVERE,
							"Couldn't find the parent question for the dependency: "
									+ q.getText());
				}
			}
		}
	}
}
