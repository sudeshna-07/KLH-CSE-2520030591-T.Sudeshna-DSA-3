
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {

    private static final int PORT = 8080;

    // Expected project structure:
    // DSA-3 PROJECT/
    //   WebServer.java
    //   MatrixChain.java
    //   matrix_dataset.csv
    //   web/index.html
    //   web/style.css
    //   web/script.js

    private static final Path WEB_DIR = Paths.get("web");
    private static final Path DATASET = Paths.get("matrix_dataset.csv");

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0
        );

        server.createContext(
            "/api/dataset", WebServer::handleDataset
        );

        server.createContext(
            "/api/optimize", WebServer::handleOptimize
        );

        server.createContext(
            "/", WebServer::handleStaticFiles
        );

        server.start();

        System.out.println("--------------------------------------");
        System.out.println(" Matrix Chain Optimizer Dashboard");
        System.out.println("--------------------------------------");
        System.out.println("Server started successfully.");
        System.out.println("Open: http://localhost:8080");
        System.out.println("Keep this terminal open.");
    }

    // --------------------------------------------------
    // DATASET: DOWNLOAD / READ / UPLOAD
    // --------------------------------------------------

    private static void handleDataset(HttpExchange exchange)
            throws IOException {

        try {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {

                if (!Files.exists(DATASET)) {
                    sendJson(
                        exchange,
                        404,
                        "{\"error\":\"matrix_dataset.csv not found in the project folder.\"}"
                    );
                    return;
                }

                String csv = Files.readString(
                    DATASET, StandardCharsets.UTF_8
                );

                sendJson(
                    exchange,
                    200,
                    "{\"csv\":" + jsonString(csv) + "}"
                );

            } else if ("POST".equals(method)) {

                String csv = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                );

                validateCSV(csv);

                Files.writeString(
                    DATASET,
                    csv,
                    StandardCharsets.UTF_8
                );

                sendJson(
                    exchange,
                    200,
                    "{\"message\":\"Dataset uploaded successfully.\"}"
                );

            } else {
                sendJson(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed.\"}"
                );
            }

        } catch (IllegalArgumentException exception) {
            sendJson(
                exchange,
                400,
                "{\"error\":" + jsonString(exception.getMessage()) + "}"
            );

        } catch (Exception exception) {
            sendJson(
                exchange,
                500,
                "{\"error\":" + jsonString(
                    "Dataset error: " + exception.getMessage()
                ) + "}"
            );
        }
    }

    private static void validateCSV(String csv) {

        String[] lines = csv.split("\\R");

        if (lines.length < 2 ||
            !lines[0].trim().startsWith("TestCase,Dimensions")) {

            throw new IllegalArgumentException(
                "CSV must contain the headers: TestCase,Dimensions,Category,Description."
            );
        }

        boolean foundData = false;

        for (int i = 1; i < lines.length; i++) {

            if (lines[i].trim().isEmpty()) {
                continue;
            }

            String[] fields = lines[i].split(",", 4);

            if (fields.length < 2) {
                throw new IllegalArgumentException(
                    "Invalid CSV row at line " + (i + 1)
                );
            }

            String dimensions = fields[1]
                .trim()
                .replaceAll("^\"|\"$", "");

            parseDimensions(dimensions);

            foundData = true;
        }

        if (!foundData) {
            throw new IllegalArgumentException(
                "The CSV must contain at least one test case."
            );
        }
    }

    // --------------------------------------------------
    // MATRIX CHAIN OPTIMIZATION
    // --------------------------------------------------

    private static void handleOptimize(HttpExchange exchange)
            throws IOException {

        if (!"POST".equals(exchange.getRequestMethod())) {

            sendJson(
                exchange,
                405,
                "{\"error\":\"Use POST for optimization.\"}"
            );

            return;
        }

        try {

            String requestBody = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            // Read the dimensions property from the JSON request.
            Pattern pattern = Pattern.compile(
                "\"dimensions\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
            );

            Matcher matcher = pattern.matcher(requestBody);

            if (!matcher.find()) {
                sendJson(
                    exchange,
                    400,
                    "{\"error\":\"Please enter matrix dimensions.\"}"
                );
                return;
            }

            String dimensionsText = matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

            int[] dimensions = parseDimensions(dimensionsText);

            // Run the existing dynamic programming algorithm.
            MatrixChain chain = new MatrixChain(dimensions);

            long optimalCost = chain.getMinimumCost();
            long leftToRightCost = chain.getLeftToRightCost();
            long costSaved = leftToRightCost - optimalCost;

            double percentSaved = leftToRightCost == 0
                ? 0.0
                : costSaved * 100.0 / leftToRightCost;

            // Return the data required by the dashboard.
            StringBuilder json = new StringBuilder();

            json.append("{");

            json.append("\"optimalCost\":")
                .append(optimalCost).append(",");

            json.append("\"leftToRightCost\":")
                .append(leftToRightCost).append(",");

            json.append("\"costSaved\":")
                .append(costSaved).append(",");

            json.append("\"percentSaved\":")
                .append(percentSaved).append(",");

            json.append("\"matrixCount\":")
                .append(chain.getMatrixCount()).append(",");

            json.append("\"parenthesization\":")
                .append(jsonString(
                    chain.getOptimalParenthesization()
                )).append(",");

            json.append("\"costTable\":")
                .append(tableToJson(chain.getCostTable())).append(",");

            json.append("\"splitTable\":")
                .append(tableToJson(chain.getSplitTable()));

            json.append("}");

            sendJson(exchange, 200, json.toString());

        } catch (IllegalArgumentException exception) {

            sendJson(
                exchange,
                400,
                "{\"error\":" + jsonString(exception.getMessage()) + "}"
            );

        } catch (Exception exception) {

            sendJson(
                exchange,
                500,
                "{\"error\":" + jsonString(
                    "Optimization failed: " + exception.getMessage()
                ) + "}"
            );
        }
    }

    // --------------------------------------------------
    // VALIDATE AND CONVERT MATRIX DIMENSIONS
    // --------------------------------------------------

    private static int[] parseDimensions(String text) {

        String trimmed = text.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                "Enter dimensions, for example: 40 20 30 10."
            );
        }

        String[] parts = trimmed.split("[\\s,]+");

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                "Enter at least two dimensions for one matrix."
            );
        }

        if (parts.length > 501) {
            throw new IllegalArgumentException(
                "A maximum of 500 matrices is supported."
            );
        }

        int[] dimensions = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {

            try {
                dimensions[i] = Integer.parseInt(parts[i]);

            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                    "Dimensions must be positive integers."
                );
            }

            if (dimensions[i] <= 0) {
                throw new IllegalArgumentException(
                    "Dimensions must be positive integers."
                );
            }
        }

        return dimensions;
    }

    // --------------------------------------------------
    // CONVERT DP TABLES INTO JSON ARRAYS
    // --------------------------------------------------

    private static String tableToJson(long[][] table) {

        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < table.length; i++) {

            if (i > 0) {
                result.append(",");
            }

            result.append("[");

            for (int j = 0; j < table[i].length; j++) {

                if (j > 0) {
                    result.append(",");
                }

                result.append(table[i][j] < 0 ? "-1" : table[i][j]);
            }

            result.append("]");
        }

        return result.append("]").toString();
    }

    private static String tableToJson(int[][] table) {

        StringBuilder result = new StringBuilder("[");

        for (int i = 0; i < table.length; i++) {

            if (i > 0) {
                result.append(",");
            }

            result.append("[");

            for (int j = 0; j < table[i].length; j++) {

                if (j > 0) {
                    result.append(",");
                }

                result.append(table[i][j]);
            }

            result.append("]");
        }

        return result.append("]").toString();
    }

    // --------------------------------------------------
    // SERVE HTML, CSS AND JAVASCRIPT
    // --------------------------------------------------

    private static void handleStaticFiles(HttpExchange exchange)
            throws IOException {

        if (!"GET".equals(exchange.getRequestMethod())) {

            sendText(
                exchange,
                405,
                "Method not allowed",
                "text/plain; charset=UTF-8"
            );

            return;
        }

        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // Serve only the intended frontend files.
        if (!requestPath.equals("/index.html")
            && !requestPath.equals("/style.css")
            && !requestPath.equals("/script.js")) {

            sendText(
                exchange,
                404,
                "Page not found",
                "text/plain; charset=UTF-8"
            );

            return;
        }

        Path file = WEB_DIR
            .resolve(requestPath.substring(1))
            .normalize();

        if (!Files.exists(file)) {

            sendText(
                exchange,
                404,
                "File not found: " + requestPath
                    + ". Check the web folder.",
                "text/plain; charset=UTF-8"
            );

            return;
        }

        String contentType;

        if (requestPath.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";

        } else if (requestPath.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";

        } else {
            contentType = "text/javascript; charset=UTF-8";
        }

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set(
            "Content-Type", contentType
        );

        exchange.getResponseHeaders().set(
            "Cache-Control", "no-store"
        );

        exchange.sendResponseHeaders(200, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // --------------------------------------------------
    // JSON STRING ESCAPING
    // --------------------------------------------------

    private static String jsonString(String text) {

        StringBuilder result = new StringBuilder("\"");

        for (char c : text.toCharArray()) {

            switch (c) {

                case '"':
                    result.append("\\\"");
                    break;

                case '\\':
                    result.append("\\\\");
                    break;

                case '\n':
                    result.append("\\n");
                    break;

                case '\r':
                    result.append("\\r");
                    break;

                case '\t':
                    result.append("\\t");
                    break;

                default:
                    if (c < 32) {
                        result.append(
                            String.format("\\u%04x", (int) c)
                        );
                    } else {
                        result.append(c);
                    }
            }
        }

        return result.append("\"").toString();
    }

    // --------------------------------------------------
    // SEND HTTP RESPONSES
    // --------------------------------------------------

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json) throws IOException {

        sendText(
            exchange,
            status,
            json,
            "application/json; charset=UTF-8"
        );
    }

    private static void sendText(
            HttpExchange exchange,
            int status,
            String text,
            String contentType) throws IOException {

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", contentType
        );

        exchange.getResponseHeaders().set(
            "Cache-Control", "no-store"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
