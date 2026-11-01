package com.sriundee.preorder.service;

import java.text.Normalizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sriundee.preorder.entity.Artist;
import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;

@Service
public class ArtistQuickAddService {
    private final ArtistRepository artists;
    private final GroupRepository groups;
    public ArtistQuickAddService(ArtistRepository artists, GroupRepository groups) {
        this.artists = artists; this.groups = groups;
    }
    public List<Group> groups() { return groups.getDropdownData(); }

    @Transactional
    public Artist add(String requestedName, Integer groupId) {
        String name = requestedName == null ? "" : Normalizer.normalize(requestedName, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
        if (name.isBlank() || name.length() > 255 || WebsiteQuickAddService.normalizedName(name).isEmpty()
                || name.matches("(?s).*[<>\\p{Cntrl}].*")) {
            throw new ArtistAddException(400, "กรอกชื่อศิลปิน 1–255 ตัวอักษร โดยไม่ใช้แท็ก HTML หรืออักขระควบคุม");
        }
        if (groupId == null || groups.findById(groupId).filter(g -> "A".equals(g.getDelete())).isEmpty()) {
            throw new ArtistAddException(400, "กรุณาเลือกสังกัดที่ใช้งานอยู่");
        }
        if (!Boolean.TRUE.equals(artists.lockArtistCreation())) {
            throw new ArtistAddException(409, "กำลังเพิ่มศิลปินอีกคำขอหนึ่ง กรุณาลองอีกครั้ง");
        }
        String key = WebsiteQuickAddService.normalizedName(name);
        for (Artist artist : artists.findByDeleteOrderByNameAsc("A")) {
            if (groupId.equals(artist.getGroup()) && key.equals(WebsiteQuickAddService.normalizedName(artist.getName()))) return artist;
        }
        Artist artist = new Artist();
        artist.setName(name); artist.setGroup(groupId); artist.setDelete("A"); artist.setLogo("");
        return artists.save(artist);
    }
    public static class ArtistAddException extends RuntimeException {
        private final int status;
        public ArtistAddException(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
    }
}
