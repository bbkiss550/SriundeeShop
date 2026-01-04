package com.sriundee.preorder.dto;

public class WebsiteDto {
    private String websiteName;
    
    public WebsiteDto() {
    }

    public String getWebsiteName() {
		return websiteName;
	}

	public void setWebsiteName(String websiteName) {
		this.websiteName = websiteName;
	}

	@Override
    public String toString() {
        return "WebsiteDto{" +
                "websiteName='" + websiteName + '\'' +
                '}';
    }
}