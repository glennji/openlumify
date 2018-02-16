package org.visallo.web.structuredingest.core.util;

import com.amazonaws.services.devicefarm.model.ArgumentException;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.visallo.core.exception.VisalloException;

import java.util.concurrent.TimeUnit;

public abstract class ProgressReporter {
    private static final double REPORT_EVERY_N_SECONDS = 5.0;
    private static final long MINIMUM_MILLIS_REMAINING = 10 * 1000;
    private static final double SMOOTHING = 0.6;

    private final RateLimiter rateLimiter = RateLimiter.create(1.0 / REPORT_EVERY_N_SECONDS);
    private final double[] partPercentages;
    private final Stopwatch stopwatch = new Stopwatch();

    private Double completedPerMillisecond = null;
    private Double averageCompletedPerMillisecond = null;
    private double lastPercent = 0;
    private int partNumber = 0;
    private String lastMessage = null;

    public ProgressReporter(double[] partPercentages) {
        if (partPercentages.length > 0) {
            this.partPercentages = partPercentages;
        } else {
            throw new ArgumentException("Parts must be greater than 0");
        }
    }

    public abstract void reportThrottled(String message, long row, long totalRows, double totalPercent, String remaining);

    public final void report(String message, long row, long totalRows) {
        if (!message.equals(lastMessage)) {
            partNumber++;
            if (partNumber > partPercentages.length) {
                throw new VisalloException("Too many progress messages than expected: " + partPercentages.length);
            }
        }
        lastMessage = message;

        if (totalRows > 0 && (rateLimiter.tryAcquire() || row == totalRows)) {
            double percent = (double) row / totalRows;
            double partPercent = partPercentages[partNumber - 1];
            double previous = 0;
            for (int i = 0; i < (partNumber - 1); i++) {
                previous += partPercentages[i];
            }
            double completed = (partPercent * percent) + previous;
            String remaining = null;

            if (stopwatch.isRunning()) {
                stopwatch.stop();
                long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                completedPerMillisecond = (completed - lastPercent) / (double) millis;
                lastPercent = completed;
                if (averageCompletedPerMillisecond == null) {
                    averageCompletedPerMillisecond = completedPerMillisecond;
                } else {
                    averageCompletedPerMillisecond =
                        SMOOTHING * completedPerMillisecond +
                        (1 - SMOOTHING) * averageCompletedPerMillisecond;
                }

                if (averageCompletedPerMillisecond > 0) {
                    long millisRemaining = Math.round((1 - completed) / averageCompletedPerMillisecond);
                    if (millisRemaining > MINIMUM_MILLIS_REMAINING) {
                        remaining = DurationFormatUtils.formatDurationWords(millisRemaining, true, true);
                    }
                }

                stopwatch.reset();
            }

            reportThrottled(message, row, totalRows, completed, remaining);

            stopwatch.start();
        }
    }
}
