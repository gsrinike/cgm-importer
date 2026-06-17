package eu.tscnet.cgmes.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CgmesRepository {
    private final AtomicReference<Model> currentModel = new AtomicReference<>(ModelFactory.createDefaultModel());

    public void replace(Model model) {
        currentModel.set(model);
    }

    public Model current() {
        return currentModel.get();
    }

    public Optional<Model> currentIfLoaded() {
        Model model = currentModel.get();
        return model.isEmpty() ? Optional.empty() : Optional.of(model);
    }
}
