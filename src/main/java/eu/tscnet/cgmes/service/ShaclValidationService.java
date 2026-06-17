package eu.tscnet.cgmes.service;

import eu.tscnet.cgmes.model.ShaclValidationResponse;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

@Service
public class ShaclValidationService {
    private static final Logger log = LoggerFactory.getLogger(ShaclValidationService.class);

    public ShaclValidationResponse validate(Model dataModel, MultipartFile shapesFile) throws IOException {
        String filename = shapesFile.getOriginalFilename() == null ? "shapes.ttl" : shapesFile.getOriginalFilename();
        log.info("Running SHACL validation with shapes '{}' against {} triples", filename, dataModel.size());
        Model shapesModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(shapesModel, shapesFile.getInputStream(), languageFor(filename));
        ValidationReport report = ShaclValidator.get().validate(Shapes.parse(shapesModel.getGraph()), dataModel.getGraph());
        String resultsTurtle = writeReport(report);
        String resultsText = report.conforms() ? "Conforms" : "Does not conform. See resultsTurtle for details.";
        log.info("SHACL validation completed. conforms={}", report.conforms());
        return new ShaclValidationResponse(report.conforms(), resultsText, resultsTurtle);
    }

    private String writeReport(ValidationReport report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RDFDataMgr.write(output, report.getModel(), RDFFormat.TURTLE_PRETTY);
        return output.toString();
    }

    private Lang languageFor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".rdf") || lower.endsWith(".xml")) return Lang.RDFXML;
        return Lang.TURTLE;
    }
}
