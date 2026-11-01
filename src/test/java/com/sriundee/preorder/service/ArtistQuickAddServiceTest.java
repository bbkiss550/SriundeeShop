package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sriundee.preorder.entity.Artist;
import com.sriundee.preorder.entity.Group;
import com.sriundee.preorder.repository.ArtistRepository;
import com.sriundee.preorder.repository.GroupRepository;
import com.sriundee.preorder.service.ArtistQuickAddService.ArtistAddException;

@ExtendWith(MockitoExtension.class)
class ArtistQuickAddServiceTest {
    @Mock ArtistRepository artists;
    @Mock GroupRepository groups;
    @InjectMocks ArtistQuickAddService service;
    void activeGroup() {
        Group group = new Group(); group.setId(2); group.setDelete("A");
        when(groups.findById(2)).thenReturn(Optional.of(group));
    }
    @Test void createsAndReturnsArtistForImmediateSelection() {
        activeGroup(); when(artists.lockArtistCreation()).thenReturn(true);
        when(artists.findByDeleteOrderByNameAsc("A")).thenReturn(List.of());
        when(artists.save(any())).thenAnswer(call -> { Artist a = call.getArgument(0); a.setId(8); return a; });
        Artist result = service.add("  New   Artist  ", 2);
        assertThat(result.getId()).isEqualTo(8);
        assertThat(result.getName()).isEqualTo("New Artist");
        assertThat(result.getGroup()).isEqualTo(2);
        assertThat(result.getDelete()).isEqualTo("A");
    }
    @Test void reusesSameArtistInSameGroup() {
        activeGroup(); when(artists.lockArtistCreation()).thenReturn(true);
        Artist a = new Artist(); a.setName("New Artist"); a.setGroup(2);
        when(artists.findByDeleteOrderByNameAsc("A")).thenReturn(List.of(a));
        assertThat(service.add("new-artist", 2)).isSameAs(a);
        verify(artists, never()).save(any());
    }
    @Test void rejectsInvalidNamesBeforeDatabaseAccess() {
        for (String name : List.of("", "   ", "<b>Artist</b>", "a".repeat(256)))
            assertThatThrownBy(() -> service.add(name, 2)).isInstanceOf(ArtistAddException.class);
        verifyNoInteractions(artists, groups);
    }
    @Test void requiresActiveGroup() {
        when(groups.findById(2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.add("Artist", 2)).isInstanceOf(ArtistAddException.class);
        verifyNoInteractions(artists);
    }
    @Test void concurrentCreationCanBeRetriedWithoutInsertingDuplicate() {
        activeGroup(); when(artists.lockArtistCreation()).thenReturn(false);
        assertThatThrownBy(() -> service.add("Artist", 2)).isInstanceOfSatisfying(ArtistAddException.class,
            e -> assertThat(e.status()).isEqualTo(409));
        verify(artists, never()).save(any());
    }
}
