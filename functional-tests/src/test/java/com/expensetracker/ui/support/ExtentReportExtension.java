package com.expensetracker.ui.support;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.util.Optional;

public class ExtentReportExtension
        implements BeforeEachCallback, AfterEachCallback, TestWatcher {

    private static final ExtentReports EXTENT = buildExtent();
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EXTENT::flush));
    }

    private static ExtentReports buildExtent() {
        String dir = System.getProperty("extent.report.dir",
                "target" + File.separator + "extent-report");
        new File(dir).mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(dir + File.separator + "report.html");
        spark.config().setDocumentTitle("ExpenseTracker UI Test Report");
        spark.config().setReportName("Functional Test Results");
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setEncoding("utf-8");

        ExtentReports extent = new ExtentReports();
        extent.setSystemInfo("Application", "ExpenseTracker");
        extent.setSystemInfo("Framework", "Playwright Java + JUnit 5");
        extent.attachReporter(spark);
        return extent;
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String suiteName = ctx.getTestClass()
                .map(c -> c.getSimpleName().replace("FunctionalTest", ""))
                .orElse("Unknown");
        ExtentTest test = EXTENT.createTest(
                "[" + suiteName + "] " + ctx.getDisplayName());
        CURRENT.set(test);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        CURRENT.remove();
    }

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        ExtentTest t = CURRENT.get();
        if (t != null) t.pass("PASSED");
        EXTENT.flush();
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        ExtentTest t = CURRENT.get();
        if (t != null) t.fail(cause);
        EXTENT.flush();
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        ExtentTest t = CURRENT.get();
        if (t != null) t.skip("Aborted: " + cause.getMessage());
        EXTENT.flush();
    }

    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        EXTENT.createTest(ctx.getDisplayName())
              .skip("Disabled: " + reason.orElse("no reason"));
        EXTENT.flush();
    }
}
