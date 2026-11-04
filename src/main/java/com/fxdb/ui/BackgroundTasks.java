package com.fxdb.ui;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Runs a one-shot blocking call (a JDBC round-trip, typically) off the JavaFX
 * Application Thread, showing the Navigator's busy overlay while it's in flight.
 * {@code work} must not touch any JavaFX node - only {@code onSuccess}/{@code onFailure}
 * may, since Task guarantees those run back on the FX thread.
 */
public final class BackgroundTasks {

    private BackgroundTasks() {
    }

    public static <T> void run(Navigator navigator, String busyMessage, Callable<T> work,
                                Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            navigator.hideBusy();
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            navigator.hideBusy();
            onFailure.accept(task.getException());
        });

        navigator.showBusy(busyMessage);
        Thread thread = new Thread(task, "fxdb-background-task");
        thread.setDaemon(true);
        thread.start();
    }
}
