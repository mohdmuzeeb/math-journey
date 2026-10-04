package app.mathjourney.learner;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores {@link OnboardingStage} as its kebab-case value. */
@Converter
class OnboardingStageConverter implements AttributeConverter<OnboardingStage, String> {

	@Override
	public String convertToDatabaseColumn(OnboardingStage stage) {
		return (stage != null) ? stage.value() : null;
	}

	@Override
	public OnboardingStage convertToEntityAttribute(String value) {
		return (value != null) ? OnboardingStage.fromValue(value) : null;
	}

}
