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

import com.svenruppert.openprobatum.catalog.CatalogIntegrityService;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CatalogIntegrityService — destructive-operation guard (P006)")
class CatalogIntegrityServiceTest {

  private InMemoryCatalogRepository catalog;
  private InMemoryCredentialRepository credentials;
  private java.util.Set<java.util.UUID> bundleMembers;
  private CatalogIntegrityService service;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    credentials = new InMemoryCredentialRepository();
    // A simple in-test bundle-membership set stands in for the BundleRepository scan.
    bundleMembers = new java.util.HashSet<>();
    service = new CatalogIntegrityService(catalog, credentials, bundleMembers::contains);
  }

  private static Offering draftOffering(String title) {
    LearningPath path = new LearningPath(title, List.of(Module.mandatory("M1", "content")));
    return Offering.publicPath(title, "desc", path);
  }

  @Test
  @DisplayName("a DRAFT offering with no references may be hard-deleted")
  void draftWithoutReferencesDeletable() {
    Offering draft = draftOffering("Intro");
    catalog.save(draft);

    var verdict = service.checkHardDelete(draft);
    assertTrue(verdict.allowed());
    assertTrue(verdict.blockers().isEmpty());
    assertTrue(service.mayHardDelete(draft));
  }

  @Test
  @DisplayName("a PUBLISHED offering may not be hard-deleted — deactivate instead")
  void publishedNotDeletable() {
    Offering published = draftOffering("Live").withStatus(ContentStatus.PUBLISHED);
    catalog.save(published);

    var verdict = service.checkHardDelete(published);
    assertFalse(verdict.allowed());
    assertEquals(1, verdict.blockers().size());
    assertTrue(verdict.blockers().get(0).contains("PUBLISHED"));
    assertTrue(service.canDeactivate(published), "deactivate stays available");
  }

  @Test
  @DisplayName("a DRAFT another offering lists as a prerequisite may not be deleted")
  void prerequisiteBlocksDelete() {
    Offering target = draftOffering("Foundations");
    catalog.save(target);
    LearningPath path = new LearningPath("Advanced", List.of(Module.mandatory("M1", "c")));
    Offering dependent = Offering.prerequisitePath("Advanced", "desc", path, target.id());
    catalog.save(dependent);

    var verdict = service.checkHardDelete(target);
    assertFalse(verdict.allowed());
    assertTrue(verdict.blockers().stream().anyMatch(b -> b.contains("prerequisite")));
  }

  @Test
  @DisplayName("an offering an issued credential references may not be deleted")
  void credentialBlocksDelete() {
    Offering draft = draftOffering("Certified");
    catalog.save(draft);
    credentials.save(Credential.issue("C", CredentialType.COMPLETION_CERTIFICATE, 1L, "Ada",
        "Academy", Instant.parse("2026-01-01T00:00:00Z"), null,
        Evidence.pathCompleted(draft.id(), 1)));

    var verdict = service.checkHardDelete(draft);
    assertFalse(verdict.allowed());
    assertTrue(verdict.blockers().stream().anyMatch(b -> b.contains("credential")));
  }

  @Test
  @DisplayName("a DRAFT that a bundle lists as a member may not be deleted — it would strand the bundle (P012)")
  void bundleMembershipBlocksDelete() {
    Offering member = draftOffering("Module A");
    catalog.save(member);
    bundleMembers.add(member.id()); // a published bundle references this draft

    var verdict = service.checkHardDelete(member);
    assertFalse(verdict.allowed());
    assertTrue(verdict.blockers().stream().anyMatch(b -> b.contains("bundle")),
        "the bundle reference must block the delete: " + verdict.blockers());
  }

  @Test
  @DisplayName("deactivation is always permitted by integrity")
  void deactivateAlwaysAllowed() {
    assertTrue(service.canDeactivate(draftOffering("Any")));
    assertTrue(service.canDeactivate(draftOffering("Any").withStatus(ContentStatus.PUBLISHED)));
  }
}
