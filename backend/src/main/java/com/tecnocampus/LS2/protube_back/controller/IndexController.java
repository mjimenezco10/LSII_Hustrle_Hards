package com.tecnocampus.LS2.protube_back.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    /**
     * Production (jar): the SPA is bundled in static/ and its client-side routes are served by index.html, so
     * opening or reloading e.g. /login or /videos/3 works. With Docker nginx does the same (try_files).
     * Keep in sync with the routes of frontend/src/App.tsx.
     */
    @GetMapping({"", "/", "/login", "/settings", "/upload", "/analytics", "/favourites", "/subscriptions",
            "/videos/{id}", "/videos/{id}/edit"})
    public ModelAndView home() {
        return new ModelAndView("index");
    }
    @PostMapping("logout")
    public ModelAndView logout() {
        return new ModelAndView("logout");
    }

}
