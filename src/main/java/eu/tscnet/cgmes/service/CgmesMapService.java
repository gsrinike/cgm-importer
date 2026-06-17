package eu.tscnet.cgmes.service;

import eu.tscnet.cgmes.model.GeoJsonFeatureCollection;
import eu.tscnet.cgmes.model.ParseSummary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CgmesMapService {
    private static final Set<String> EQUIPMENT_TYPES = Set.of("Substation", "VoltageLevel", "GeneratingUnit", "ACLineSegment", "PowerTransformer");

    public ParseSummary summary(Model model) {
        Map<String, Long> types = model.listStatements(null, RDF.type, (RDFNode) null).toList().stream()
                .collect(Collectors.groupingBy(statement -> localName(statement.getObject()), Collectors.counting()));
        Map<String, Long> equipmentCounts = types.entrySet().stream()
                .filter(entry -> EQUIPMENT_TYPES.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        Map<String, Long> topTypes = types.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(15)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        return new ParseSummary(model.size(), types.values().stream().mapToLong(Long::longValue).sum(), equipmentCounts, topTypes);
    }

    public GeoJsonFeatureCollection features(Model model) {
        List<GeoJsonFeatureCollection.Feature> features = model.listSubjects().toList().stream()
                .map(subject -> toFeature(model, subject))
                .flatMap(List::stream)
                .limit(1_000)
                .toList();
        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }

    private List<GeoJsonFeatureCollection.Feature> toFeature(Model model, Resource subject) {
        Map<String, RDFNode> properties = model.listStatements(subject, null, (RDFNode) null).toList().stream()
                .collect(Collectors.toMap(statement -> localName(statement.getPredicate()), statement -> statement.getObject(), (left, right) -> left));
        Double latitude = number(properties.get("latitude"), properties.get("lat"), properties.get("yPosition"));
        Double longitude = number(properties.get("longitude"), properties.get("lon"), properties.get("xPosition"));
        if (latitude == null || longitude == null) {
            return List.of();
        }
        String name = text(properties.get("IdentifiedObject.name"), properties.get("name"), properties.get("Name"));
        if (name == null || name.isBlank()) {
            name = localName(subject);
        }
        String type = model.listObjectsOfProperty(subject, RDF.type).toList().stream().findFirst().map(this::localName).orElse("Resource");
        return List.of(new GeoJsonFeatureCollection.Feature(
                "Feature",
                new GeoJsonFeatureCollection.Geometry("Point", List.of(longitude, latitude)),
                new GeoJsonFeatureCollection.Properties(subject.getURI(), name, type)));
    }

    private Double number(RDFNode... values) {
        for (RDFNode value : values) {
            String text = text(value);
            if (text == null) continue;
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                // Try the next candidate property.
            }
        }
        return null;
    }

    private String text(RDFNode... values) {
        for (RDFNode value : values) {
            if (value == null) continue;
            if (value.isLiteral()) return value.asLiteral().getLexicalForm();
            if (value.isResource()) return localName(value.asResource());
            return value.toString();
        }
        return null;
    }

    private String localName(RDFNode node) {
        if (node.isResource()) return localName(node.asResource());
        return node.toString();
    }

    private String localName(Resource resource) {
        String localName = resource.getLocalName();
        if (localName != null) return localName;
        String uri = resource.getURI();
        if (uri == null) return resource.toString();
        int hash = uri.lastIndexOf('#');
        int slash = uri.lastIndexOf('/');
        return uri.substring(Math.max(hash, slash) + 1);
    }
}
