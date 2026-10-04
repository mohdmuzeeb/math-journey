package app.mathjourney.learner;

import java.util.Arrays;

/**
 * Where the learner is in first-run onboarding (AD-15). React routes by this stage alone.
 * The kebab-case {@link #value()} is what the database and the API contract hold.
 */
public enum OnboardingStage {

	WELCOME("welcome"),

	PREREQ_CHECK("prereq-check"),

	JOURNEY("journey");

	private final String value;

	OnboardingStage(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	public static OnboardingStage fromValue(String value) {
		return Arrays.stream(values())
			.filter((stage) -> stage.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown onboarding stage '" + value + "'"));
	}

}
