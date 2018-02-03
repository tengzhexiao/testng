package test.groupbug;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ITCaseTwo {

  @BeforeClass
  public void beforeClassTwo() {
    System.out.printf("RUN %s.beforeClass()\n", getClass());
  }

  @AfterClass(alwaysRun = true)
  public void afterClassTwo() {
    System.out.printf("RUN %s.afterClass()\n", getClass());
  }

  @Test(groups = "std-two")
  public void two1() {
    System.out.printf("RUN %s.two1()\n", getClass());
  }

  @Test(groups = "logic-two", dependsOnGroups = "std-two")
  public void two2() {
    System.out.printf("RUN %s.two2()\n", getClass());
  }

}