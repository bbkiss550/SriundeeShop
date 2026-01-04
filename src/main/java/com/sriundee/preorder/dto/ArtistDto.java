package com.sriundee.preorder.dto;

public class ArtistDto {
    private String artistName;
    private String groupId;

    public ArtistDto() {
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "ArtistDto{" +
                "artistName='" + artistName + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }
}