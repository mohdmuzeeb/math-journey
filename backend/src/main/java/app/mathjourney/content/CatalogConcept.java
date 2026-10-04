package app.mathjourney.content;

import tools.jackson.databind.JsonNode;

/**
 * A validated concept file. {@code json} is the whole file as loaded; its shape is defined by
 * the content schemas (AD-5, AD-12), not by Java types.
 *
 * @param id the concept id, equal to the file name (AD-10)
 * @param land the id of the land the concept belongs to
 * @param reviewed whether the parent has skimmed it, so it may be scheduled as new (AD-13)
 * @param retired whether the concept is retired (kept only so its id stays reserved)
 * @param json the full concept file
 */
public record CatalogConcept(String id, String land, boolean reviewed, boolean retired, JsonNode json) {
}
