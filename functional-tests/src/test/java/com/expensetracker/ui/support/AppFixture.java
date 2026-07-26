package com.expensetracker.ui.support;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppFixture {

    private static final int STARTUP_TIMEOUT_MS = 20_000;
    private static final int POLL_INTERVAL_MS   = 300;

    private final int    port;
    private final String appDir;

    private Process appProcess;
    private Path    dbFilePath;

    public AppFixture() {
        this(findFreePort());
    }

    public AppFixture(int port) {
        this.port   = port;
        this.appDir = System.getProperty(
                "app.dir",
                Paths.get(System.getProperty("user.dir")).getParent().toString());
    }

    private static int findFreePort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            return ss.getLocalPort();
        } catch (IOException e) {
            return 5002;
        }
    }

    public void start() throws Exception {
        dbFilePath = Files.createTempFile("expenses_functional_", ".db");
        Files.delete(dbFilePath);

        ProcessBuilder pb = new ProcessBuilder(pythonExecutable(), "app.py");
        pb.directory(Paths.get(appDir).toFile());
        pb.environment().put("DB_PATH", dbFilePath.toAbsolutePath().toString());
        pb.environment().put("PORT",    String.valueOf(port));
        pb.redirectErrorStream(true);

        appProcess = pb.start();
        waitForReady();
    }

    public void stop() {
        if (appProcess != null && appProcess.isAlive()) {
            appProcess.destroyForcibly();
        }
        if (dbFilePath != null) {
            dbFilePath.toFile().delete();
        }
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public Path dbPath() {
        return dbFilePath;
    }

    private String pythonExecutable() {
        Path win = Paths.get(appDir, "venv", "Scripts", "python.exe");
        if (win.toFile().exists()) return win.toAbsolutePath().toString();
        Path unix = Paths.get(appDir, "venv", "bin", "python");
        if (unix.toFile().exists()) return unix.toAbsolutePath().toString();
        return "python";
    }

    private void waitForReady() throws Exception {
        URL healthUrl = new URL(baseUrl() + "/");
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            if (!appProcess.isAlive()) {
                throw new IllegalStateException(
                        "Flask process exited before becoming ready (exit=" + appProcess.exitValue() + ")");
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) healthUrl.openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                if (conn.getResponseCode() == 200) return;
            } catch (IOException ignored) {
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        stop();
        throw new IllegalStateException(
                "Flask app did not become ready within " + STARTUP_TIMEOUT_MS + "ms on port " + port);
    }
}
