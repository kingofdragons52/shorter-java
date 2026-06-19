package ru.codekitchen.spring;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class RequestHelper {
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String handleShorten(String rawFormData) {
        String longUrl = parseCutLink(rawFormData);

        if (longUrl == null || longUrl.trim().isEmpty()) {
            return "<div class='alert alert-danger'>Ссылка не может быть пустой</div>";
        }

        longUrl = URLDecoder.decode(longUrl, StandardCharsets.UTF_8).trim();
        String existingToken = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT token FROM links WHERE url = ?")) {

            checkStmt.setString(1, longUrl);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    existingToken = rs.getString("token");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка БД при проверке: " + e.getMessage();
        }

        if (existingToken != null) {
            String shortUrl = "http://127.0.0.1:8080/" + existingToken;
            return "Такая ссылка уже есть: <a href='" + shortUrl + "'>" + shortUrl + "</a>";
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(
                     "INSERT INTO links (url, token) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            insertStmt.setString(1, longUrl);
            insertStmt.setString(2, "");
            insertStmt.executeUpdate();

            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    String token = encodeBase62(id);

                    try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE links SET token = ? WHERE id = ?")) {
                        updateStmt.setString(1, token);
                        updateStmt.setLong(2, id);
                        updateStmt.executeUpdate();
                    }

                    String shortUrl = "http://127.0.0.1:8080/" + token;
                    return "Короткая ссылка: <a href=\"" + shortUrl + "\" target=\"_blank\">" + shortUrl + "</a>";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Ошибка базы данных при сохранении: " + e.getMessage();
        }

        return "Не удалось сгенерировать ссылку";
    }

    private static String encodeBase62(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int) (id % 62)));
            id /= 62;
        }
        return sb.length() == 0 ? "a" : sb.reverse().toString();
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