package app.mathjourney.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ConceptControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsDemoConcept() throws Exception {
		this.mockMvc.perform(get("/api/concepts/demo-number-line"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value("demo-number-line"))
			.andExpect(jsonPath("$.items[0].id").value("demo-number-line#three-quarters"))
			.andExpect(jsonPath("$.items[0].answer").value(0.75))
			.andExpect(jsonPath("$.items[0].payload.prompt").value("Drag the point to 3/4"))
			.andExpect(jsonPath("$.items[0].payload.min").value(0))
			.andExpect(jsonPath("$.items[0].payload.max").value(2))
			.andExpect(jsonPath("$.items[0].payload.step").value(0.25))
			.andExpect(jsonPath("$.items[0].payload.target").value("3/4"))
			// the renderer does arithmetic on these, so they must arrive as JSON numbers, not strings
			.andExpect(jsonPath("$.items[0].payload.min").isNumber())
			.andExpect(jsonPath("$.items[0].payload.max").isNumber())
			.andExpect(jsonPath("$.items[0].payload.step").isNumber())
			.andExpect(jsonPath("$.items[0].payload.target").isString());
	}

	@Test
	void unknownConceptIsNotFound() throws Exception {
		this.mockMvc.perform(get("/api/concepts/nope")).andExpect(status().isNotFound());
	}

}
