package com.huawei.modellite.repository.common.util;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple Mockito verification test to confirm Mockito is properly configured.
 */
class MockitoVerificationTest {

    @Test
    void shouldMockListAndVerifyInteraction() {
        // Given
        List<String> mockedList = mock(List.class);

        // When
        mockedList.add("test-element");
        when(mockedList.size()).thenReturn(1);
        when(mockedList.get(0)).thenReturn("test-element");

        // Then
        assertEquals(1, mockedList.size());
        assertEquals("test-element", mockedList.get(0));
        verify(mockedList, times(1)).add("test-element");
        verify(mockedList, times(1)).size();
    }
}