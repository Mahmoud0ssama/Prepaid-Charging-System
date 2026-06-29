package msc.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.net.URL;

public class WebServer {
    public static void main(String[] args) {
        int port = 8080;
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Set API Endpoints
        context.addServlet(new ServletHolder(new UserServlet()), "/api/users");
        context.addServlet(new ServletHolder(new CdrServlet()), "/api/cdrs");

        // Set Static Resource Servlet (HTML/CSS/JS)
        ServletHolder staticHolder = new ServletHolder("default", DefaultServlet.class);
        
        // Attempt to find resources in classpath, fallback to filesystem path
        URL webDir = WebServer.class.getClassLoader().getResource("web");
        if (webDir != null) {
            staticHolder.setInitParameter("resourceBase", webDir.toExternalForm());
        } else {
            staticHolder.setInitParameter("resourceBase", "./src/main/resources/web");
        }
        staticHolder.setInitParameter("dirAllowed", "false");
        context.addServlet(staticHolder, "/");

        try {
            System.out.println("Starting Web Server on http://localhost:" + port);
            server.start();
            server.join();
        } catch (Exception e) {
            System.err.println("Error starting Jetty Web Server:");
            e.printStackTrace();
        }
    }
}
