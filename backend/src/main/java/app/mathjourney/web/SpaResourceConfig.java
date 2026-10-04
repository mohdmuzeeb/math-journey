package app.mathjourney.web;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app from {@code classpath:/static/} with an SPA fallback (AD-1):
 * an existing file is served as is; an unknown {@code api/**} path or a missing file (last
 * segment ends in a static-file extension) resolves to nothing, so it becomes a 404
 * ProblemDetail; any other path is a client-side route and gets {@code index.html}. Other dots
 * are allowed because concept ids contain them (AD-10, e.g. {@code 8.EE.7-two-step-equations}).
 */
@Configuration(proxyBeanMethods = false)
class SpaResourceConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(false)
			.addResolver(new SpaFallbackResolver());
	}

	static final class SpaFallbackResolver extends PathResourceResolver {

		private static final Set<String> STATIC_FILE_EXTENSIONS = Set.of("js", "mjs", "css", "map", "json", "html",
				"txt", "svg", "png", "jpg", "jpeg", "gif", "webp", "ico", "woff", "woff2", "ttf", "mp3", "wav");

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource file = super.getResource(resourcePath, location);
			if (file != null) {
				return file;
			}
			if (isApiPath(resourcePath) || isStaticFilePath(resourcePath)) {
				return null;
			}
			return super.getResource("index.html", location);
		}

		private static boolean isApiPath(String path) {
			return path.equals("api") || path.startsWith("api/");
		}

		private static boolean isStaticFilePath(String path) {
			String lastSegment = path.substring(path.lastIndexOf('/') + 1);
			int dot = lastSegment.lastIndexOf('.');
			return dot >= 0
					&& STATIC_FILE_EXTENSIONS.contains(lastSegment.substring(dot + 1).toLowerCase(Locale.ROOT));
		}

	}

}
