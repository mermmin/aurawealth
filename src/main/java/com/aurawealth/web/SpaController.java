package com.aurawealth.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React app for its client-side routes ({@code /dashboard}, {@code /accounts}, ...) so a reload or a
 * shared link lands on the app, which then routes itself. Paths with a dot are files (JS, CSS, icons) and are
 * left to the static resource handler; {@code /api} paths have their own controllers.
 */
@Controller
public class SpaController {

	@GetMapping("/{path:^(?!api$)[^.]*}")
	public String app() {
		return "forward:/index.html";
	}

}
