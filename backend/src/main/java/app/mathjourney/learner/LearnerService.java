package app.mathjourney.learner;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The {@code learner} module's public entry point: the one learner profile of this installation. */
@Service
public class LearnerService {

	private static final Logger log = LoggerFactory.getLogger(LearnerService.class);

	private final LearnerProfileRepository repository;

	LearnerService(LearnerProfileRepository repository) {
		this.repository = repository;
	}

	/**
	 * Returns the learner profile.
	 * @throws IllegalStateException if the database does not hold exactly one profile row
	 */
	@Transactional(readOnly = true)
	public LearnerView profile() {
		List<LearnerProfile> profiles = this.repository.findAll();
		if (profiles.size() != 1) {
			log.error("[learner] Expected exactly one learner_profile row but found {}", profiles.size());
			throw new IllegalStateException("Expected exactly one learner profile, found " + profiles.size());
		}
		LearnerProfile profile = profiles.get(0);
		return new LearnerView(profile.getOnboardingStage(), profile.isSound(), profile.getCompanionName());
	}

}
