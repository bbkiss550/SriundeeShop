package com.sriundee.preorder.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.sriundee.preorder.service.AiApiSettingsService;
import com.sriundee.preorder.service.AiApiSettingsService.View;

class AiApiSettingsControllerTest {
    final AiApiSettingsService service=mock(AiApiSettingsService.class);
    final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new AiApiSettingsController(service,mock(MenuController.class))).build();
    @Test void configIsScopedToLoggedInUserAndNeverReturnsAKey() throws Exception {
        var session=new MockHttpSession();session.setAttribute(LoginController.SESSION_USER_ID,7);
        when(service.get(7)).thenReturn(new View("OLLAMA","http://localhost:11434","model",true,false,15));
        mvc.perform(get("/settings/ai/config").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.keyConfigured").value(true)).andExpect(jsonPath("$.apiKey").doesNotExist());verify(service).get(7);
    }
    @Test void cannotAccessSettingsWithoutLogin() throws Exception {
        mvc.perform(get("/settings/ai/config")).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void saveUsesSessionIdentityEvenWhenBodyIncludesDifferentUser() throws Exception {
        var session=new MockHttpSession();session.setAttribute(LoginController.SESSION_USER_ID,7);
        mvc.perform(post("/settings/ai/config").session(session).contentType("application/json").content("{\"userId\":99,\"provider\":\"OLLAMA\",\"baseUrl\":\"http://localhost:11434\",\"model\":\"model\",\"apiKey\":\"dummy-key\",\"allowRemote\":false,\"timeoutSeconds\":15}" )).andExpect(status().isOk());
        verify(service).save(eq(7),argThat(draft->draft.apiKey().equals("dummy-key")));
    }
}
