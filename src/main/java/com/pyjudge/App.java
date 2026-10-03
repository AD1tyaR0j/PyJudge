package com.pyjudge;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import com.pyjudge.config.AppConfig;

/**
 * Entry point: starts an embedded Tomcat that serves the JSP views in src/main/webapp and the
 * servlets/filters/listeners compiled into target/classes (discovered through their annotations).
 *
 * Run with:  ./mvnw compile exec:java   then open http://localhost:8080
 */
public final class App {

    private final Tomcat tomcat = new Tomcat();

    private App() {
    }

    public static void main(String[] args) throws LifecycleException {
        App app = start(AppConfig.fromEnvironment());
        System.out.println("PyJudge running on http://localhost:" + app.port());
        app.tomcat.getServer().await();
    }

    /** Starts the server with the given configuration. Port 0 picks a free port (used by tests). */
    public static App start(AppConfig config) throws LifecycleException {
        AppConfig.install(config);
        App app = new App();
        File baseDir = new File(config.dataDir(), "tomcat");
        baseDir.mkdirs();
        app.tomcat.setBaseDir(baseDir.getAbsolutePath());
        app.tomcat.setPort(config.port());
        app.tomcat.getConnector().setURIEncoding("UTF-8");

        Context ctx = app.tomcat.addWebapp("", new File(config.webappDir()).getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                new File(config.classesDir()).getAbsolutePath(), "/"));
        ctx.setResources(resources);
        ctx.setParentClassLoader(App.class.getClassLoader());

        app.tomcat.start();
        return app;
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public void stop() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }
}
