package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the metric notebook component of the TA recruitment system.
 */
public final class MetricNotebook {
    private final Map<String, MetricSeries> seriesMap;

    /**
     * Creates a new metric notebook instance.
     */
    public MetricNotebook() {
        this.seriesMap = new LinkedHashMap<>();
    }

    /**
     * Handles the register series operation.
     */
    public MetricSeries registerSeries(String name) {
        String normalized = requireText(name, "name");
        return seriesMap.computeIfAbsent(normalized, MetricSeries::new);
    }

    /**
     * Handles the contains series operation.
     */
    public boolean containsSeries(String name) {
        return seriesMap.containsKey(safeText(name));
    }

    /**
     * Handles the get series operation.
     */
    public MetricSeries getSeries(String name) {
        MetricSeries series = seriesMap.get(safeText(name));
        return series == null ? null : series.copy();
    }

    /**
     * Handles the get series names operation.
     */
    public List<String> getSeriesNames() {
        return Collections.unmodifiableList(new ArrayList<>(seriesMap.keySet()));
    }

    /**
     * Handles the add point operation.
     */
    public MetricNotebook addPoint(String seriesName, LocalDate day, double value) {
        registerSeries(seriesName).addPoint(day, value);
        return this;
    }

    /**
     * Handles the fill sequence operation.
     */
    public MetricNotebook fillSequence(String seriesName, LocalDate start, int length, double firstValue, double step) {
        MetricSeries series = registerSeries(seriesName);
        for (int i = 0; i < Math.max(0, length); i++) {
            series.addPoint(start.plusDays(i), firstValue + step * i);
        }
        return this;
    }

    /**
     * Handles the build summary operation.
     */
    public MetricSummary buildSummary(String seriesName) {
        MetricSeries series = seriesMap.get(safeText(seriesName));
        if (series == null) {
            return MetricSummary.empty();
        }
        return series.buildSummary();
    }

    /**
     * Handles the moving average operation.
     */
    public List<MetricPoint> movingAverage(String seriesName, int window) {
        MetricSeries series = seriesMap.get(safeText(seriesName));
        if (series == null) {
            return Collections.emptyList();
        }
        return series.movingAverage(window);
    }

    /**
     * Handles the overall summary operation.
     */
    public Map<String, MetricSummary> overallSummary() {
        Map<String, MetricSummary> result = new LinkedHashMap<>();
        for (Map.Entry<String, MetricSeries> entry : seriesMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().buildSummary());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Handles the merge operation.
     */
    public MetricNotebook merge(MetricNotebook other) {
        if (other == null) {
            return this;
        }
        for (String seriesName : other.seriesMap.keySet()) {
            MetricSeries current = registerSeries(seriesName);
            for (MetricPoint point : other.seriesMap.get(seriesName).getPoints()) {
                current.addPoint(point.getDay(), point.getValue());
            }
        }
        return this;
    }

    /**
     * Handles the render mini summary operation.
     */
    public String renderMiniSummary() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, MetricSummary> entry : overallSummary().entrySet()) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(entry.getKey())
                    .append(": count=").append(entry.getValue().getCount())
                    .append(", avg=").append(format(entry.getValue().getAverage()))
                    .append(", last=").append(format(entry.getValue().getLastValue()));
        }
        return builder.toString();
    }

    /**
     * Handles the clear operation.
     */
    public MetricNotebook clear() {
        seriesMap.clear();
        return this;
    }

    /**
     * Handles the copy operation.
     */
    public MetricNotebook copy() {
        MetricNotebook copy = new MetricNotebook();
        for (Map.Entry<String, MetricSeries> entry : seriesMap.entrySet()) {
            copy.seriesMap.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    private static String requireText(String value, String field) {
        String normalized = safeText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Represents the metric series component of the TA recruitment system.
     */
    public static final class MetricSeries {
        private final String name;
        private final List<MetricPoint> points;

        /**
         * Creates a new metric series instance.
         */
        public MetricSeries(String name) {
            this.name = requireText(name, "name");
            this.points = new ArrayList<>();
        }

        /**
         * Handles the get name operation.
         */
        public String getName() {
            return name;
        }

        /**
         * Handles the get points operation.
         */
        public List<MetricPoint> getPoints() {
            List<MetricPoint> copy = new ArrayList<>(points);
            copy.sort(Comparator.comparing(MetricPoint::getDay));
            return Collections.unmodifiableList(copy);
        }

        /**
         * Handles the add point operation.
         */
        public MetricSeries addPoint(LocalDate day, double value) {
            points.add(new MetricPoint(day, value));
            points.sort(Comparator.comparing(MetricPoint::getDay));
            return this;
        }

        /**
         * Handles the latest operation.
         */
        public MetricPoint latest() {
            if (points.isEmpty()) {
                return null;
            }
            return getPoints().get(getPoints().size() - 1);
        }

        /**
         * Handles the values operation.
         */
        public List<Double> values() {
            List<Double> values = new ArrayList<>();
            for (MetricPoint point : getPoints()) {
                values.add(point.getValue());
            }
            return Collections.unmodifiableList(values);
        }

        /**
         * Handles the build summary operation.
         */
        public MetricSummary buildSummary() {
            if (points.isEmpty()) {
                return MetricSummary.empty();
            }
            List<MetricPoint> ordered = getPoints();
            double min = ordered.get(0).getValue();
            double max = ordered.get(0).getValue();
            double total = 0.0;
            for (MetricPoint point : ordered) {
                min = Math.min(min, point.getValue());
                max = Math.max(max, point.getValue());
                total += point.getValue();
            }
            double average = total / ordered.size();
            double first = ordered.get(0).getValue();
            double last = ordered.get(ordered.size() - 1).getValue();
            return new MetricSummary(ordered.size(), min, max, average, first, last);
        }

        /**
         * Handles the moving average operation.
         */
        public List<MetricPoint> movingAverage(int window) {
            int safeWindow = Math.max(1, window);
            List<MetricPoint> ordered = getPoints();
            List<MetricPoint> result = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i++) {
                int start = Math.max(0, i - safeWindow + 1);
                double total = 0.0;
                int count = 0;
                for (int j = start; j <= i; j++) {
                    total += ordered.get(j).getValue();
                    count++;
                }
                result.add(new MetricPoint(ordered.get(i).getDay(), total / count));
            }
            return Collections.unmodifiableList(result);
        }

        /**
         * Handles the copy operation.
         */
        public MetricSeries copy() {
            MetricSeries copy = new MetricSeries(name);
            for (MetricPoint point : points) {
                copy.points.add(point.copy());
            }
            return copy;
        }
    }

    /**
     * Represents the metric point component of the TA recruitment system.
     */
    public static final class MetricPoint {
        private final LocalDate day;
        private final double value;

        /**
         * Creates a new metric point instance.
         */
        public MetricPoint(LocalDate day, double value) {
            this.day = Objects.requireNonNull(day, "day");
            this.value = value;
        }

        /**
         * Handles the get day operation.
         */
        public LocalDate getDay() {
            return day;
        }

        /**
         * Handles the get value operation.
         */
        public double getValue() {
            return value;
        }

        /**
         * Handles the copy operation.
         */
        public MetricPoint copy() {
            return new MetricPoint(day, value);
        }
    }

    /**
     * Represents the metric summary component of the TA recruitment system.
     */
    public static final class MetricSummary {
        private final int count;
        private final double min;
        private final double max;
        private final double average;
        private final double firstValue;
        private final double lastValue;

        /**
         * Creates a new metric summary instance.
         */
        public MetricSummary(int count, double min, double max, double average, double firstValue, double lastValue) {
            this.count = count;
            this.min = min;
            this.max = max;
            this.average = average;
            this.firstValue = firstValue;
            this.lastValue = lastValue;
        }

        /**
         * Handles the empty operation.
         */
        public static MetricSummary empty() {
            return new MetricSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        /**
         * Handles the get count operation.
         */
        public int getCount() {
            return count;
        }

        /**
         * Handles the get min operation.
         */
        public double getMin() {
            return min;
        }

        /**
         * Handles the get max operation.
         */
        public double getMax() {
            return max;
        }

        /**
         * Handles the get average operation.
         */
        public double getAverage() {
            return average;
        }

        /**
         * Handles the get first value operation.
         */
        public double getFirstValue() {
            return firstValue;
        }

        /**
         * Handles the get last value operation.
         */
        public double getLastValue() {
            return lastValue;
        }

        /**
         * Handles the get delta operation.
         */
        public double getDelta() {
            return lastValue - firstValue;
        }
    }
}
