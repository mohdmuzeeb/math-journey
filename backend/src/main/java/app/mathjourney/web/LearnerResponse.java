package app.mathjourney.web;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

/** The learner profile as the API returns it. React routes by {@code onboardingStage} alone (AD-15). */
@Schema(description = "The learner profile")
record LearnerResponse(
		@Schema(requiredMode = REQUIRED, allowableValues = { "welcome", "prereq-check", "journey" })
		String onboardingStage,
		@Schema(requiredMode = REQUIRED) LearnerSettingsResponse settings) {
}

@Schema(description = "The learner's settings")
record LearnerSettingsResponse(
		@Schema(requiredMode = REQUIRED) boolean sound,
		@Schema(requiredMode = REQUIRED) String companionName) {
}
