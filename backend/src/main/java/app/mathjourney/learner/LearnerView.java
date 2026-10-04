package app.mathjourney.learner;

/** The learner profile as other modules see it. */
public record LearnerView(OnboardingStage onboardingStage, boolean sound, String companionName) {

}
