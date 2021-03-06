package test.groupbug;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ITCaseOne {

  @BeforeClass
  public void beforeClassOne() {
    System.out.printf("RUN %s.beforeClass()\n", getClass());
  }

  @AfterClass(alwaysRun = true)
  public void afterClassOne() {
    System.out.printf("RUN %s.afterClass()\n", getClass());
  }

  @Test(groups = "std-one")
  public void one1() {
    System.out.printf("RUN %s.one1()\n", getClass());
  }

  /**
   * Commenting out dependsOnGroups fixes the ordering, that's the bug.
   */
  @Test(groups = "logic-one", dependsOnGroups = "std-one")
  public void one2() {
    System.out.printf("RUN %s.one2()\n", getClass());
  }

}