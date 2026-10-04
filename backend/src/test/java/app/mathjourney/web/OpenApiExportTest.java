package app.mathjourney.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exports the OpenAPI document of the running app to {@code ../frontend/openapi.json}, which the
 * frontend generates its API types from (AD-7). Runs in Maven's test phase, before the frontend
 * build in prepare-package, so a DTO change reaches {@code tsc} in the same build.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiExportTest {

	/** The working directory of the backend tests is {@code backend/}. */
	static final Path TARGET = Path.of("..", "frontend", "openapi.json");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void exportsOpenApiDocument() throws Exception {
		String body = this.mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		JsonNode document = this.objectMapper.readTree(body);
		assertThat(document.at("/components/schemas/ConceptResponse").isMissingNode()).isFalse();

		String pretty = this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document) + "\n";
		Files.writeString(TARGET, pretty, StandardCharsets.UTF_8);
	}

}
