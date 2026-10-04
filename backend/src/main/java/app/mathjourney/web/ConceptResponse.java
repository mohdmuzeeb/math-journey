package app.mathjourney.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A concept as the API returns it. Item {@code payload} and {@code answer} stay opaque JSON:
 * their shape belongs to the content schemas, never to this contract (AD-7).
 */
@Schema(description = "A concept and its items")
record ConceptResponse(
		@Schema(requiredMode = REQUIRED) String id,
		@Schema(requiredMode = REQUIRED) String title,
		@Schema(requiredMode = REQUIRED) List<ConceptItemResponse> items) {
}

@Schema(description = "One activity item of a concept")
record ConceptItemResponse(
		@Schema(requiredMode = REQUIRED) String id,
		@Schema(requiredMode = REQUIRED) String kind,
		@Schema(requiredMode = REQUIRED, description = "Opaque kind-specific payload; typed only by the content schemas (AD-7)")
		Map<String, Object> payload,
		@Schema(requiredMode = REQUIRED, nullable = false, description = "Opaque canonical answer, any JSON value (AD-3)")
		Object answer) {
}
