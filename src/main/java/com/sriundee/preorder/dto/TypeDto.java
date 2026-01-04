package com.sriundee.preorder.dto;

public class TypeDto {
    private String typeName;
    
    public TypeDto() {
    }

	public String getTypeName() {
		return typeName;
	}

	public void setTypetName(String typeName) {
		this.typeName = typeName;
	}

    @Override
    public String toString() {
        return "TypeDto{" +
                "typeName='" + typeName + '\'' +
                '}';
    }
}
