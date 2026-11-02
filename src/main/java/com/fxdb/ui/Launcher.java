package com.fxdb.ui;

/**
 * A plain main class that is NOT itself a javafx.application.Application subclass.
 * JavaFX refuses to start when the Application subclass is launched directly via the
 * classpath (as IntelliJ's Run button does) rather than the module path - it only checks
 * the declared main class, so routing through this indirection satisfies it. Run this class,
 * not MainApp, from an IDE; `mvn javafx:run` is unaffected either way since the plugin
 * launches via its own mechanism.
 */
public final class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
