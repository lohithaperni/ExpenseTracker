import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { exec } from "child_process";
import { promisify } from "util";
import { fileURLToPath } from "url";
import path from "path";
import fs from "fs";

const execAsync = promisify(exec);
const __dirname  = path.dirname(fileURLToPath(import.meta.url));

// Resolve functional-tests dir: mcp-test-runner/../functional-tests
const FUNCTIONAL_TESTS_DIR = process.env.FUNCTIONAL_TESTS_DIR ||
  path.join(__dirname, "..", "functional-tests");

// mvn is mvn.cmd on Windows
const MVN = process.platform === "win32" ? "mvn.cmd" : "mvn";

// ─── MCP server ───────────────────────────────────────────────────────────────

const server = new Server(
  { name: "expense-tracker-test-runner", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

// ── Tool list ──────────────────────────────────────────────────────────────────

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "run_functional_tests",
      description:
        "Run the Playwright Java functional tests for ExpenseTracker using Maven. " +
        "Returns the full Maven output including test results.",
      inputSchema: {
        type: "object",
        properties: {
          tags: {
            type: "string",
            description:
              "Optional JUnit 5 tag filter (e.g. 'smoke', 'regression', 'negative'). " +
              "Leave empty to run all tests.",
          },
        },
        required: [],
      },
    },
    {
      name: "generate_allure_report",
      description:
        "Generate the Allure HTML report from the last test run results. " +
        "Must be called after run_functional_tests. " +
        "Returns the path to the generated report index.html.",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "get_test_summary",
      description:
        "Read the Surefire test result files and return a structured summary: " +
        "total tests, passed, failed, skipped, and per-class breakdown.",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
  ],
}));

// ── Tool handlers ──────────────────────────────────────────────────────────────

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  switch (name) {
    case "run_functional_tests":
      return handleRunTests(args?.tags || "");
    case "generate_allure_report":
      return handleGenerateReport();
    case "get_test_summary":
      return handleGetSummary();
    default:
      return {
        content: [{ type: "text", text: `Unknown tool: ${name}` }],
        isError: true,
      };
  }
});

// ── run_functional_tests ───────────────────────────────────────────────────────

async function handleRunTests(tags) {
  const tagFilter = tags ? `-Dgroups="${tags}"` : "";
  const cmd = `${MVN} test ${tagFilter} -B`.trim();

  try {
    const { stdout, stderr } = await execAsync(cmd, {
      cwd: FUNCTIONAL_TESTS_DIR,
      timeout: 300_000,
    });
    const output  = (stdout + stderr).trim();
    const summary = extractMavenSummary(output);

    return {
      content: [
        {
          type: "text",
          text:
            `COMMAND: ${cmd}\n` +
            `DIRECTORY: ${FUNCTIONAL_TESTS_DIR}\n\n` +
            `SUMMARY:\n${summary}\n\n` +
            `FULL OUTPUT:\n${output}`,
        },
      ],
    };
  } catch (error) {
    const output  = ((error.stdout || "") + (error.stderr || "")).trim();
    const summary = extractMavenSummary(output);

    return {
      content: [
        {
          type: "text",
          text:
            `COMMAND: ${cmd}\n` +
            `STATUS: FAILED\n\n` +
            `SUMMARY:\n${summary}\n\n` +
            `FULL OUTPUT:\n${output}`,
        },
      ],
      isError: true,
    };
  }
}

// ── generate_allure_report ─────────────────────────────────────────────────────

async function handleGenerateReport() {
  const cmd = `${MVN} allure:report -B`;

  try {
    const { stdout } = await execAsync(cmd, {
      cwd: FUNCTIONAL_TESTS_DIR,
      timeout: 120_000,
    });
    const reportIndex = path.join(
      FUNCTIONAL_TESTS_DIR,
      "target", "site", "allure-maven-plugin", "index.html"
    );
    const exists = fs.existsSync(reportIndex);

    return {
      content: [
        {
          type: "text",
          text:
            `STATUS: ${exists ? "SUCCESS" : "REPORT NOT FOUND"}\n` +
            `REPORT PATH: ${reportIndex}\n\n` +
            `OUTPUT:\n${stdout.trim()}`,
        },
      ],
    };
  } catch (error) {
    return {
      content: [{ type: "text", text: `STATUS: FAILED\nERROR: ${error.message}` }],
      isError: true,
    };
  }
}

// ── get_test_summary ───────────────────────────────────────────────────────────

async function handleGetSummary() {
  const surefireDir = path.join(
    FUNCTIONAL_TESTS_DIR, "target", "surefire-reports"
  );

  if (!fs.existsSync(surefireDir)) {
    return {
      content: [{ type: "text", text: "No surefire-reports found. Run the tests first." }],
      isError: true,
    };
  }

  const xmlFiles = fs.readdirSync(surefireDir)
    .filter((f) => f.endsWith(".xml") && f.startsWith("TEST-"));

  if (xmlFiles.length === 0) {
    return {
      content: [{ type: "text", text: "No test result XML files found." }],
      isError: true,
    };
  }

  let totalTests = 0, totalFailed = 0, totalErrors = 0, totalSkipped = 0;
  const classResults = [];

  for (const file of xmlFiles) {
    const xml     = fs.readFileSync(path.join(surefireDir, file), "utf-8");
    const tests   = parseInt(xml.match(/tests="(\d+)"/)?.[1]    || "0");
    const failed  = parseInt(xml.match(/failures="(\d+)"/)?.[1] || "0");
    const errors  = parseInt(xml.match(/errors="(\d+)"/)?.[1]   || "0");
    const skipped = parseInt(xml.match(/skipped="(\d+)"/)?.[1]  || "0");
    const name    = xml.match(/name="([^"]+)"/)?.[1] || file;

    totalTests   += tests;
    totalFailed  += failed;
    totalErrors  += errors;
    totalSkipped += skipped;

    const passed = tests - failed - errors - skipped;
    classResults.push(
      `  ${name.split(".").pop()}: ${tests} tests | ${passed} passed | ${failed} failed | ${errors} errors | ${skipped} skipped`
    );
  }

  const totalPassed = totalTests - totalFailed - totalErrors - totalSkipped;
  const status      = totalFailed + totalErrors === 0 ? "ALL PASSED" : "FAILURES DETECTED";

  return {
    content: [
      {
        type: "text",
        text:
          `STATUS: ${status}\n\n` +
          `TOTALS:\n` +
          `  Tests:   ${totalTests}\n` +
          `  Passed:  ${totalPassed}\n` +
          `  Failed:  ${totalFailed}\n` +
          `  Errors:  ${totalErrors}\n` +
          `  Skipped: ${totalSkipped}\n\n` +
          `PER CLASS:\n${classResults.join("\n")}`,
      },
    ],
  };
}

// ── Helpers ────────────────────────────────────────────────────────────────────

function extractMavenSummary(output) {
  const lines = output.split("\n");
  const summaryLines = lines.filter(
    (l) =>
      l.includes("Tests run:") ||
      l.includes("BUILD SUCCESS") ||
      l.includes("BUILD FAILURE") ||
      l.includes("ERROR")
  );
  return summaryLines.length > 0 ? summaryLines.join("\n") : "(no summary found)";
}

// ─── Start stdio transport ────────────────────────────────────────────────────

const transport = new StdioServerTransport();
await server.connect(transport);
