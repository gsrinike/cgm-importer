package eu.tscnet.cgmes.model;

import java.util.Map;

public record ParseSummary(long triples, long typedResources, Map<String, Long> equipmentCounts, Map<String, Long> topTypes) {}
