package org.testng.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.IClass;
import org.testng.IInstanceInfo;
import org.testng.ITestContext;
import org.testng.ITestObjectFactory;
import org.testng.TestNGException;
import org.testng.annotations.IAnnotation;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.annotations.AnnotationHelper;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import static org.testng.internal.ClassHelper.getAvailableMethods;

/**
 * This class creates an ITestClass from a test class.
 *
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
public class TestNGClassFinder extends BaseClassFinder {

  private final ITestContext m_testContext;
  private final Map<Class<?>, List<Object>> m_instanceMap = Maps.newHashMap();

  public TestNGClassFinder(ClassInfoMap cim,
                           XmlTest xmlTest,
                           IConfiguration configuration,
                           ITestContext testContext)
  {
    this(cim,  Maps.<Class<?>, List<Object>>newHashMap(), xmlTest, configuration, testContext);
  }

  public TestNGClassFinder(ClassInfoMap cim,
                           Map<Class<?>, List<Object>> instanceMap,
                           XmlTest xmlTest,
                           IConfiguration configuration,
                           ITestContext testContext)
  {
    m_testContext = testContext;

    if (instanceMap == null) {
      throw new IllegalArgumentException("instanceMap must not be null");
    }

    IAnnotationFinder annotationFinder = configuration.getAnnotationFinder();
    ITestObjectFactory objectFactory = configuration.getObjectFactory();

    // Find all the new classes and their corresponding instances
    Set<Class<?>> allClasses= cim.getClasses();

    //very first pass is to find ObjectFactory, can't create anything else until then
    if(objectFactory == null) {
      objectFactory = new ObjectFactoryImpl();
      outer:
      for (Class<?> cls : allClasses) {
        try {
          if (null != cls) {
            Method[] ms;
            try {
              ms = cls.getMethods();
            } catch (NoClassDefFoundError e) {
              // https://github.com/cbeust/testng/issues/602
              Utils.log("TestNGClassFinder", 5, "[WARN] Can't link and determine methods of " + cls + "(" + e.getMessage() + ")");
              ms = new Method[0];
            }
            for (Method m : ms) {
              IAnnotation a = annotationFinder.findAnnotation(m,
                  org.testng.annotations.IObjectFactoryAnnotation.class);
              if (null != a) {
                if (!ITestObjectFactory.class.isAssignableFrom(m.getReturnType())) {
                  throw new TestNGException("Return type of " + m + " is not IObjectFactory");
                }
                try {
                  Object instance = cls.newInstance();
                  if (m.getParameterTypes().length > 0 && m.getParameterTypes()[0].equals(ITestContext.class)) {
                    objectFactory = (ITestObjectFactory) m.invoke(instance, testContext);
                  } else {
                    objectFactory = (ITestObjectFactory) m.invoke(instance);
                  }
                  break outer;
                }
                catch (Exception ex) {
                  throw new TestNGException("Error creating object factory: " + cls,
                      ex);
                }
              }
            }
          }
        } catch (NoClassDefFoundError e) {
          Utils.log("[TestNGClassFinder]", 1, "Unable to read methods on class " + cls.getName()
              + " - unable to resolve class reference " + e.getMessage());
          for (XmlClass xmlClass : xmlTest.getXmlClasses()) {
            if (xmlClass.loadClasses() && xmlClass.getName().equals(cls.getName())) {
              throw e;
            }
          }

        }
      }
    }

    for(Class<?> cls : allClasses) {
      if (null == cls) {
        Utils.log("TestNGClassFinder", 5, "[WARN] FOUND NULL CLASS");
        continue;
      }

      if(isTestNGClass(cls, annotationFinder)) {
        List<Object> allInstances = instanceMap.get(cls);
        Object thisInstance = (allInstances != null && !allInstances.isEmpty()) ? allInstances.get(0) : null;

        // If annotation class and instances are abstract, skip them
        if ((null == thisInstance) && Modifier.isAbstract(cls.getModifiers())) {
          Utils.log("", 5, "[WARN] Found an abstract class with no valid instance attached: " + cls);
          continue;
        }

        IClass ic= findOrCreateIClass(m_testContext, cls, cim.getXmlClass(cls), thisInstance,
            xmlTest, annotationFinder, objectFactory);
        if(null != ic) {
          putIClass(cls, ic);

          List<ConstructorOrMethod> factoryMethods = ClassHelper.findDeclaredFactoryMethods(cls, annotationFinder);
          for (ConstructorOrMethod factoryMethod : factoryMethods) {
            if (factoryMethod.getEnabled()) {
              Object[] theseInstances = ic.getInstances(false);
              if (theseInstances.length == 0) {
                theseInstances = ic.getInstances(true);
              }

              Object instance = theseInstances.length != 0 ? theseInstances[0] : null;
              FactoryMethod fm = new FactoryMethod(
                      factoryMethod,
                      instance,
                      xmlTest,
                      annotationFinder,
                      m_testContext, objectFactory);
              ClassInfoMap moreClasses = new ClassInfoMap();

              // If the factory returned IInstanceInfo, get the class from it,
              // otherwise, just call getClass() on the returned instances
              int i = 0;
              for (Object o : fm.invoke()) {
                if (o == null) {
                  throw new TestNGException("The factory " + fm + " returned a null instance" +
                          "at index " + i);
                }
                Class<?> oneMoreClass;
                if(IInstanceInfo.class.isAssignableFrom(o.getClass())) {
                  IInstanceInfo<?> ii = (IInstanceInfo) o;
                  addInstance(ii);
                  oneMoreClass = ii.getInstanceClass();
                } else {
                  addInstance(o);
                  oneMoreClass = o.getClass();
                }
                if(!classExists(oneMoreClass)) {
                  moreClasses.addClass(oneMoreClass);
                }
                i++;
              }

              if(moreClasses.getSize() > 0) {
                TestNGClassFinder finder =
                        new TestNGClassFinder(moreClasses,
                                m_instanceMap,
                                xmlTest,
                                configuration,
                                m_testContext);

                for(IClass ic2 : finder.findTestClasses()) {
                  putIClass(ic2.getRealClass(), ic2);
                }
              } // if moreClasses.size() > 0
            }
          }
        } // null != ic
      } // if not TestNG class
      else {
        Utils.log("TestNGClassFinder", 3, "SKIPPING CLASS " + cls + " no TestNG annotations found");
      }
    } // for

    //
    // Add all the instances we found to their respective IClasses
    //
    for(Map.Entry<Class<?>, List<Object>> entry : m_instanceMap.entrySet()) {
      Class<?> clazz = entry.getKey();
      for(Object instance : entry.getValue()) {
        IClass ic= getIClass(clazz);
        if(null != ic) {
          ic.addInstance(instance);
        }
      }
    }
  }

  /**
   * @return true if this class contains TestNG annotations (either on itself
   * or on a superclass).
   */
  private static boolean isTestNGClass(Class<?> c, IAnnotationFinder annotationFinder) {
    Class<?> cls = c;

    try {
      for(Class<? extends IAnnotation> annotation : AnnotationHelper.getAllAnnotations()) {
        for (cls = c; cls != null; cls = cls.getSuperclass()) {
          // Try on the methods
          for (Method m : getAvailableMethods(cls)) {
            IAnnotation ma= annotationFinder.findAnnotation(m, annotation);
            if(null != ma) {
              return true;
            }
          }

          // Try on the class
          IAnnotation a= annotationFinder.findAnnotation(cls, annotation);
          if(null != a) {
            return true;
          }

          // Try on the constructors
          for (Constructor ctor : cls.getConstructors()) {
            IAnnotation ca= annotationFinder.findAnnotation(ctor, annotation);
            if(null != ca) {
              return true;
            }
          }
        }
      }

      return false;

    } catch (NoClassDefFoundError e) {
      Utils.log("[TestNGClassFinder]", 1,
          "Unable to read methods on class " + cls.getName()
          + " - unable to resolve class reference " + e.getMessage());
      return false;
    }
  }

  // IInstanceInfo<T> should be replaced by IInstanceInfo<?> but eclipse complains against it: https://github.com/cbeust/testng/issues/1070
  private <T> void addInstance(IInstanceInfo<T> ii) {
    addInstance(ii.getInstanceClass(), ii.getInstance());
  }

  private void addInstance(Object o) {
    addInstance(o.getClass(), o);
  }

  // Class<S> should be replaced by Class<? extends T> but java doesn't fail as expected: https://github.com/cbeust/testng/issues/1070
  private <T, S extends T> void addInstance(Class<S> clazz, T instance) {
    List<Object> instances = m_instanceMap.get(clazz);

    if (instances == null) {
      instances = Lists.newArrayList();
      m_instanceMap.put(clazz, instances);
    }

    instances.add(instance);
  }
}
