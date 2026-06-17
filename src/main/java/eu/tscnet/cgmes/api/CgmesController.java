package eu.tscnet.cgmes.api;

import eu.tscnet.cgmes.model.GeoJsonFeatureCollection;
import eu.tscnet.cgmes.model.ParseSummary;
import eu.tscnet.cgmes.model.ShaclValidationResponse;
import eu.tscnet.cgmes.service.CgmesMapService;
import eu.tscnet.cgmes.service.CgmesParserService;
import eu.tscnet.cgmes.service.CgmesRepository;
import eu.tscnet.cgmes.service.ShaclValidationService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cgmes")
@Tag(name = "CGMES Utility", description = "Load CGMES RDF, run SHACL, and expose map GeoJSON.")
public class CgmesController {
    private static final Logger log = LoggerFactory.getLogger(CgmesController.class);
    private final CgmesParserService parserService;
    private final CgmesMapService mapService;
    private final ShaclValidationService validationService;
    private final CgmesRepository repository;
    private final Tracer tracer;

    public CgmesController(CgmesParserService parserService, CgmesMapService mapService,
                           ShaclValidationService validationService, CgmesRepository repository, OpenTelemetry openTelemetry) {
        this.parserService = parserService;
        this.mapService = mapService;
        this.validationService = validationService;
        this.repository = repository;
        this.tracer = openTelemetry.getTracer("eu.tscnet.cgmes");
    }

    @PostMapping(value = "/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Load CGMES files", description = "Accepts RDF/XML, Turtle, N-Triples, N3, TriG, or ZIP archives and stores the parsed graph in memory.")
    @ApiResponse(responseCode = "200", description = "CGMES data loaded", content = @Content(schema = @Schema(implementation = ParseSummary.class)))
    public ParseSummary load(@Parameter(description = "CGMES files or ZIP archives") @RequestPart("files") List<MultipartFile> files) throws IOException {
        Span span = tracer.spanBuilder("cgmes.load").startSpan();
        try (var ignored = span.makeCurrent()) {
            Model model = parserService.parse(files);
            repository.replace(model);
            ParseSummary summary = mapService.summary(model);
            span.setAttribute("cgmes.triples", summary.triples());
            log.info("CGMES load request completed with {} triples", summary.triples());
            return summary;
        } finally {
            span.end();
        }
    }

    @GetMapping("/summary")
    @Operation(summary = "Return the loaded graph summary")
    public ResponseEntity<ParseSummary> summary() {
        return repository.currentIfLoaded()
                .map(model -> ResponseEntity.ok(mapService.summary(model)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/features")
    @Operation(summary = "Return GeoJSON map features", description = "Extracts resources with latitude/longitude or x/y coordinate properties for map display.")
    public GeoJsonFeatureCollection features() {
        return mapService.features(repository.current());
    }

    @PostMapping(value = "/shacl", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Run SHACL validation", description = "Runs an uploaded SHACL shapes graph against the currently loaded CGMES model.")
    public ResponseEntity<ShaclValidationResponse> validate(@RequestPart("shapes") MultipartFile shapes) throws IOException {
        Span span = tracer.spanBuilder("cgmes.shacl.validate").startSpan();
        try (var ignored = span.makeCurrent()) {
            return repository.currentIfLoaded()
                    .map(model -> {
                        try {
                            ShaclValidationResponse response = validationService.validate(model, shapes);
                            span.setAttribute("cgmes.shacl.conforms", response.conforms());
                            return ResponseEntity.ok(response);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } finally {
            span.end();
        }
    }
}
