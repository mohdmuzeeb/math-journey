package app.mathjourney.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LearnerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsDefaultProfile() throws Exception {
		this.mockMvc.perform(get("/api/learner"))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{ "onboardingStage": "journey", "settings": { "sound": true, "companionName": "Lumi" } }
					""", JsonCompareMode.STRICT));
	}

}
