package com.expensetracker.api.support;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the Flask app subprocess for integration tests.
 *
 * Lifecycle:
 *   start()  — launch Python with a temp SQLite DB; block until HTTP /  returns 200.
 *   stop()   — kill the process and delete the temp DB.
 *
 * DB_PATH and PORT are passed as environment variables so app.py picks them up.
 * The venv Python is preferred when present at ../venv/Scripts/python.exe (Windows)
 * or ../venv/bin/python (Unix/macOS).
 */
public class AppFixture {

    private static final int STARTUP_TIMEOUT_MS = 20_000;
    private static final int POLL_INTERVAL_MS   = 300;

    private final int    port;
    private final String appDir;

    private Process appProcess;
    private Path    dbFilePath;

    public AppFixture() {
        this(5001);
    }

    public AppFixture(int port) {
        this.port   = port;
        // api-tests/ is one level below the Flask project root
        this.appDir = System.getProperty(
                "app.dir",
                Paths.get(System.getProperty("user.dir")).getParent().toString());
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() throws Exception {
        dbFilePath = Files.createTempFile("expenses_test_", ".db");
        // Flask's init_db() creates the file; delete the empty placeholder so it
        // starts fresh (SQLite will re-create it on first connect).
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

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    /** Absolute path to the temp SQLite DB file used by this fixture. */
    public Path dbPath() {
        return dbFilePath;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private String pythonExecutable() {
        // Windows venv
        Path win = Paths.get(appDir, "venv", "Scripts", "python.exe");
        if (win.toFile().exists()) return win.toAbsolutePath().toString();
        // Unix/macOS venv
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
                // not ready yet
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        stop();
        throw new IllegalStateException(
                "Flask app did not become ready within " + STARTUP_TIMEOUT_MS + "ms on port " + port);
    }
}
