package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.sriundee.preorder.entity.Type;
import com.sriundee.preorder.repository.TypeRepository;

class TypeQuickAddServiceTest {
    private final TypeRepository repository = mock(TypeRepository.class);
    private final TypeQuickAddService service = new TypeQuickAddService(repository);

    @Test void reusesActiveMatchingTypeInsteadOfCreatingDuplicate() {
        Type existing = new Type(); existing.setId(4); existing.setName("Album Pre-order"); existing.setDelete("A");
        when(repository.lockTypeCreation()).thenReturn(true);
        when(repository.getDropdownData()).thenReturn(List.of(existing));
        assertThat(service.add("  ALBUM   PRE-ORDER ")).isSameAs(existing);
        verify(repository, never()).save(any());
    }
    @Test void validatesBeforeWritingAndRejectsConcurrentCreation() {
        assertThatThrownBy(() -> service.add("<script>")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(" ")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
        when(repository.lockTypeCreation()).thenReturn(false);
        assertThatThrownBy(() -> service.add("ALBUM")).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
    @Test void createsNormalizedActiveType() {
        when(repository.lockTypeCreation()).thenReturn(true);
        when(repository.getDropdownData()).thenReturn(List.of());
        when(repository.save(any(Type.class))).thenAnswer(call -> call.getArgument(0));
        Type type=service.add("  NEW   TYPE  ");
        assertThat(type.getName()).isEqualTo("NEW TYPE");
        assertThat(type.getDelete()).isEqualTo("A");
    }
}
