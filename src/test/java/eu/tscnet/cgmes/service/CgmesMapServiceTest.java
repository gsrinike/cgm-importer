package eu.tscnet.cgmes.service;

import eu.tscnet.cgmes.model.GeoJsonFeatureCollection;
import eu.tscnet.cgmes.model.ParseSummary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CgmesMapServiceTest {
    private final CgmesMapService service = new CgmesMapService();

    @Test
    void summarizesAndExtractsGeoJsonFeatures() {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new ByteArrayInputStream("""
                @prefix cim: <http://iec.ch/TC57/2013/CIM-schema-cim16#> .
                @prefix ex: <http://example.test/> .
                ex:s1 a cim:Substation ;
                  cim:IdentifiedObject.name "North" ;
                  cim:latitude "52.5" ;
                  cim:longitude "13.4" .
                """.getBytes(StandardCharsets.UTF_8)), Lang.TURTLE);

        ParseSummary summary = service.summary(model);
        GeoJsonFeatureCollection features = service.features(model);

        assertThat(summary.triples()).isEqualTo(4);
        assertThat(summary.equipmentCounts()).containsEntry("Substation", 1L);
        assertThat(features.features()).hasSize(1);
        assertThat(features.features().getFirst().geometry().coordinates()).containsExactly(13.4, 52.5);
        assertThat(features.features().getFirst().properties().name()).isEqualTo("North");
    }
}
