package eu.tscnet.cgmes.model;

public record ShaclValidationResponse(boolean conforms, String resultsText, String resultsTurtle) {}
