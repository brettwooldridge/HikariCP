package com.zaxxer.hikari.metrics.prometheus;

import java.util.List;
import java.util.stream.Collectors;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot.HistogramDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.SummarySnapshot.SummaryDataPointSnapshot;

public class Samples {
	private static DataPointSnapshot getSnapshotValue(PrometheusRegistry registry, String name, String[] labelNames, String[] labelValues) {
		MetricSnapshots metricSnapshots = registry.scrape(s -> s.equals(name));
		Labels labels = Labels.of(labelNames, labelValues);
		List<? extends DataPointSnapshot> snapshots = metricSnapshots.stream()
				.flatMap(metricSnapshot -> metricSnapshot.getDataPoints().stream())
				.collect(Collectors.toList());
		if (!labels.isEmpty()) {
			snapshots = snapshots.stream()
					.filter(dataPointSnapshot ->
							dataPointSnapshot.getLabels().hasSameNames(labels) &&
							dataPointSnapshot.getLabels().hasSameValues(labels))
					.toList();
		}
		if (snapshots.isEmpty()) {
			return null;
		}
		return snapshots.getFirst();
	}
	
	static Double getSampleValue(PrometheusRegistry registry, String name, String[] labelNames, String[] labelValues) {
		return switch (getSnapshotValue(registry, name, labelNames, labelValues)) {
			case GaugeDataPointSnapshot gauge -> gauge.getValue();
			case CounterDataPointSnapshot counter -> counter.getValue();
			case null -> null;
			default ->
					throw new IllegalStateException("Unexpected snapshot value: " + getSnapshotValue(registry, name, labelNames, labelValues));
		};
	}
	
	static Long getSampleCountValue(PrometheusRegistry registry, String name, String[] labelNames, String[] labelValues) {
		return switch (getSnapshotValue(registry, name, labelNames, labelValues)) {
			case HistogramDataPointSnapshot histogram -> histogram.getCount();
			case SummaryDataPointSnapshot summary -> summary.getCount();
			case null -> null;
			default ->
					throw new IllegalStateException("Unexpected snapshot value: " + getSnapshotValue(registry, name, labelNames, labelValues));
		};
	}
	
	static Double getSampleSumValue(PrometheusRegistry registry, String name, String[] labelNames, String[] labelValues) {
		return switch (getSnapshotValue(registry, name, labelNames, labelValues)) {
			case HistogramDataPointSnapshot histogram -> histogram.getSum();
			case SummaryDataPointSnapshot summary -> summary.getSum();
			case null -> null;
			default ->
					throw new IllegalStateException("Unexpected snapshot value: " + getSnapshotValue(registry, name, labelNames, labelValues));
		};
	}
}
