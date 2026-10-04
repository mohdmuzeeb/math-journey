package app.mathjourney.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.mathjourney.learner.LearnerService;
import app.mathjourney.learner.LearnerView;

@RestController
@RequestMapping("/api/learner")
class LearnerController {

	private final LearnerService learnerService;

	LearnerController(LearnerService learnerService) {
		this.learnerService = learnerService;
	}

	@GetMapping
	LearnerResponse learner() {
		LearnerView view = this.learnerService.profile();
		return new LearnerResponse(view.onboardingStage().value(),
				new LearnerSettingsResponse(view.sound(), view.companionName()));
	}

}
