package com.aki.p17;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import annotation.AnnotationController;
import annotation.GetMethode;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    private Map<String, Method> urlMappings; // URL -> Method

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        urlMappings = new HashMap<>();
        scanControllers();
    }

    private void scanControllers() {
        try {
            String controllerPackage = getServletContext().getInitParameter("Controllers");
            if (controllerPackage == null) return;

            String packagePath = controllerPackage.replace('.', '/') + "/";
            java.util.Set<String> paths = getServletContext().getResourcePaths("/WEB-INF/classes/" + packagePath);
            if (paths != null) {
                for (String path : paths) {
                    if (path.endsWith(".class")) {
                        String className = path.substring("/WEB-INF/classes/".length()).replace('/', '.').replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(AnnotationController.class)) {
                                String prefix = "/" + clazz.getAnnotation(AnnotationController.class).value();
                                for (Method method : clazz.getDeclaredMethods()) {
                                    if (method.isAnnotationPresent(GetMethode.class)) {
                                        String methodPath = method.getAnnotation(GetMethode.class).value();
                                        String fullPath = prefix + "/" + methodPath;
                                        urlMappings.put(fullPath, method);
                                    }
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // Ignore classes that can't be loaded
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Redirect root to /list
        if (path.equals("/")) {
            res.sendRedirect("list");
            return;
        }

        // Check for /list endpoint
        if (path.equals("/list")) {
            listUrls(req, res);
            return;
        }

        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            // Vérifie si un contrôleur annoté peut traiter la requête
            if (!handleAnnotatedControllers(req, res, path)) {
                customServe(req, res);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        service(req, res);
    }

    private boolean handleAnnotatedControllers(HttpServletRequest req, HttpServletResponse res, String path) {
        try {
            if (urlMappings == null) return false;

            // Normalisation pour ignorer le '/' final
            String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            // Normalisation pour remplacer les underscores par des tirets
            normalizedPath = normalizedPath.replace('_', '-');

            if (urlMappings.containsKey(normalizedPath)) {
                Method method = urlMappings.get(normalizedPath);
                Object controllerInstance = method.getDeclaringClass().getDeclaredConstructor().newInstance();

                res.setContentType("text/html;charset=UTF-8");
                PrintWriter out = res.getWriter();
                Object result = method.invoke(controllerInstance);
                out.println("Return type: " + method.getReturnType().getSimpleName().toLowerCase());
                out.println(result);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false; // Aucun contrôleur ne correspond
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = "<html>" +
                "<head><title>Resource Not Found</title></head>" +
                "<body>" +
                "<h1>Unknown resource</h1>" +
                "<p>The requested URL was not found: <strong>" + uri + "</strong></p>" +
                "</body>" +
                "</html>";

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void listUrls(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            res.setContentType("text/html;charset=UTF-8");
            out.println("<html><head><title>URL Mappings</title></head><body>");
            out.println("<h1>All Mapped URLs</h1>");
            out.println("<table border='1'><tr><th>URL</th><th>Class</th><th>Method</th></tr>");

            for (Map.Entry<String, Method> entry : urlMappings.entrySet()) {
                String url = entry.getKey();
                Method method = entry.getValue();
                String className = method.getDeclaringClass().getSimpleName();
                String methodName = method.getName();
                out.println("<tr><td>" + url + "</td><td>" + className + "</td><td>" + methodName + "</td></tr>");
            }

            out.println("</table></body></html>");
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
