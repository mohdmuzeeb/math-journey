package app.mathjourney.learner;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The single learner profile row of this installation. Never leaves the {@code learner} module. */
@Entity
@Table(name = "learner_profile")
class LearnerProfile {

	@Id
	private UUID id;

	@Convert(converter = OnboardingStageConverter.class)
	@Column(name = "onboarding_stage", nullable = false, length = 20)
	private OnboardingStage onboardingStage;

	@Column(name = "sound", nullable = false)
	private boolean sound;

	@Column(name = "companion_name", nullable = false, length = 40)
	private String companionName;

	protected LearnerProfile() {
	}

	UUID getId() {
		return this.id;
	}

	OnboardingStage getOnboardingStage() {
		return this.onboardingStage;
	}

	boolean isSound() {
		return this.sound;
	}

	String getCompanionName() {
		return this.companionName;
	}

}
