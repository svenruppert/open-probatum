/*
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the EUPL, Version 1.2 (the "Licence");
 * you may not use this file except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package junit.com.svenruppert.openprobatum.catalog;

import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingAuthoringService;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.content.InMemoryContentAuthorship;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OfferingAuthoringService — offering CRUD + multi-module assembly (P004)")
class OfferingAuthoringServiceTest {

  private static final Long AUTHOR = 7L;

  private InMemoryCatalogRepository catalog;
  private InMemoryContentAuthorship authorship;
  private OfferingAuthoringService service;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    authorship = new InMemoryContentAuthorship();
    service = new OfferingAuthoringService(catalog, authorship,
        new InMemoryCredentialRepository());
  }

  private static List<Module> twoModules() {
    return List.of(Module.mandatory("Intro", "start here"),
        Module.optional("Extra", "optional reading"));
  }

  private Offering draft() {
    return service.createDraft("Course", "desc", OfferingVisibility.PUBLIC, null, null,
        twoModules(), AUTHOR);
  }

  @Test
  @DisplayName("createDraft assembles the ordered multi-module path, records authorship, no auto-submit")
  void createAssemblesOrderedPath() {
    Offering o = draft();

    assertEquals(ContentStatus.DRAFT, o.status(), "created as DRAFT, not submitted");
    assertEquals(List.of("Intro", "Extra"),
        o.path().modules().stream().map(Module::title).toList(), "module order preserved");
    assertTrue(o.path().modules().get(0).mandatory());
    assertFalse(o.path().modules().get(1).mandatory());
    assertTrue(catalog.findById(o.id()).isPresent(), "persisted");
    assertTrue(authorship.isAuthor(o.lineageId(), AUTHOR), "authorship recorded");
  }

  @Test
  @DisplayName("createDraft honours visibility — a CODE offering carries its access code")
  void createCodeOffering() {
    Offering o = service.createDraft("Gated", "d", OfferingVisibility.CODE, "S3CRET", null,
        twoModules(), AUTHOR);
    assertEquals(OfferingVisibility.CODE, o.visibility());
    assertEquals("S3CRET", o.accessCode());
  }

  @Test
  @DisplayName("editing a DRAFT updates it in place (same id, same version)")
  void editDraftInPlace() {
    Offering d = draft();
    Offering edited = service.saveEdit(d, "Course v2", "newdesc", OfferingVisibility.REGISTERED,
        null, null, List.of(Module.mandatory("Only", "one")));

    assertEquals(d.id(), edited.id(), "DRAFT edited in place");
    assertEquals(d.version(), edited.version());
    assertEquals(ContentStatus.DRAFT, edited.status());
    assertEquals("Course v2", edited.title());
    assertEquals(1, edited.path().modules().size());
    assertEquals(OfferingVisibility.REGISTERED, edited.visibility());
  }

  @Test
  @DisplayName("editing a PUBLISHED offering branches a fresh DRAFT version (same lineage)")
  void editPublishedBranchesNewVersion() {
    Offering published = draft().withStatus(ContentStatus.PUBLISHED);
    catalog.save(published);

    Offering edited = service.saveEdit(published, "Revised", "d", OfferingVisibility.PUBLIC,
        null, null, twoModules());

    assertNotEquals(published.id(), edited.id(), "a published record is never rewritten");
    assertEquals(published.lineageId(), edited.lineageId(), "same logical offering");
    assertEquals(published.version() + 1, edited.version());
    assertEquals(ContentStatus.DRAFT, edited.status());
    assertTrue(catalog.findById(published.id()).orElseThrow().isPublished(),
        "the published version stays intact");
  }

  @Test
  @DisplayName("deactivate moves a published offering to DEPRECATED")
  void deactivateDeprecates() {
    Offering published = draft().withStatus(ContentStatus.PUBLISHED);
    catalog.save(published);

    service.deactivate(published.id());
    assertEquals(ContentStatus.DEPRECATED,
        catalog.findById(published.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("delete removes a clean DRAFT but refuses a PUBLISHED offering")
  void deleteGuarded() {
    Offering d = draft();
    var ok = service.delete(d.id());
    assertTrue(ok.allowed());
    assertTrue(catalog.findById(d.id()).isEmpty(), "DRAFT removed");

    Offering published = draft().withStatus(ContentStatus.PUBLISHED);
    catalog.save(published);
    var blocked = service.delete(published.id());
    assertFalse(blocked.allowed());
    assertTrue(catalog.findById(published.id()).isPresent(), "published kept");
  }

  @Test
  @DisplayName("submitForReview moves a DRAFT to IN_REVIEW")
  void submitForReview() {
    Offering d = draft();
    service.submitForReview(d.id());
    assertEquals(ContentStatus.IN_REVIEW, catalog.findById(d.id()).orElseThrow().status());
  }

  @Test
  @DisplayName("myOfferings returns the author's own offerings, latest version per lineage")
  void myOfferingsLatestPerLineage() {
    Offering mine = draft();
    // a second version of the same lineage (published → edit branches v2)
    Offering published = mine.withStatus(ContentStatus.PUBLISHED);
    catalog.save(published);
    Offering v2 = service.saveEdit(published, "Course", "d", OfferingVisibility.PUBLIC,
        null, null, twoModules());
    // someone else's offering must not appear
    service.createDraft("Other", "d", OfferingVisibility.PUBLIC, null, null, twoModules(), 9L);

    List<Offering> mineList = service.myOfferings(AUTHOR);
    assertEquals(1, mineList.size(), "latest-per-lineage, own only");
    assertEquals(v2.version(), mineList.get(0).version(), "the newest version wins");
    assertEquals(mine.lineageId(), mineList.get(0).lineageId());
  }

  @Test
  @DisplayName("delete of an unknown id is refused, not a crash")
  void deleteUnknown() {
    var verdict = service.delete(UUID.randomUUID());
    assertFalse(verdict.allowed());
  }
}
