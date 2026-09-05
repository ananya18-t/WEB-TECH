package com.electricity;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Embedded WebServer to run ElectricityBillServlet directly using Java 17 built-in HttpServer.
 * Serves static web resources and delegates servlet paths to ElectricityBillServlet.
 */
public class WebServer {

    private static final int PORT = 8080;
    private static final String WEB_APP_ROOT = "src/main/webapp";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Servlet Handler
        ElectricityBillServlet billServlet = new ElectricityBillServlet();
        billServlet.init(new DummyServletConfig());

        HttpHandler servletHandler = exchange -> handleServletCall(billServlet, exchange);

        server.createContext("/ElectricityBillServlet", servletHandler);
        server.createContext("/calculate", servletHandler);

        // Static Content Handler
        server.createContext("/", exchange -> handleStaticFile(exchange));

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println("=======================================================================");
        System.out.println("⚡ Electricity Bill Calculator Servlet Web Server is RUNNING!");
        System.out.println("👉 Access Web App: http://localhost:" + PORT + "/index.html");
        System.out.println("👉 Servlet Endpoint: http://localhost:" + PORT + "/ElectricityBillServlet");
        System.out.println("=======================================================================");
    }

    private static void handleServletCall(ElectricityBillServlet servlet, HttpExchange exchange) throws IOException {
        try {
            SimpleServletRequest request = new SimpleServletRequest(exchange);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SimpleServletResponse response = new SimpleServletResponse(exchange, baos);

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                servlet.doPost(request, response);
            } else {
                servlet.doGet(request, response);
            }

            response.flushBuffer();
            byte[] responseBytes = baos.toByteArray();
            
            String contentType = response.getContentType();
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            
            int status = response.getStatus() == 0 ? 200 : response.getStatus();
            exchange.sendResponseHeaders(status, responseBytes.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            String err = "Servlet Error: " + e.getMessage();
            exchange.sendResponseHeaders(500, err.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(err.getBytes());
            os.close();
        }
    }

    private static void handleStaticFile(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(WEB_APP_ROOT + path);
        if (!file.exists() || file.isDirectory()) {
            file = new File("." + path);
        }

        if (!file.exists() || file.isDirectory()) {
            String msg = "404 Not Found: " + path;
            exchange.sendResponseHeaders(404, msg.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes());
            os.close();
            return;
        }

        String contentType = "text/html";
        if (path.endsWith(".css")) contentType = "text/css";
        else if (path.endsWith(".js")) contentType = "application/javascript";
        else if (path.endsWith(".png")) contentType = "image/png";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (path.endsWith(".svg")) contentType = "image/svg+xml";

        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, fileBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }

    // Dummy ServletConfig
    static class DummyServletConfig implements ServletConfig {
        public String getServletName() { return "ElectricityBillServlet"; }
        public ServletContext getServletContext() { return null; }
        public String getInitParameter(String name) { return null; }
        public Enumeration<String> getInitParameterNames() { return Collections.enumeration(Collections.emptyList()); }
    }

    // Lightweight HttpServletRequest Wrapper
    static class SimpleServletRequest implements HttpServletRequest {
        private final HttpExchange exchange;
        private final Map<String, String[]> parameters = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();

        public SimpleServletRequest(HttpExchange exchange) throws IOException {
            this.exchange = exchange;
            
            // Copy headers
            for (String h : exchange.getRequestHeaders().keySet()) {
                headers.put(h.toLowerCase(), exchange.getRequestHeaders().getFirst(h));
            }

            // Parse Query String
            String rawQuery = exchange.getRequestURI().getRawQuery();
            parseQueryString(rawQuery);

            // Parse POST Body
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String body = result.toString(StandardCharsets.UTF_8);
                parseQueryString(body);
            }
        }

        private void parseQueryString(String query) {
            if (query == null || query.isEmpty()) return;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                try {
                    String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
                    
                    if (parameters.containsKey(key)) {
                        String[] oldArr = parameters.get(key);
                        String[] newArr = Arrays.copyOf(oldArr, oldArr.length + 1);
                        newArr[newArr.length - 1] = value;
                        parameters.put(key, newArr);
                    } else {
                        parameters.put(key, new String[]{value});
                    }
                } catch (Exception ignored) {}
            }
        }

        public String getParameter(String name) {
            String[] vals = parameters.get(name);
            return (vals != null && vals.length > 0) ? vals[0] : null;
        }

        public Map<String, String[]> getParameterMap() { return parameters; }
        public Enumeration<String> getParameterNames() { return Collections.enumeration(parameters.keySet()); }
        public String[] getParameterValues(String name) { return parameters.get(name); }
        public String getHeader(String name) { return headers.get(name.toLowerCase()); }
        public Enumeration<String> getHeaderNames() { return Collections.enumeration(headers.keySet()); }
        public Enumeration<String> getHeaders(String name) {
            String val = headers.get(name.toLowerCase());
            return val != null ? Collections.enumeration(Collections.singletonList(val)) : Collections.enumeration(Collections.emptyList());
        }

        // Stubbed Servlet Request methods
        public Object getAttribute(String name) { return null; }
        public Enumeration<String> getAttributeNames() { return Collections.enumeration(Collections.emptyList()); }
        public String getCharacterEncoding() { return "UTF-8"; }
        public void setCharacterEncoding(String env) {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return getHeader("content-type"); }
        public javax.servlet.ServletInputStream getInputStream() { return null; }
        public String getProtocol() { return "HTTP/1.1"; }
        public String getScheme() { return "http"; }
        public String getServerName() { return "localhost"; }
        public int getServerPort() { return 8080; }
        public BufferedReader getReader() { return new BufferedReader(new StringReader("")); }
        public String getRemoteAddr() { return "127.0.0.1"; }
        public String getRemoteHost() { return "localhost"; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public Locale getLocale() { return Locale.getDefault(); }
        public Enumeration<Locale> getLocales() { return Collections.enumeration(Collections.singletonList(Locale.getDefault())); }
        public boolean isSecure() { return false; }
        public javax.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        public String getRealPath(String path) { return WEB_APP_ROOT + path; }
        public int getRemotePort() { return 0; }
        public String getLocalName() { return "localhost"; }
        public String getLocalAddr() { return "127.0.0.1"; }
        public int getLocalPort() { return 8080; }
        public ServletContext getServletContext() { return null; }
        public javax.servlet.AsyncContext startAsync() { return null; }
        public javax.servlet.AsyncContext startAsync(javax.servlet.ServletRequest r, javax.servlet.ServletResponse s) { return null; }
        public boolean isAsyncStarted() { return false; }
        public boolean isAsyncSupported() { return false; }
        public javax.servlet.AsyncContext getAsyncContext() { return null; }
        public javax.servlet.DispatcherType getDispatcherType() { return javax.servlet.DispatcherType.REQUEST; }
        public String getAuthType() { return null; }
        public Cookie[] getCookies() { return new Cookie[0]; }
        public long getDateHeader(String name) { return 0; }
        public int getIntHeader(String name) { return 0; }
        public String getMethod() { return exchange.getRequestMethod(); }
        public String getPathInfo() { return exchange.getRequestURI().getPath(); }
        public String getPathTranslated() { return null; }
        public String getContextPath() { return ""; }
        public String getQueryString() { return exchange.getRequestURI().getQuery(); }
        public String getRemoteUser() { return null; }
        public boolean isUserInRole(String role) { return false; }
        public java.security.Principal getUserPrincipal() { return null; }
        public String getRequestedSessionId() { return null; }
        public String getRequestURI() { return exchange.getRequestURI().getPath(); }
        public StringBuffer getRequestURL() { return new StringBuffer("http://localhost:8080" + getRequestURI()); }
        public String getServletPath() { return getRequestURI(); }
        public HttpSession getSession(boolean create) { return null; }
        public HttpSession getSession() { return null; }
        public String changeSessionId() { return null; }
        public boolean isRequestedSessionIdValid() { return false; }
        public boolean isRequestedSessionIdFromCookie() { return false; }
        public boolean isRequestedSessionIdFromURL() { return false; }
        public boolean isRequestedSessionIdFromUrl() { return false; }
        public boolean authenticate(HttpServletResponse response) { return false; }
        public void login(String username, String password) {}
        public void logout() {}
        public Collection<javax.servlet.http.Part> getParts() { return Collections.emptyList(); }
        public javax.servlet.http.Part getPart(String name) { return null; }
        public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
    }

    // Lightweight HttpServletResponse Wrapper
    static class SimpleServletResponse implements HttpServletResponse {
        private final HttpExchange exchange;
        private final ByteArrayOutputStream baos;
        private PrintWriter writer;
        private String contentType = "text/html;charset=UTF-8";
        private int status = 200;

        public SimpleServletResponse(HttpExchange exchange, ByteArrayOutputStream baos) {
            this.exchange = exchange;
            this.baos = baos;
        }

        public String getContentType() { return contentType; }
        public void setContentType(String type) { this.contentType = type; }
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);
            }
            return writer;
        }

        public void setStatus(int sc) { this.status = sc; }
        public int getStatus() { return status; }
        public void flushBuffer() {
            if (writer != null) writer.flush();
        }

        // Stubbed response methods
        public void addCookie(Cookie cookie) {}
        public boolean containsHeader(String name) { return false; }
        public String encodeURL(String url) { return url; }
        public String encodeRedirectURL(String url) { return url; }
        public String encodeUrl(String url) { return url; }
        public String encodeRedirectUrl(String url) { return url; }
        public void sendError(int sc, String msg) throws IOException { this.status = sc; getWriter().write(msg); }
        public void sendError(int sc) throws IOException { this.status = sc; }
        public void sendRedirect(String location) throws IOException {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(302, -1);
        }
        public void setDateHeader(String name, long date) {}
        public void addDateHeader(String name, long date) {}
        public void setHeader(String name, String value) { exchange.getResponseHeaders().set(name, value); }
        public void addHeader(String name, String value) { exchange.getResponseHeaders().add(name, value); }
        public void setIntHeader(String name, int value) {}
        public void addIntHeader(String name, int value) {}
        public void setStatus(int sc, String sm) { this.status = sc; }
        public String getCharacterEncoding() { return "UTF-8"; }
        public javax.servlet.ServletOutputStream getOutputStream() {
            return new javax.servlet.ServletOutputStream() {
                public boolean isReady() { return true; }
                public void setWriteListener(javax.servlet.WriteListener writeListener) {}
                public void write(int b) { baos.write(b); }
            };
        }
        public void setCharacterEncoding(String charset) {}
        public void setContentLength(int len) {}
        public void setContentLengthLong(long len) {}
        public void setBufferSize(int size) {}
        public int getBufferSize() { return 8192; }
        public void resetBuffer() { baos.reset(); }
        public boolean isCommitted() { return false; }
        public void reset() { baos.reset(); }
        public void setLocale(Locale loc) {}
        public Locale getLocale() { return Locale.getDefault(); }
        public String getHeader(String name) { return exchange.getResponseHeaders().getFirst(name); }
        public Collection<String> getHeaders(String name) { return exchange.getResponseHeaders().get(name); }
        public Collection<String> getHeaderNames() { return exchange.getResponseHeaders().keySet(); }
    }
}
