package com.bupt.tarecruit;

import com.bupt.tarecruit.handler.*;
import com.bupt.tarecruit.service.DataService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        String baseDir = System.getProperty("user.dir");
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        DataService ds = new DataService(baseDir);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth", new AuthHandler(ds));
        server.createContext("/api/jobs", new JobHandler(ds));
        server.createContext("/api/applications", new ApplicationHandler(ds));
        server.createContext("/api/admin", new AdminHandler(ds));
        server.createContext("/api/upload", new UploadHandler(ds));

        Path staticPath = Paths.get(baseDir, "src", "main", "resources", "static");
        server.createContext("/", new StaticHandler(staticPath.toString()));

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("===========================================");
        System.out.println("  TA Recruitment System");
        System.out.println("  Running at http://localhost:" + port);
        System.out.println("  Default admin: admin / admin123");
        System.out.println("===========================================");

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) Runtime.getRuntime().exec("cmd /c start http://localhost:" + port);
            else if (os.contains("mac")) Runtime.getRuntime().exec("open http://localhost:" + port);
            else Runtime.getRuntime().exec("xdg-open http://localhost:" + port);
        } catch (Exception ignored) {}
    }
}
