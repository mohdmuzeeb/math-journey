package app.mathjourney.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** A missing profile row is a server fault, answered with the generic ProblemDetail. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // the row deletion is rolled back, so the shared test database keeps its defaults
class LearnerMissingRowTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void missingProfileRowIsGeneric500() throws Exception {
		this.jdbcTemplate.update("DELETE FROM learner_profile");
		this.mockMvc.perform(get("/api/learner"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value(ApiExceptionHandler.GENERIC_DETAIL));
	}

}
