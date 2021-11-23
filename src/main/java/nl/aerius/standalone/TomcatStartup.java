/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.aerius.standalone;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class TomcatStartup {

  private static final String CONFIG_PORT = "TOMCAT_STANDALONE_PORT";
  private static final String CONFIG_APP_BASE = "TOMCAT_STANDALONE_APP_BASE";
  private static final String CONFIG_CONTEXT_PATH = "TOMCAT_STANDALONE_CONTEXT_PATH";
  private static final String CONFIG_CONTEXT_DIRECTORY = "TOMCAT_STANDALONE_CONTEXT_DIRECTORY";

  // Every environment variable starting with this prefix will be written to the System properties (without the prefix) to be used inside the context.xml dynamically.
  private static final String CONTEXT_PREFIX = "CONTEXT_";

  public static void main(final String[] args) throws LifecycleException, InterruptedException, ServletException {
    try {
      final Integer configStandalonePort = getSettingInt(CONFIG_PORT);
      final String configAppBase = getSetting(CONFIG_APP_BASE);
      final String configContextPath = getSetting(CONFIG_CONTEXT_PATH);
      final String configContextDirectory = getSetting(CONFIG_CONTEXT_DIRECTORY);

      // Create a temp directory to use as the app base. If overridden use the path given, otherwise the current directory will be used.
      final String appBase =
        Files.createTempDirectory(Paths.get(configAppBase == null ? "" : configAppBase).toAbsolutePath(), "standalone").toString();
      System.out.println("- Going to use appBase: " + appBase);

      setContextPropertiesFromEnvironment();

      final Tomcat tomcat = new Tomcat();
      tomcat.setPort(configStandalonePort == null ? 8080 : configStandalonePort);
      tomcat.getHost().setAppBase(appBase);
      // Enable JDNI naming (as needed by some webapps)
      tomcat.enableNaming();
      // This does more then print the Connector. getConnector() also initialises the Connector and adds it to the service.
      // We could do it ourselves but why add code already present and maintained by the Tomcat team.
      System.out.println("- Using Connector: " + tomcat.getConnector());

      System.out.println("- Using context: " + (configContextPath == null ? "/" : configContextPath));
      if (configContextDirectory == null) {
        final URI jarURI = TomcatStartup.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        System.out.println("- Going to deploy: " + jarURI.toURL().toString());
        tomcat.addWebapp(configContextPath == null ? "" : configContextPath, jarURI.toURL());
      } else {
        System.out.println("- Going to deploy: " + configContextDirectory);
        tomcat.addWebapp(configContextPath == null ? "" : configContextPath, configContextDirectory);
      }
      System.out.println();

      System.out.println("--- Start embedded Tomcat logging ---");
      tomcat.start();
      tomcat.getServer().await();
    } catch (final URISyntaxException | IOException  e) {
      System.err.println("Error starting embedded tomcat properly");
      e.printStackTrace();
    }
  }

  /**
   * Looks for ENV variables starting with CONTEXT_PREFIX and sets them
   *  (without the prefix) as System property to be used in context.xml files
   *  as present in the standalone war.
   * Tomcat will by default allow System properties to be used as variables
   *  in context.xml files.
   */
  private static void setContextPropertiesFromEnvironment() {
    System.getenv().entrySet().stream()
      .filter(x -> x.getKey().startsWith(CONTEXT_PREFIX))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).entrySet()
      .forEach(x -> setContextPropertyFromEnvironment(x.getKey(), x.getValue()));
  }

  private static void setContextPropertyFromEnvironment(final String envKey, final String envValue) {
    final String key = envKey.substring(CONTEXT_PREFIX.length());
    System.out.println("- Setting system property: " + key);
    System.setProperty(key, envValue);
  }

  /**
   * Get settings from system properties and environment variables.
   * System properties will override environment variables if both are present.
   *
   * @return String containing the setting, null if not found.
   */
  private static String getSetting(final String name) {
    String result = null;

    result = System.getProperty(name);
    if (result == null) {
      result = System.getenv(name);
    }

    return result;
  }

  /**
   * Get settings from system properties and environment variables as an integer.
   * System properties will override environment variables if both are present.
   *
   * @return If set an Integer containing the setting, null if not found.
   */
  private static Integer getSettingInt(final String name) {
    final String result = getSetting(name);

    return result == null ? null : Integer.parseInt(result);
  }

}