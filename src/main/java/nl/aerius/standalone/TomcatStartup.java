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

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class TomcatStartup {

  private static final String CONFIG_PORT = "TOMCAT_STANDALONE_PORT";
  private static final String CONFIG_APP_BASE = "TOMCAT_STANDALONE_APP_BASE";
  private static final String CONFIG_CONTEXT_PATH = "TOMCAT_STANDALONE_CONTEXT_PATH";

  public static void main(final String[] args) throws LifecycleException, InterruptedException, ServletException {
    try {
      final Integer configStandalonePort = getSettingInt(CONFIG_PORT);
      final String configAppBase = getSetting(CONFIG_APP_BASE);
      final String configContextPath = getSetting(CONFIG_CONTEXT_PATH);

      // Create a temp directory to use as the app base. If overriden use the path given, otherwise the current directory will be used.
      final String appBase =
        Files.createTempDirectory(Paths.get(configAppBase == null ? "" : configAppBase).toAbsolutePath(), "standalone").toString();
      System.out.println("- Going to use appBase: " + appBase);

      final Tomcat tomcat = new Tomcat();
      tomcat.setPort(configStandalonePort == null ? 8080 : configStandalonePort);
      tomcat.getHost().setAppBase(appBase);
      // This does more then print the Connector. getConnector() also initializes the Connector and adds it to the service.
      // We could do it ourselves but why add code already present and maintained by the Tomcat team. 
      System.out.println("- Using Connector: " + tomcat.getConnector());

      final URI jarURI = TomcatStartup.class.getProtectionDomain().getCodeSource().getLocation().toURI();
      System.out.println("- Going to deploy: " + jarURI.toURL().toString());
      System.out.println("- Using context: " + (configContextPath == null ? "/" : configContextPath));
      tomcat.addWebapp(configContextPath == null ? "" : configContextPath, jarURI.toURL());
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
   * Get settings from system properties and environment variables.
   * System properties will override envrionment variables if both are present.
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
   * System properties will override envrionment variables if both are present.
   *
   * @return If set an Integer containing the setting, null if not found.
   */
  private static Integer getSettingInt(final String name) {
    final String result = getSetting(name);

    return result == null ? null : Integer.parseInt(result);
  }

}