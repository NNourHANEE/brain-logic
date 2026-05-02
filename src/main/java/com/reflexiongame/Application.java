package com.reflexiongame;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class Application {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", null);

        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.register(AppConfig.class);

        DispatcherServlet dispatcher = new DispatcherServlet(springContext);

        Tomcat.addServlet(ctx, "dispatcher", dispatcher);
        ctx.addServletMappingDecoded("/*", "dispatcher");

        tomcat.start();
        System.out.println("Server started on port " + port);
        tomcat.getServer().await();
    }
}
