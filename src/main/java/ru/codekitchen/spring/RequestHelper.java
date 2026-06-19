package ru.codekitchen.spring;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class RequestHelper {

    public static String handleShorten(String rawFormData) {
        String longUrl = parseCutLink(rawFormData);

        if (longUrl == null || longUrl.trim().isEmpty()) {
            return "<div class='alert alert-danger'>Ссылка не может быть пустой</div>";
        }

        longUrl = URLDecoder.decode(longUrl, StandardCharsets.UTF_8).trim();

        try (Connection conn = Database.getConnection()) {
            boolean searchBool = false;
            String token = "";

            while (!searchBool) {
                token = tokenGen(5, 8);

                String checkTokenSql = "SELECT COUNT(*) FROM links WHERE token = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkTokenSql)) {
                    stmt.setString(1, token);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            searchBool = true;
                        }
                    }
                }
            }

            String checkUrlSql = "SELECT COUNT(*) FROM links WHERE url = ?";
            boolean urlExists = false;
            try (PreparedStatement stmt = conn.prepareStatement(checkUrlSql)) {
                stmt.setString(1, longUrl);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        urlExists = true;
                    }
                }
            }

            if (!urlExists) {
                String insertSql = "INSERT INTO links (url, token) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, longUrl);
                    insertStmt.setString(2, token);
                    int rows = insertStmt.executeUpdate();

                    if (rows > 0) {
                        String shortUrl = "http://127.0.0.1:8080/" + token;
                        return "Короткая ссылка: <a href=\"" + shortUrl + "\" target=\"_blank\">" + shortUrl + "</a>";
                    } else {
                        return "Ссылка не добавлена";
                    }
                }
            } else {
                return "Такая ссылка уже есть в системе";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка базы данных: " + e.getMessage();
        }
    }

    private static String tokenGen(int min, int max) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rand = new Random();
        int randEnd = rand.nextInt((max - min) + 1) + min;

        StringBuilder token = new StringBuilder();
        for (int i = 0; i < randEnd; i++) {
            token.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return token.toString();
    }

    private static String parseCutLink(String formData) {
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length > 1 && "cut-link".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
}