package com.fxdb.db.jdbc;

import com.fxdb.core.exception.FxDbException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Lets a user drop a JDBC driver jar fxDb doesn't ship with - most notably Oracle's ojdbc,
 * which isn't bundled by default over OTN redistribution licensing - into a "drivers" folder
 * next to the app and have it actually usable, without rebuilding or repackaging anything.
 * Called once at startup; a missing or empty directory is a normal no-op, not an error.
 */
public final class ExternalDriverLoader {

    private ExternalDriverLoader() {
    }

    public static void loadDriversFrom(Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        List<Path> jars;
        try (var stream = Files.list(directory)) {
            jars = stream.filter(p -> p.toString().endsWith(".jar")).toList();
        } catch (IOException e) {
            throw new FxDbException("Failed to list driver jars in " + directory, e);
        }
        if (jars.isEmpty()) {
            return;
        }

        URL[] urls = jars.stream().map(ExternalDriverLoader::toUrl).toArray(URL[]::new);
        URLClassLoader classLoader = new URLClassLoader(urls, ExternalDriverLoader.class.getClassLoader());

        for (Path jar : jars) {
            for (String driverClassName : readDeclaredDriverClassNames(jar)) {
                registerDriver(classLoader, driverClassName);
            }
        }
    }

    private static List<String> readDeclaredDriverClassNames(Path jar) {
        List<String> classNames = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var entry = jarFile.getEntry("META-INF/services/java.sql.Driver");
            if (entry == null) {
                return classNames;
            }
            try (InputStream in = jarFile.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.strip();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        classNames.add(trimmed);
                    }
                }
            }
        } catch (IOException e) {
            throw new FxDbException("Failed to read driver service declaration from " + jar, e);
        }
        return classNames;
    }

    private static void registerDriver(URLClassLoader classLoader, String driverClassName) {
        try {
            Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (ReflectiveOperationException e) {
            throw new FxDbException("Failed to load external JDBC driver " + driverClassName, e);
        } catch (java.sql.SQLException e) {
            throw new FxDbException("Failed to register external JDBC driver " + driverClassName, e);
        }
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (java.net.MalformedURLException e) {
            throw new FxDbException("Invalid driver jar path " + path, e);
        }
    }
}
