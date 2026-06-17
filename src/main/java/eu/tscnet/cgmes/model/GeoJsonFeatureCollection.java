package eu.tscnet.cgmes.model;

import java.util.List;

public record GeoJsonFeatureCollection(String type, List<Feature> features) {
    public record Feature(String type, Geometry geometry, Properties properties) {}
    public record Geometry(String type, List<Double> coordinates) {}
    public record Properties(String id, String name, String type) {}
}
