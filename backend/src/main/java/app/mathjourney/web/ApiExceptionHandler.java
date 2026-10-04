package app.mathjourney.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The one global error handler: every non-2xx response is a {@link ProblemDetail} (RFC 9457).
 * Spring MVC's own exceptions (including {@code NoResourceFoundException} for unknown
 * {@code /api/**} paths and {@code ResponseStatusException}) are handled by the base class.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	static final String GENERIC_DETAIL = "Something went wrong on the server.";

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
		log.error("[web] Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_DETAIL);
	}

}
