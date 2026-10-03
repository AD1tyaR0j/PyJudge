package com.pyjudge.web;

import java.util.logging.Logger;

import com.pyjudge.Services;
import com.pyjudge.config.AppConfig;
import com.pyjudge.service.SeedService;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Web-app lifecycle: builds the services (database, judge thread pool, ...) when the container starts
 * the application, loads built-in content, and shuts everything down cleanly when it stops.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Services services = new Services(AppConfig.current());
        SeedService.Report report = services.seed.seed();
        LOG.info("PyJudge ready: " + report.exercisesAdded() + " exercise(s) added, course "
                + report.course().lessonsCreated() + " new / " + report.course().lessonsUpdated() + " refreshed lessons");
        event.getServletContext().setAttribute(Services.ATTRIBUTE, services);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object services = event.getServletContext().getAttribute(Services.ATTRIBUTE);
        if (services instanceof Services s) {
            s.close(); // lets in-flight judging finish, then closes the connection pool
        }
    }
}
