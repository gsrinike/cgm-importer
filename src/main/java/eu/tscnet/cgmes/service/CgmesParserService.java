package eu.tscnet.cgmes.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CgmesParserService {
    private static final Logger log = LoggerFactory.getLogger(CgmesParserService.class);

    public Model parse(List<MultipartFile> files) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename() == null ? "upload.rdf" : file.getOriginalFilename();
            log.info("Parsing CGMES upload '{}' ({} bytes)", filename, file.getSize());
            if (filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                parseZip(model, file.getInputStream());
            } else {
                parseStream(model, file.getInputStream(), filename);
            }
        }
        log.info("Loaded CGMES model with {} triples from {} uploaded file(s)", model.size(), files.size());
        return model;
    }

    private void parseZip(Model model, InputStream inputStream) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !isRdfFile(entry.getName())) {
                    continue;
                }
                log.debug("Parsing RDF entry '{}' from ZIP upload", entry.getName());
                byte[] payload = zip.readAllBytes();
                parseStream(model, new ByteArrayInputStream(payload), entry.getName());
            }
        }
    }

    private void parseStream(Model model, InputStream inputStream, String name) {
        Lang lang = languageFor(name);
        RDFDataMgr.read(model, inputStream, lang);
    }

    private boolean isRdfFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".rdf") || lower.endsWith(".xml") || lower.endsWith(".ttl")
                || lower.endsWith(".nt") || lower.endsWith(".n3") || lower.endsWith(".trig");
    }

    private Lang languageFor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ttl")) return Lang.TURTLE;
        if (lower.endsWith(".nt")) return Lang.NTRIPLES;
        if (lower.endsWith(".n3")) return Lang.N3;
        if (lower.endsWith(".trig")) return Lang.TRIG;
        return Lang.RDFXML;
    }
}
