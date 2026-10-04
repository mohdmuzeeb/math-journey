package app.mathjourney.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Error bodies are ProblemDetails, unknown {@code /api/**} paths are 404s, and every other
 * unknown path without a file extension falls back to {@code index.html} (AD-1). Uses the
 * test {@code static/} under {@code src/test/resources}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(ErrorAndSpaRoutingTest.FailingController.class)
@ExtendWith(OutputCaptureExtension.class)
class ErrorAndSpaRoutingTest {

	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unknownApiPathIsProblemDetail404() throws Exception {
		this.mockMvc.perform(get("/api/nope"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void unknownConceptIsProblemDetail404NamingTheId() throws Exception {
		this.mockMvc.perform(get("/api/concepts/nope"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value(containsString("nope")));
	}

	@Test
	void spaDeepLinksGetIndexHtml() throws Exception {
		for (String path : new String[] { "/map", "/land/numbers" }) {
			this.mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<div id=\"root\">")));
		}
	}

	@Test
	void clientRouteWithDottedConceptIdGetsIndexHtml() throws Exception {
		this.mockMvc.perform(get("/concept/8.EE.7-two-step-equations"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<div id=\"root\">")));
	}

	@Test
	void rootServesIndexHtml() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/")).andReturn();
		// "/" is served either directly or through Boot's welcome-page forward to index.html.
		String forwarded = result.getResponse().getForwardedUrl();
		if (forwarded != null) {
			assertThat(forwarded).isEqualTo("index.html");
			this.mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<div id=\"root\">")));
		}
		else {
			assertThat(result.getResponse().getStatus()).isEqualTo(200);
			assertThat(result.getResponse().getContentAsString()).contains("<div id=\"root\">");
		}
	}

	@Test
	void realAssetIsServedAsItself() throws Exception {
		this.mockMvc.perform(get("/assets/app.js"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("test asset")))
			.andExpect(content().string(not(containsString("<div id=\"root\">"))));
	}

	@Test
	void missingAssetIs404NotIndexHtml() throws Exception {
		this.mockMvc.perform(get("/assets/missing.js"))
			.andExpect(status().isNotFound())
			.andExpect(content().string(not(containsString("<div id=\"root\">"))));
	}

	@Test
	void unexpectedFailureIsGeneric500ProblemDetail(CapturedOutput output) throws Exception {
		this.mockMvc.perform(get("/api/test-failure"))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value(ApiExceptionHandler.GENERIC_DETAIL))
			.andExpect(content().string(not(containsString(FailingController.SECRET))))
			.andExpect(content().string(not(containsString("RuntimeException"))))
			.andExpect(content().string(not(containsString("at app.mathjourney"))));
		assertThat(output).contains("[web] Unexpected error handling GET /api/test-failure");
		assertThat(output).contains(FailingController.SECRET);
	}

	@Test
	void responseStatusExceptionKeepsItsStatusAndReason(CapturedOutput output) throws Exception {
		this.mockMvc.perform(get("/api/test-conflict"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("taken"));
		assertThat(output).doesNotContain("[web] Unexpected error");
	}

	@Test
	void responseStatusExceptionWithoutReasonHasNoDetail() throws Exception {
		this.mockMvc.perform(get("/api/test-gone"))
			.andExpect(status().isGone())
			.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(410))
			.andExpect(jsonPath("$.detail").doesNotExist());
	}

	/** Test-only: nested in a test class, so other tests' component scans and the OpenAPI export skip it. */
	@RestController
	static class FailingController {

		static final String SECRET = "internal-secret-message";

		@GetMapping("/api/test-failure")
		String fail() {
			throw new RuntimeException(SECRET);
		}

		@GetMapping("/api/test-conflict")
		String conflict() {
			throw new TakenException();
		}

		@GetMapping("/api/test-gone")
		String gone() {
			throw new GoneException();
		}

	}

	@ResponseStatus(code = HttpStatus.CONFLICT, reason = "taken")
	static class TakenException extends RuntimeException {

	}

	@ResponseStatus(HttpStatus.GONE)
	static class GoneException extends RuntimeException {

	}

}
