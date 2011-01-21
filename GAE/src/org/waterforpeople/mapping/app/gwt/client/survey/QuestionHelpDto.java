package org.waterforpeople.mapping.app.gwt.client.survey;

import com.gallatinsystems.framework.gwt.dto.client.BaseDto;

public class QuestionHelpDto extends BaseDto {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1563528591253495401L;
	private String text;
	private String resourceUrl;
	private Type type;

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getResourceUrl() {
		return resourceUrl;
	}

	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}

	public enum Type {
		PHOTO, VIDEO, TEXT, ACTIVITY
	}
}
