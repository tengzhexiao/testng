package org.testng;


/**
 * This class describes the result of a test.
 *
 * @author Cedric Beust, May 2, 2004
 * @since May 2, 2004
 * @version $Revision: 721 $, $Date: 2009-05-23 09:55:46 -0700 (Sat, 23 May 2009) $
 *
 */
public interface ITestResult extends IAttributes, Comparable<ITestResult> {

  // Test status
  int SUCCESS = 1;
  int FAILURE = 2;
  int SKIP = 3;
  int SUCCESS_PERCENTAGE_FAILURE = 4;
  int STARTED= 16;

  /**
   * @return The status of this result, using one of the constants
   * above.
   */
  int getStatus();
  void setStatus(int status);

  /**
   * @return The test method this result represents.
   */
  ITestNGMethod getMethod();

  /**
   * @return The parameters this method was invoked with.
   */
  Object[] getParameters();
  void setParameters(Object[] parameters);

  /**
   * @return The test class used this object is a result for.
   */
  IClass getTestClass();

  /**
   * @return The throwable that was thrown while running the
   * method, or null if no exception was thrown.
   */
  Throwable getThrowable();
  void setThrowable(Throwable throwable);

  /**
   * @return the start date for this test, in milliseconds.
   */
  long getStartMillis();

  /**
   * @return the end date for this test, in milliseconds.
   */
  long getEndMillis();
  void setEndMillis(long millis);

  /**
   * @return The name of this TestResult, typically identical to the name
   * of the method.
   */
  String getName();

  /**
   * @return true if if this test run is a SUCCESS
   */
  boolean isSuccess();

  /**
   * @return The host where this suite was run, or null if it was run locally.  The
   * returned string has the form:  host:port
   */
  String getHost();

  /**
   * The instance on which this method was run.
   */
  Object getInstance();

  /**
   * If this result's related instance implements ITest or use @Test(testName=...), returns its test name, otherwise returns null.
   */
  String getTestName();

  String getInstanceName();
  
  /**
   * @return the {@link ITestContext} for this test result.
   */
  ITestContext getTestContext();
}
