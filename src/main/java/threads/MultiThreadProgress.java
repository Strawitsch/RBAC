package threads;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MultiThreadProgress {
    private static final int NUM_THREADS = 5;
    private static final int TOTAL_STEPS = 50;
    private static final int STEP_DELAY_MS = 50;
    private static final int RENDER_DELAY_MS = 100;

    private static class ThreadState {
        final int index;
        final Thread thread;
        final AtomicInteger currentStep = new AtomicInteger(0);
        final AtomicLong startTime = new AtomicLong(0);
        final AtomicLong finishTime = new AtomicLong(0);
        boolean finished = false;

        ThreadState(int index, Thread thread) {
            this.index = index;
            this.thread = thread;
        }
    }

    private static final ThreadState[] states = new ThreadState[NUM_THREADS];

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < NUM_THREADS; i++) {
            final int idx = i;
            Thread worker = new Thread(() -> runWorker(idx));
            states[i] = new ThreadState(i, worker);
        }

        Thread renderer = new Thread(() -> runRenderer());
        renderer.setDaemon(true);
        renderer.start();

        for (ThreadState state : states) {
            state.thread.start();
        }

        for (ThreadState state : states) {
            state.thread.join();
        }

        Thread.sleep(RENDER_DELAY_MS + 50);

        System.out.println("\nВсе потоки завершены.\n");
        for (ThreadState state : states) {
            long duration = state.finishTime.get() - state.startTime.get();
            System.out.printf("Поток %d (id=%d): время выполнения = %d ms%n",
                    state.index + 1, state.thread.getId(), duration);
        }
    }

    private static void runWorker(int idx) {
        ThreadState state = states[idx];
        state.startTime.set(System.currentTimeMillis());

        for (int step = 0; step <= TOTAL_STEPS; step++) {
            state.currentStep.set(step);
            try {
                Thread.sleep(STEP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        state.finishTime.set(System.currentTimeMillis());
        state.finished = true;
    }

    private static void runRenderer() {
        boolean allFinished = false;
        while (!allFinished) {
            StringBuilder sb = new StringBuilder();
            sb.append("\u001B[H");
            sb.append("\u001B[2J");

            allFinished = true;
            for (ThreadState state : states) {
                if (!state.finished) {
                    allFinished = false;
                }
                long now = state.finishTime.get() != 0 ? state.finishTime.get() : System.currentTimeMillis();
                long elapsed = state.startTime.get() != 0 ? now - state.startTime.get() : 0;

                sb.append(formatLine(state, elapsed));
                sb.append("\n");
            }

            System.out.print(sb.toString());

            try {
                Thread.sleep(RENDER_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static String formatLine(ThreadState state, long elapsedMs) {
        int step = state.currentStep.get();
        int percent = (step * 100) / TOTAL_STEPS;
        int width = 50;
        int filled = (step * width) / TOTAL_STEPS;
        String bar = "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";

        String line = String.format("Поток %-2d (id=%-3d) %s %3d%%  elapsed: %4d ms",
                state.index + 1,
                state.thread.getId(),
                bar,
                percent,
                elapsedMs);
        return line;
    }
}