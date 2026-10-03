package app.mathjourney.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Without the demo profile no concepts are loaded, so the demo id is a 404. */
@SpringBootTest
@AutoConfigureMockMvc
class ConceptControllerWithoutDemoTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void demoConceptIsNotFoundWithoutDemoProfile() throws Exception {
		this.mockMvc.perform(get("/api/concepts/demo-number-line")).andExpect(status().isNotFound());
	}

}
