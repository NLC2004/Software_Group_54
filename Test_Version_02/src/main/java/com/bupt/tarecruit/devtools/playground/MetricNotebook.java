package com.bupt.tarecruit.devtools.playground;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MetricNotebook {
    private final Map<String, MetricSeries> seriesMap;

    public MetricNotebook() {
        this.seriesMap = new LinkedHashMap<>();
    }

    public MetricSeries registerSeries(String name) {
        String normalized = requireText(name, "name");
        return seriesMap.computeIfAbsent(normalized, MetricSeries::new);
    }

    public boolean containsSeries(String name) {
        return seriesMap.containsKey(safeText(name));
    }

    public MetricSeries getSeries(String name) {
        MetricSeries series = seriesMap.get(safeText(name));
        return series == null ? null : series.copy();
    }

    public List<String> getSeriesNames() {
        return Collections.unmodifiableList(new ArrayList<>(seriesMap.keySet()));
    }

    public MetricNotebook addPoint(String seriesName, LocalDate day, double value) {
        registerSeries(seriesName).addPoint(day, value);
        return this;
    }

    public MetricNotebook fillSequence(String seriesName, LocalDate start, int length, double firstValue, double step) {
        MetricSeries series = registerSeries(seriesName);
        for (int i = 0; i < Math.max(0, length); i++) {
            series.addPoint(start.plusDays(i), firstValue + step * i);
        }
        return this;
    }

    public MetricSummary buildSummary(String seriesName) {
        MetricSeries series = seriesMap.get(safeText(seriesName));
        if (series == null) {
            return MetricSummary.empty();
        }
        return series.buildSummary();
    }

    public List<MetricPoint> movingAverage(String seriesName, int window) {
        MetricSeries series = seriesMap.get(safeText(seriesName));
        if (series == null) {
            return Collections.emptyList();
        }
        return series.movingAverage(window);
    }

    public Map<String, MetricSummary> overallSummary() {
        Map<String, MetricSummary> result = new LinkedHashMap<>();
        for (Map.Entry<String, MetricSeries> entry : seriesMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().buildSummary());
        }
        return Collections.unmodifiableMap(result);
    }

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

    public MetricNotebook clear() {
        seriesMap.clear();
        return this;
    }

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

    public static final class MetricSeries {
        private final String name;
        private final List<MetricPoint> points;

        public MetricSeries(String name) {
            this.name = requireText(name, "name");
            this.points = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public List<MetricPoint> getPoints() {
            List<MetricPoint> copy = new ArrayList<>(points);
            copy.sort(Comparator.comparing(MetricPoint::getDay));
            return Collections.unmodifiableList(copy);
        }

        public MetricSeries addPoint(LocalDate day, double value) {
            points.add(new MetricPoint(day, value));
            points.sort(Comparator.comparing(MetricPoint::getDay));
            return this;
        }

        public MetricPoint latest() {
            if (points.isEmpty()) {
                return null;
            }
            return getPoints().get(getPoints().size() - 1);
        }

        public List<Double> values() {
            List<Double> values = new ArrayList<>();
            for (MetricPoint point : getPoints()) {
                values.add(point.getValue());
            }
            return Collections.unmodifiableList(values);
        }

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

        public MetricSeries copy() {
            MetricSeries copy = new MetricSeries(name);
            for (MetricPoint point : points) {
                copy.points.add(point.copy());
            }
            return copy;
        }
    }

    public static final class MetricPoint {
        private final LocalDate day;
        private final double value;

        public MetricPoint(LocalDate day, double value) {
            this.day = Objects.requireNonNull(day, "day");
            this.value = value;
        }

        public LocalDate getDay() {
            return day;
        }

        public double getValue() {
            return value;
        }

        public MetricPoint copy() {
            return new MetricPoint(day, value);
        }
    }

    public static final class MetricSummary {
        private final int count;
        private final double min;
        private final double max;
        private final double average;
        private final double firstValue;
        private final double lastValue;

        public MetricSummary(int count, double min, double max, double average, double firstValue, double lastValue) {
            this.count = count;
            this.min = min;
            this.max = max;
            this.average = average;
            this.firstValue = firstValue;
            this.lastValue = lastValue;
        }

        public static MetricSummary empty() {
            return new MetricSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public int getCount() {
            return count;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getAverage() {
            return average;
        }

        public double getFirstValue() {
            return firstValue;
        }

        public double getLastValue() {
            return lastValue;
        }

        public double getDelta() {
            return lastValue - firstValue;
        }
    }
}
