package org.waterforpeople.mapping.app.gwt.client.accesspoint;

import java.util.ArrayList;
import java.util.Date;

import com.gallatinsystems.framework.gwt.dto.client.BaseDto;

public class AccessPointScoreDetailDto extends BaseDto{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3858867918094001048L;
	private Long accessPointId = null;
	private Integer score = null;
	private ArrayList<String> scoreComputationItems = null;
	private String status = null;
	private Date computationDate = null;

	public Long getAccessPointId() {
		return accessPointId;
	}

	public void setAccessPointId(Long accessPointId) {
		this.accessPointId = accessPointId;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public ArrayList<String> getScoreComputationItems() {
		return scoreComputationItems;
	}

	public void setScoreComputationItems(ArrayList<String> scoreComputationItems) {
		this.scoreComputationItems = scoreComputationItems;
	}

	public void addScoreComputationItem(String item) {
		if (scoreComputationItems == null) {
			scoreComputationItems = new ArrayList<String>();
		}
		scoreComputationItems.add(item);

	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setComputationDate(Date computationDate) {
		this.computationDate = computationDate;
	}

	public Date getComputationDate() {
		return computationDate;
	}

}