package app.mathjourney.content;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Content is validated before Flyway migrates the learner's database (AD-5, AD-17): bad content
 * must refuse to start without touching her data.
 */
@Configuration(proxyBeanMethods = false)
class ContentBeforeFlywayConfiguration {

	@Bean
	static FlywayDependsOnContentCatalog flywayDependsOnContentCatalog() {
		return new FlywayDependsOnContentCatalog();
	}

	static final class FlywayDependsOnContentCatalog extends AbstractDependsOnBeanFactoryPostProcessor {

		FlywayDependsOnContentCatalog() {
			super(FlywayMigrationInitializer.class, ContentCatalog.class);
		}

	}

}
