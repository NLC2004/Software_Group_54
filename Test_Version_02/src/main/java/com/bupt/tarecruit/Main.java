package com.bupt.tarecruit;

import com.bupt.tarecruit.handler.*;
import com.bupt.tarecruit.service.DataService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

/**
 * Application entry point for the TA Recruitment System web server.
 *
 * <p>This class wires the JSON-backed service layer to the HTTP API handlers
 * and to the static frontend file handler. The application is intentionally
 * self-contained: it serves the browser pages and all backend endpoints from
 * one lightweight {@link HttpServer} instance.</p>
 *
 * <p>Persistent application files are resolved relative to the working
 * directory used when the server is started. In normal operation that working
 * directory is the {@code Test_Version_02} project folder, allowing
 * {@link DataService} to use its local {@code data/} and {@code uploads/}
 * directories.</p>
 */
public class Main {

    /**
     * Starts the application server and registers each HTTP endpoint group.
     *
     * @param args optional first argument specifying the HTTP port; when the
     *             value is absent or not numeric the server uses port 8080
     * @throws Exception if storage initialization or server startup fails
     */
    public static void main(String[] args) throws Exception {
        String baseDir = System.getProperty("user.dir");
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        /*
         * A single DataService instance owns persisted JSON records, uploaded
         * documents and in-memory sessions for every request handler. Sharing
         * it ensures a submission created through one endpoint is immediately
         * visible to dashboards, notifications and administration endpoints.
         */
        DataService ds = new DataService(baseDir);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register the backend API surface used by TA, MO and Admin pages.
        server.createContext("/api/auth", new AuthHandler(ds));
        server.createContext("/api/jobs", new JobHandler(ds));
        server.createContext("/api/applications", new ApplicationHandler(ds));
        server.createContext("/api/drafts", new DraftHandler(ds));
        server.createContext("/api/admin", new AdminHandler(ds));
        server.createContext("/api/upload", new UploadHandler(ds));
        server.createContext("/api/notifications", new NotificationHandler(ds));

        /*
         * The catch-all route is registered last conceptually: it serves
         * frontend HTML/CSS/JavaScript assets after the more specific API
         * prefixes above have been associated with their handlers.
         */
        server.createContext("/", new StaticHandler(baseDir));

        /*
         * A bounded executor permits concurrent browser/API requests without
         * creating an unbounded number of threads during multi-page use.
         */
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("===========================================");
        System.out.println("  TA Recruitment System v2");
        System.out.println("  Running at http://localhost:" + port);
        System.out.println("  Default admin: admin / admin123");
        System.out.println("===========================================");
        System.out.println("  TA Login:    http://localhost:" + port + "/TA/index.html");
        System.out.println("  MO Login:    http://localhost:" + port + "/MO/index.html");
        System.out.println("  Admin Login: http://localhost:" + port + "/admin/index.html");
        System.out.println("===========================================");

        // Keep startup non-intrusive: users choose which role portal to open.
    }
}
