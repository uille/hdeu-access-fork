/*
 * Copyright (C) 2024 European Union
 *  __
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the “Licence”);
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an “AS IS” basis, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */
package eu.europa.data.health.access.services;

import static eu.europa.data.health.access.Constants.CONTROLLED_VOCABULARY_COLLECTION;
import static eu.europa.data.health.access.Constants.COUNTRY_SCHEME;
import static eu.europa.data.health.access.Constants.DESCRIPTION;
import static eu.europa.data.health.access.Constants.LABEL;
import static eu.europa.data.health.access.Constants.LANGUAGE_SCHEME;
import static eu.europa.data.health.access.Constants.SUPPORTED_COUNTRY_CONCEPTS;
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGES;
import static eu.europa.data.health.access.Constants.SUPPORTED_LANGUAGE_CONCEPTS;
import static eu.europa.data.health.access.Constants.VALUE;
import static java.util.Objects.requireNonNull;

import eu.europa.data.health.access.hub_translation_client.HubTranslationClient;
import eu.europa.data.health.access.utils.TranslationUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Manages controlled vocabularies stored in MongoDB, fetching and parsing them from RDF concept
 * schemes when not already cached. Supports special handling for SPDX checksum algorithms and ISO
 * 19115 Role Codes. Also handles triggering and processing translations via the Hub Translation
 * service.
 */
public class ControlledVocabularyManager {

  private static final String CHECKSUM_SCHEME = "http://spdx.org/rdf/terms";
  private static final String FILE_TYPE_SCHEME = "http://publications.europa.eu/resource/authority/file-type";
  private static final String GML_NS = "http://www.opengis.net/gml/3.2";
  private static final String ROLE_CODE_SCHEME = "https://standards.iso.org/iso/19115/resources/Codelists/gml/CI_RoleCode.xml";
  private static final String ITEMS = "items";
  private static final String SOURCE = "source";
  private static final String SOURCE_LANG = "en";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final String collection;
  private final MongoClient mongo;
  private final WebClient client;
  private final HubTranslationClient hubTranslationClient;

  public ControlledVocabularyManager(MongoClient mongo, WebClient client,
    HubTranslationClient hubTranslationClient) {
    this(mongo, client, CONTROLLED_VOCABULARY_COLLECTION, hubTranslationClient);
  }

  public ControlledVocabularyManager(MongoClient mongo, WebClient client, String collection,
    HubTranslationClient hubTranslationClient) {
    this.mongo = requireNonNull(mongo, "mongo");
    this.client = requireNonNull(client, "client");
    this.collection = requireNonNull(collection, "collection");
    this.hubTranslationClient = requireNonNull(hubTranslationClient, "hubTranslationClient");
  }

  /**
   * Refreshes the vocabulary for the given source URL by fetching and rebuilding it, comparing it
   * to the existing document, and updating it in MongoDB if there are changes. If updated, it
   * triggers translation for missing labels.
   *
   * @param source   The RDF concept scheme URL.
   * @param existing The existing vocabulary document, or null if none.
   * @return A Future that completes when the operation is done.
   */
  public Future<Void> refreshBySource(String source, JsonObject existing) {
    requireNonNull(source, SOURCE);
    return buildFromSource(source)
      .compose(doc -> {
        if (doc == null || !doc.containsKey(ITEMS) || doc.getJsonArray(ITEMS).isEmpty()) {
          log.warn("No entries found when refreshing vocabulary for source '{}'; skipping update",
            source);
          return Future.succeededFuture(existing);
        }
        if (existing != null && docsEquivalent(existing, doc)) {
          log.debug(
            "No changes detected when refreshing vocabulary for source '{}'; skipping update",
            source
          );
          return Future.succeededFuture(existing);
        }
        return upsertBySource(source, doc).onSuccess(updated ->
          triggerTranslation(source, updated)
        );
      })
      .mapEmpty();
  }

  /**
   * Compares two vocabulary documents for equivalence, ignoring the order of items and extra
   * languages in existing labels. Used to determine if a refresh update is needed.
   *
   * @param existing The existing vocabulary document.
   * @param fresh    The freshly built vocabulary document.
   * @return True if equivalent, false otherwise.
   */
  private boolean docsEquivalent(JsonObject existing, JsonObject fresh) {
    JsonArray oldItems = existing.getJsonArray(ITEMS, new JsonArray());
    JsonArray newItems = fresh.getJsonArray(ITEMS, new JsonArray());

    // If the number of items differs -> always treat as different
    if (oldItems.size() != newItems.size()) {
      return false;
    }

    Map<String, JsonObject> oldByValue = itemsByValue(oldItems);
    Map<String, JsonObject> newByValue = itemsByValue(newItems);

    // If the value sets differ (same count but different URIs), it's different
    if (!oldByValue.keySet().equals(newByValue.keySet())) {
      return false;
    }

    // Compare text for each item; for multilingual labels, compare only langs present in *fresh*
    for (Entry<String, JsonObject> value : newByValue.entrySet()) {
      JsonObject oldItem = oldByValue.get(value.getKey());
      JsonObject newItem = value.getValue();

      Object oldLabel = oldItem.getValue(LABEL);
      Object newLabel = newItem.getValue(LABEL);

      if (!labelsEquivalentForRefresh(oldLabel, newLabel)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Compares labels for refresh purposes.
   * <ul>
   *   <li>If the fresh label is a JsonObject (multilingual), the old must be a JsonObject and must
   *       contain exactly the same texts for the languages present in fresh.</li>
   *   <li>If the fresh label is a String, the old must also be a String and equal.</li>
   * </ul>
   *
   * @param oldLabel The existing label (String or JsonObject).
   * @param newLabel The fresh label (String or JsonObject).
   * @return True if equivalent, false otherwise.
   */
  private boolean labelsEquivalentForRefresh(Object oldLabel, Object newLabel) {
    if (newLabel instanceof JsonObject freshObj) {
      if (!(oldLabel instanceof JsonObject oldObj)) {
        return false;
      }
      // Compare only langs present in fresh (ignore extra langs in old; those might be translations)
      for (String lang : freshObj.fieldNames()) {
        String freshTxt = normalize(freshObj.getString(lang));
        String oldTxt = normalize(oldObj.getString(lang));
        if (!Objects.equals(oldTxt, freshTxt)) {
          return false;
        }
      }
      return true;
    }

    if (newLabel instanceof String freshStr) {
      return (oldLabel instanceof String oldStr) && normalize(oldStr).equals(normalize(freshStr));
    }

    // Different/unknown types -> treat as different
    return false;
  }

  /**
   * Builds a map of items by their "value" field from a JsonArray of items.
   *
   * @param items The JsonArray of items.
   * @return A Map from value to item JsonObject.
   */
  private Map<String, JsonObject> itemsByValue(JsonArray items) {
    Map<String, JsonObject> map = new LinkedHashMap<>();
    for (int i = 0; i < items.size(); i++) {
      JsonObject it = items.getJsonObject(i);
      String value = it.getString(VALUE);
      if (StringUtils.isNotBlank(value)) {
        map.put(value, it);
      }
    }
    return map;
  }

  private static String normalize(String s) {
    return s == null ? null : s.trim();
  }

  /**
   * Upserts the vocabulary document by its source URL.
   *
   * @param source The RDF concept scheme URL.
   * @param doc    The vocabulary document to store.
   * @return A Future with the stored document.
   */
  private Future<JsonObject> upsertBySource(String source, JsonObject doc) {
    JsonObject query = new JsonObject().put(SOURCE, source);
    JsonObject update = new JsonObject().put("$set", doc);
    UpdateOptions opts = new UpdateOptions().setUpsert(true);

    Promise<JsonObject> promise = Promise.promise();
    mongo.updateCollectionWithOptions(collection, query, update, opts).onComplete(ur -> {
      if (ur.succeeded()) {
        promise.complete(doc);
      } else {
        promise.fail(ur.cause());
      }
    });
    return promise.future();
  }

  /**
   * Retrieves all stored controlled vocabularies from MongoDB.
   *
   * @return A Future with a list of vocabulary documents.
   */
  public Future<List<JsonObject>> getAll() {
    Promise<List<JsonObject>> promise = Promise.promise();
    mongo.find(collection, new JsonObject()).onComplete(ar -> {
      if (ar.succeeded()) {
        promise.complete(ar.result());
      } else {
        log.debug("Mongo find failed:", ar.cause());
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  /**
   * Finds the controlled vocabulary for the given RDF concept scheme URL. If absent in Mongo, it
   * fetches, builds, upserts and returns it.
   *
   * @param source The URL of the concept scheme.
   * @return A Future with an array of the vocabulary entries.
   */
  public Future<JsonArray> getBySource(String source) {
    requireNonNull(source, SOURCE);

    Promise<JsonArray> promise = Promise.promise();
    final JsonObject query = new JsonObject().put(SOURCE, source);

    mongo.findOne(collection, query, null).onComplete(ar -> {
      if (ar.failed()) {
        log.debug("Mongo findOne failed for source {}:", source, ar.cause());
        promise.fail(ar.cause());
        return;
      }
      JsonObject cached = ar.result();
      if (cached != null) {
        promise.complete(cached.getJsonArray(ITEMS, new JsonArray()));
        triggerTranslation(source, cached);
      } else {
        // If it's not cached, fetch and build it from the source
        buildFromSource(source)
          .compose(doc -> upsertBySource(source, doc))
          .onSuccess(stored -> {
            promise.complete(stored.getJsonArray(ITEMS, new JsonArray()));
            triggerTranslation(source, stored);
          })
          .onFailure(promise::fail);
      }
    });

    return promise.future();
  }

  /**
   * Triggers translation for missing labels in the vocabulary document via the Hub Translation
   * service.
   *
   * @param source The source URL of the vocabulary.
   * @param doc    The vocabulary document.
   */
  private void triggerTranslation(String source, JsonObject doc) {
    try {
      // Keep only items with valid value and multilingual label
      JsonArray items = doc.getJsonArray(ITEMS, new JsonArray()).stream()
        .filter(JsonObject.class::isInstance)
        .map(JsonObject.class::cast)
        .filter(item ->
          StringUtils.isNotBlank(item.getString(VALUE))
            && item.getValue(LABEL) instanceof JsonObject label
            && !label.isEmpty()
        )
        .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

      // Build dict
      JsonObject subject = new JsonObject();
      for (int i = 0; i < items.size(); i++) {
        JsonObject item = items.getJsonObject(i);
        String value = item.getString(VALUE);
        JsonObject label = item.getJsonObject(LABEL);

        // Choose source text: prefer "en", otherwise use the first available
        String srcText;
        String srcLang;
        if (label.containsKey(SOURCE_LANG)) {
          srcText = label.getString(SOURCE_LANG);
          srcLang = SOURCE_LANG;
        } else {
          String firstLang = label.fieldNames().iterator().next();
          srcText = label.getString(firstLang);
          srcLang = firstLang;
        }

        // Compute missing target languages
        Set<String> missing = new LinkedHashSet<>(SUPPORTED_LANGUAGES);
        missing.removeAll(label.fieldNames());
        missing.remove(srcLang);

        if (!missing.isEmpty()) {
          subject.put(value, new JsonObject()
            .put(VALUE, srcText)
            .put("sourceLanguage", srcLang)
            .put("targetLanguages", new JsonArray(new ArrayList<>(missing)))
          );
        }
      }

      if (subject.isEmpty()) {
        return; // nothing to translate
      }

      JsonObject request = new JsonObject()
        .put("id", CONTROLLED_VOCABULARY_COLLECTION + "-" + UUID.randomUUID())
        .put("data", new JsonObject()
          .put("applicationId", source) // use source so callback can route here
          .put("applicationType", CONTROLLED_VOCABULARY_COLLECTION)
          .put("collection", collection)
        )
        .put("dict", new JsonObject().put(source, subject))
        .put("callback", hubTranslationClient.createCallbackJson());

      hubTranslationClient.requestTranslation(request)
        .onSuccess(v -> log.info("Triggered vocabulary translation for {}", source))
        .onFailure(err -> log.error("Failed to trigger vocabulary translation for {}", source, err))
        .mapEmpty();
    } catch (Exception e) {
      log.error("Error while building/sending vocabulary translation request for {}", source, e);
    }
  }

  /**
   * Builds the vocabulary document from the given source URL, handling special cases.
   *
   * @param source The source URL.
   * @return A Future with the vocabulary document.
   */
  private Future<JsonObject> buildFromSource(String source) {
    if (CHECKSUM_SCHEME.equals(source)) {
      return buildFromChecksum(source);
    }
    if (ROLE_CODE_SCHEME.equals(source)) {
      return buildFromRoleCode(source);
    }
    return buildFromRDF(source);
  }

  /**
   * Builds the vocabulary document for SPDX checksum algorithms from the HTML at the given URL.
   * Builds items as:
   * <pre>{@code
   * {
   *  "value": "<a title>",
   *  "label": "<algorithm name>"
   * }
   * }</pre>
   *
   * @param url The SPDX terms URL.
   * @return A Future with the vocabulary document.
   */
  private Future<JsonObject> buildFromChecksum(String url) {
    Promise<JsonObject> promise = Promise.promise();

    client.getAbs(url).send(ar -> {
      if (ar.failed()) {
        promise.fail(ar.cause());
        return;
      }
      var resp = ar.result();
      if (resp.statusCode() != 200) {
        promise.fail("Failed to fetch SPDX checksum page (" + resp.statusCode() + "): " + url);
        return;
      }

      try {
        // Parse HTML with Jsoup
        String html = resp.bodyAsString();
        org.jsoup.nodes.Document doc = Jsoup.parse(html, url);
        Elements links = doc.select("#d4e2129 > dl > dd > a[title]");

        JsonArray items = new JsonArray();
        for (org.jsoup.nodes.Element a : links) {
          String value = a.attr("title"); // e.g. http://spdx.org/rdf/terms#checksumAlgorithm_sha256
          if (StringUtils.isBlank(value)) {
            continue;
          }

          // Extract algorithm name from fragment, e.g. checksumAlgorithm_sha256 -> SHA256
          String label = value.substring(value.lastIndexOf('_') + 1).toUpperCase(Locale.ROOT);

          // Add hyphens for readability: SHA256 -> SHA-256, BLAKE2B256 -> BLAKE2b-256
          label = label.replaceFirst("^SHA(\\d+)$", "SHA-$1");
          label = label.replaceFirst("^BLAKE2B(\\d+)$", "BLAKE2b-$1");

          items.add(new JsonObject().put(VALUE, value).put(LABEL, label));
        }

        promise.complete(new JsonObject().put(SOURCE, url).put(ITEMS, items));
      } catch (Exception e) {
        log.error("Failed to parse SPDX checksum HTML for {}. Head: {}", url, safeHead(resp.body()),
          e);
        promise.fail(e);
      }
    });

    return promise.future();
  }

  /**
   * Builds the vocabulary document for ISO 19115 Role Codes from the GML Dictionary at the given
   * URL. It builds items as:
   * <pre>{@code
   * {
   *   "value": "<source>#<identifier>",
   *   "label": { "en": "<Title Cased Identifier> – <Capitalized description>" }
   * }
   * }></pre>
   *
   * @param url The ISO CI_RoleCode URL.
   * @return A Future with the vocabulary document.
   */
  private Future<JsonObject> buildFromRoleCode(String url) {
    Promise<JsonObject> promise = Promise.promise();

    client.getAbs(url).send(ar -> {
      if (ar.failed()) {
        promise.fail(ar.cause());
        return;
      }
      HttpResponse<Buffer> resp = ar.result();
      if (resp.statusCode() != 200) {
        promise.fail("Failed to fetch ISO CI_RoleCode (" + resp.statusCode() + "): " + url);
        return;
      }

      try {
        Document doc = parseXML(resp.body());
        NodeList definitions = doc.getElementsByTagNameNS(GML_NS, "Definition");
        JsonArray items = new JsonArray();

        for (int i = 0; i < definitions.getLength(); i++) {
          Element def = (Element) definitions.item(i);
          String identifier = firstChildText(def, GML_NS, "identifier");
          String description = firstChildText(def, GML_NS, DESCRIPTION);

          if (StringUtils.isBlank(identifier)) {
            continue; // skip malformed entries
          }

          // Title Case the identifier, splitting camel case words with spaces
          // E.g. resourceProvider -> Resource Provider
          String title = StringUtils
            .capitalize(identifier.replaceAll("([a-z])([A-Z])", "$1 $2"))
            .trim();
          String desc = capitalizeFirst(description == null ? "" : description.trim());

          String enLabel = desc.isEmpty() ? title : (title + " – " + desc);
          JsonObject label = new JsonObject().put("en", enLabel);

          items.add(new JsonObject().put(VALUE, url + "#" + identifier).put(LABEL, label));
        }

        promise.complete(new JsonObject().put(SOURCE, url).put(ITEMS, items));
      } catch (Exception e) {
        log.error("Failed to parse ISO CI_RoleCode XML for {}. Head: {}", url,
          safeHead(resp.body()), e);
        promise.fail(e);
      }
    });

    return promise.future();
  }

  /**
   * Parses an XML document from a buffer.
   *
   * @param body The buffer containing the XML.
   * @return The parsed Document.
   */
  private Document parseXML(Buffer body)
    throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setExpandEntityReferences(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // avoid XXE
    DocumentBuilder builder = factory.newDocumentBuilder();
    try (ByteArrayInputStream in = new ByteArrayInputStream(body.getBytes())) {
      return builder.parse(in);
    }
  }

  /**
   * Returns the text content of the first child element with the given namespace and local name.
   *
   * @param parent    The parent element.
   * @param ns        The namespace URI.
   * @param localName The local name of the child element.
   * @return The text content or null if not found.
   */
  private static String firstChildText(Element parent, String ns, String localName) {
    NodeList nodeList = parent.getElementsByTagNameNS(ns, localName);
    if (nodeList.getLength() == 0) {
      return null;
    }
    Node node = nodeList.item(0);
    return node != null ? node.getTextContent() : null;
  }

  /**
   * Capitalizes the first character of a string.
   *
   * @param str The string to capitalize.
   * @return The capitalized string or an empty string if the input is blank.
   */
  private static String capitalizeFirst(String str) {
    if (StringUtils.isBlank(str)) {
      return "";
    }
    char first = str.charAt(0);
    if (Character.isUpperCase(first)) {
      return str;
    }
    return Character.toUpperCase(first) + str.substring(1);
  }

  /**
   * Builds the vocabulary document from the RDF concept scheme at the given URL.
   *
   * @param url The RDF concept scheme URL.
   * @return A Future with the vocabulary document.
   */
  private Future<JsonObject> buildFromRDF(String url) {
    final boolean multilingual = !FILE_TYPE_SCHEME.equals(url);

    // Fetch scheme model
    return fetchRDFModel(url).compose(model -> {
      // Extract concept URIs
      Set<String> conceptURIs = extractConceptURIs(model, url);

      // Filter concepts for language and country schemes
      if (LANGUAGE_SCHEME.equals(url)) {
        conceptURIs.retainAll(SUPPORTED_LANGUAGE_CONCEPTS);
      } else if (COUNTRY_SCHEME.equals(url)) {
        conceptURIs.retainAll(SUPPORTED_COUNTRY_CONCEPTS);
      }

      if (conceptURIs.isEmpty()) {
        return Future.failedFuture("No concepts found in scheme: " + url);
      }

      // For each concept, fetch its labels
      List<Future<JsonObject>> concepts = conceptURIs.stream()
        .map(uri -> buildItemFromConcept(uri, multilingual))
        .toList();

      return Future.all(concepts).map(cf -> {
        JsonArray items = new JsonArray();
        for (int i = 0; i < concepts.size(); i++) {
          items.add(cf.resultAt(i));
        }
        return new JsonObject().put(SOURCE, url).put(ITEMS, items);
      });
    });
  }

  /**
   * Fetches an RDF from the given URL and parses it into a Jena Model.
   *
   * @param url The RDF URL.
   * @return A Future with the RDF model.
   */
  private Future<Model> fetchRDFModel(String url) {
    Promise<Model> promise = Promise.promise();

    client.getAbs(url).send(ar -> {
      if (ar.failed()) {
        promise.fail(ar.cause());
        return;
      }
      HttpResponse<Buffer> resp = ar.result();
      if (resp.statusCode() != 200) {
        promise.fail("Failed to fetch RDF (" + resp.statusCode() + "): " + url);
        return;
      }
      Buffer body = resp.body();
      try (ByteArrayInputStream in = new ByteArrayInputStream(body.getBytes())) {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, in, Lang.RDFXML);
        promise.complete(model);
      } catch (Exception e) {
        // If parse fails, log body head for debugging.
        String head = safeHead(body);
        log.error("RDF parse error for {}. Body head: {}", url, head, e);
        promise.fail(e);
      }
    });

    return promise.future();
  }

  /**
   * Extracts concept URIs from the concept scheme model.
   *
   * @param model     The RDF model of the concept scheme.
   * @param schemeURI The URI of the concept scheme.
   * @return A Set of concept URIs.
   */
  private Set<String> extractConceptURIs(Model model, String schemeURI) {
    Set<String> uris = new LinkedHashSet<>();

    // Preferred: skos:hasTopConcept <concept>
    Resource scheme = model.getResource(schemeURI);
    StmtIterator topIt = model.listStatements(scheme, SKOS.hasTopConcept, (RDFNode) null);
    while (topIt.hasNext()) {
      Statement st = topIt.next();
      if (st.getObject().isResource()) {
        uris.add(st.getObject().asResource().getURI());
      }
    }

    if (!uris.isEmpty()) {
      return uris;
    }

    // Fallback: skos:inScheme <scheme> and rdf:type skos:Concept
    ResIterator concepts = model.listResourcesWithProperty(SKOS.inScheme, scheme);
    while (concepts.hasNext()) {
      Resource concept = concepts.next();
      if (concept.hasProperty(RDF.type, SKOS.Concept) || concept.getURI() != null) {
        uris.add(concept.getURI());
      }
    }
    return uris;
  }

  /**
   * Fetches the RDF of a concept and builds its item object with labels per supported language.
   * Builds items as:
   * <pre>{@code
   * {
   *  "value": "<conceptURI>",
   *  "label": { "en": "<English label>", "fr": "<French label>", ... }
   * }
   * }</pre>
   *
   * @param conceptURI The concept's URI.
   * @return A Future with the item.
   */
  private Future<JsonObject> buildItemFromConcept(String conceptURI, boolean multilingual) {
    return fetchRDFModel(conceptURI).map(model -> {
      Resource concept = model.getResource(conceptURI);

      if (!multilingual) {
        String label = findEnglishPrefLabel(concept);
        if (label == null) {
          // Fallback: use first available prefLabel (any lang) or the URI
          label = findAnyPrefLabel(concept);
          if (label == null) {
            label = conceptURI;
          }
        }
        return new JsonObject().put(VALUE, conceptURI).put(LABEL, label);
      }

      Map<String, String> labels = new LinkedHashMap<>();
      StmtIterator it = model.listStatements(concept, SKOS.prefLabel, (RDFNode) null);
      while (it.hasNext()) {
        Statement st = it.next();
        RDFNode obj = st.getObject();
        if (obj.isLiteral()) {
          Literal lit = obj.asLiteral();
          String lang = Optional.ofNullable(lit.getLanguage()).orElse("").trim();
          // Only keep supported languages
          if (!lang.isEmpty() && SUPPORTED_LANGUAGES.contains(lang)) {
            labels.put(lang, lit.getString());
          }
        }
      }

      JsonObject label = new JsonObject();
      labels.forEach(label::put);

      return new JsonObject().put(VALUE, conceptURI).put(LABEL, label);
    });
  }

  /**
   * Finds the English prefLabel of a concept, if any.
   *
   * @param concept The concept resource.
   * @return The English prefLabel, or null if there isn't one.
   */
  private String findEnglishPrefLabel(Resource concept) {
    StmtIterator it = concept.getModel().listStatements(concept, SKOS.prefLabel, (RDFNode) null);
    while (it.hasNext()) {
      Statement st = it.next();
      RDFNode obj = st.getObject();
      if (obj.isLiteral()) {
        Literal lit = obj.asLiteral();
        String lang = Optional.ofNullable(lit.getLanguage()).orElse("").trim();
        if ("en".equalsIgnoreCase(lang)) {
          return lit.getString();
        }
      }
    }
    return null;
  }

  /**
   * Finds the first available prefLabel of a concept, regardless of language.
   *
   * @param concept The concept resource.
   * @return The first found prefLabel, or null if there isn't one.
   */
  private String findAnyPrefLabel(Resource concept) {
    StmtIterator it = concept.getModel().listStatements(concept, SKOS.prefLabel, (RDFNode) null);
    while (it.hasNext()) {
      Statement st = it.next();
      RDFNode obj = st.getObject();
      if (obj.isLiteral()) {
        return obj.asLiteral().getString();
      }
    }
    return null;
  }

  /**
   * Safely reads and truncates the start of a buffer to a UTF-8 string for logging purposes.
   *
   * @param body The buffer to read.
   * @return The first 400 characters of the buffer as a single-line string, or "<unreadable>" on
   * error.
   */
  private String safeHead(Buffer body) {
    try {
      String str = body.toString(StandardCharsets.UTF_8);
      return str.substring(0, Math.min(str.length(), 400)).replaceAll("\\s+", " ");
    } catch (Exception e) {
      return "<unreadable>";
    }
  }

  /**
   * Processes the translation callback by merging the translated values back into the original
   * vocabulary items and updating the MongoDB document.
   *
   * @param source The source URL of the vocabulary.
   * @param dict   The translation dictionary received from the callback.
   */
  public void processTranslationCallback(@NotNull String source, @NotNull JsonObject dict) {
    JsonObject translatedItems = dict.getJsonObject(source);
    if (translatedItems == null || translatedItems.isEmpty()) {
      return;
    }

    JsonObject query = new JsonObject().put(SOURCE, source);
    mongo.findOne(collection, query, null)
      .compose(doc -> {
        if (doc == null) {
          log.warn("Received translation callback for unknown vocabulary '{}'", source);
          return Future.failedFuture("Config not found: " + source);
        }

        boolean updated = false; // Flag to track if any changes were made
        JsonArray items = doc.getJsonArray(ITEMS, new JsonArray());
        for (int i = 0; i < items.size(); i++) {
          JsonObject item = items.getJsonObject(i);
          String value = item.getString(VALUE);
          if (StringUtils.isBlank(value) || !(item.getValue(LABEL) instanceof JsonObject label)
            || label.isEmpty()) {
            continue;
          }

          JsonObject translations = translatedItems
            .getJsonObject(value, new JsonObject())
            .getJsonObject("translations", new JsonObject());
          translations.fieldNames().removeAll(label.fieldNames()); // Skip existing languages

          if (TranslationUtils.addTranslations(label, translations)) {
            updated = true; // Mark that changes were made
          }
        }

        if (!updated) {
          return Future.succeededFuture(doc);
        }

        // Update the document in MongoDB
        JsonObject update = new JsonObject().put("$set", new JsonObject().put(ITEMS, items));
        return mongo.updateCollection(collection, query, update).map(doc);
      })
      .onFailure(err -> log.error("Failed to process translation callback for {}", source, err));
  }
}
