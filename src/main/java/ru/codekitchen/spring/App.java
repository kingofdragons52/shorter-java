package ru.codekitchen.spring;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class App {

    public static void main(String[] args) throws Exception {
        Database.initDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new RootHandler());
        server.setExecutor(null);
        System.out.println("Сервер запущен на http://127.0.0.1:8080/");
        server.start();
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("GET".equals(method) && "/".equals(path)) {
                sendHtmlResponse(exchange, getIndexHtml(null));
            } else if ("POST".equals(method) && "/shorten".equals(path)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String resultMessage = RequestHelper.handleShorten(body);
                sendHtmlResponse(exchange, getIndexHtml(resultMessage));
            } else if ("GET".equals(method) && path.length() > 1) {
                String token = path.substring(1);
                String targetUrl = RedirectHelper.handleRedirect(token);

                if (targetUrl != null) {
                    exchange.getResponseHeaders().set("Location", targetUrl);
                    exchange.sendResponseHeaders(302, -1);
                } else {
                    sendResponse(exchange, 404, "Ссылка не найдена");
                }
            } else {
                sendResponse(exchange, 404, "Страница не найдена");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void sendHtmlResponse(HttpExchange exchange, String html) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            sendResponse(exchange, 200, html);
        }

        private String getIndexHtml(String alertMessage) {
            return "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Shorter</title>\n" +
                    "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"row mt-5\" style=\"margin-top: 20% !important;\">\n" +
                    "            <a href=\"/\" class=\"text-decoration-none text-dark\">\n" +
                    "                <h1>Shorter</h1>\n" +
                    "            </a>\n" +
                    "        </div>\n" +
                    "        <div class=\"row\">\n" +
                    "            <form action=\"/shorten\" method=\"POST\">\n" +
                    "                <div class=\"input-group mb-3\">\n" +
                    "                    <input type=\"text\" name=\"cut-link\" class=\"form-control\" placeholder=\"Вставь ссылку\">\n" +
                    "                    <button class=\"btn btn-outline-secondary\" type=\"submit\">Обрезать</button>\n" +
                    "                </div>\n" +
                    "            </form>\n" +
                    "        </div>\n" +
                    "        <div class=\"row\">\n" +
                    "            " + (alertMessage != null ? alertMessage : "") + "\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }
}