package org.testng.reporters;

import static org.testng.internal.Utils.isStringNotBlank;

import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.internal.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple reporter that collects the results and prints them on standard out.
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class TextReporter extends TestListenerAdapter {

  private final int m_verbose;
  private final String m_testName;

  public TextReporter(String testName, int verbose) {
    m_testName = testName;
    m_verbose = verbose;
  }

  @Override
  public void onFinish(ITestContext context) {
    if (m_verbose >= 2) {
      logResults();
    }
  }

  private static List<ITestNGMethod> resultsToMethods(List<ITestResult> results) {
    List<ITestNGMethod> result = new ArrayList<>(results.size());
    for (ITestResult tr : results) {
      result.add(tr.getMethod());
    }

    return result;
  }

  private void logResults() {
    // Log Text
    for (ITestResult tr : getConfigurationFailures()) {
      Throwable ex = tr.getThrowable();
      String stackTrace = "";
      if (ex != null && m_verbose >= 2) {
        stackTrace = Utils.stackTrace(ex, false)[0];
      }

      logResult("FAILED CONFIGURATION",
          Utils.detailedMethodName(tr.getMethod(), false),
          tr.getMethod().getDescription(),
          stackTrace,
          tr.getParameters(),
          tr.getMethod().getConstructorOrMethod().getParameterTypes()
      );
    }

    for (ITestResult tr : getConfigurationSkips()) {
      logResult("SKIPPED CONFIGURATION",
          Utils.detailedMethodName(tr.getMethod(), false),
          tr.getMethod().getDescription(),
          null,
          tr.getParameters(),
          tr.getMethod().getConstructorOrMethod().getParameterTypes()
      );
    }

    for (ITestResult tr : getPassedTests()) {
      logResult("PASSED", tr, null);
    }

    for (ITestResult tr : getFailedTests()) {
      Throwable ex = tr.getThrowable();
      String stackTrace= "";
      if (ex != null && m_verbose >= 2) {
        stackTrace= Utils.stackTrace(ex, false)[0];
      }

      logResult("FAILED", tr, stackTrace);
    }

    for (ITestResult tr : getSkippedTests()) {
      Throwable throwable = tr.getThrowable();
      logResult("SKIPPED", tr, throwable != null ? Utils.stackTrace(throwable, false)[0] : null);
    }

    List<ITestNGMethod> ft = resultsToMethods(getFailedTests());
    StringBuilder logBuf = new StringBuilder("\n===============================================\n");
    logBuf.append("    ").append(m_testName).append("\n");
    logBuf.append("    Tests run: ").append(Utils.calculateInvokedMethodCount(getAllTestMethods()))
        .append(", Failures: ").append(Utils.calculateInvokedMethodCount(ft))
        .append(", Skips: ").append(Utils.calculateInvokedMethodCount(resultsToMethods(getSkippedTests())));
    int confFailures = getConfigurationFailures().size();
    int confSkips = getConfigurationSkips().size();
    if(confFailures > 0 || confSkips > 0) {
      logBuf.append("\n").append("    Configuration Failures: ").append(confFailures)
          .append(", Skips: ").append(confSkips);
    }
    logBuf.append("\n===============================================\n");
    logResult("", logBuf.toString());
  }

  private void logResult(String status, ITestResult tr, String stackTrace) {
    logResult(status, tr.getName(), tr.getMethod().getDescription(), stackTrace,
        tr.getParameters(), tr.getMethod().getConstructorOrMethod().getParameterTypes());
  }

  private void logResult(String status, String message) {
    StringBuilder buf = new StringBuilder();
    if(isStringNotBlank(status)) {
      buf.append(status).append(": ");
    }
    buf.append(message);

    System.out.println(buf);
  }

  private void logResult(String status, String name,
          String description, String stackTrace,
          Object[] params, Class<?>[] paramTypes) {
    StringBuilder msg = new StringBuilder(name);

    if (null != params && params.length > 0) {
      msg.append("(");

      // The error might be a data provider parameter mismatch, so make
      // a special case here
      if (params.length != paramTypes.length) {
        msg.append(name).append(": Wrong number of arguments were passed by ")
                .append("the Data Provider: found ").append(params.length).append(" but ")
                .append("expected ").append(paramTypes.length)
                .append(")");
      } else {
        for (int i= 0; i < params.length; i++) {
          if (i > 0) {
            msg.append(", ");
          }
          msg.append(Utils.toString(params[i], paramTypes[i]));
        }

        msg.append(")");
      }
    }
    if (!Utils.isStringEmpty(description)) {
      msg.append("\n");
      for (int i = 0; i < status.length() + 2; i++) {
        msg.append(" ");
      }
      msg.append(description);
    }
    if (!Utils.isStringEmpty(stackTrace)) {
      msg.append("\n").append(stackTrace);
    }

    logResult(status, msg.toString());
  }
}
