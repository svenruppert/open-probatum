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

package junit.com.svenruppert.openprobatum.bundle;

import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleCompletionService;
import com.svenruppert.openprobatum.catalog.InMemoryCatalogRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialEventRepositoryProvider;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.credential.InMemoryCredentialEventRepository;
import com.svenruppert.openprobatum.credential.InMemoryCredentialRepository;
import com.svenruppert.openprobatum.credential.IssuanceService;
import com.svenruppert.openprobatum.credential.IssuerIdentity;
import com.svenruppert.openprobatum.progress.InMemoryProgressRepository;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BundleCompletionService — completion + atomic claim mint-once (P005)")
class BundleCompletionServiceTest {

  private static final AppUser ADA =
      new AppUser(1001L, "Ada", EnumSet.of(AuthorizationRole.LEARNER));

  private InMemoryCatalogRepository catalog;
  private InMemoryProgressRepository progressRepo;
  private ProgressService progress;
  private InMemoryCredentialRepository credentials;
  private BundleCompletionService service;
  private Offering a;
  private Offering b;
  private Bundle bundle;

  @BeforeEach
  void setUp() {
    catalog = new InMemoryCatalogRepository();
    progressRepo = new InMemoryProgressRepository();
    progress = new ProgressService(progressRepo);
    credentials = new InMemoryCredentialRepository();
    CredentialEventRepositoryProvider.setRepository(new InMemoryCredentialEventRepository());
    IssuanceService issuance = new IssuanceService(credentials,
        new IssuerIdentity("Academy", "http://h/validate"));
    service = new BundleCompletionService(catalog, progress, credentials, issuance);

    a = Offering.publicPath("A", "d", path());
    b = Offering.publicPath("B", "d", path());
    catalog.save(a);
    catalog.save(b);
    bundle = Bundle.draft("Pack", "d", Set.of(a.id(), b.id()));
  }

  @AfterEach
  void tearDown() {
    CredentialEventRepositoryProvider.reset();
  }

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("Core", "c")));
  }

  private void complete(Offering offering) {
    offering.path().mandatoryModules()
        .forEach(m -> progress.markModuleComplete(ADA.id(), offering.id(), m.id()));
  }

  @Test
  @DisplayName("a bundle is complete only when every member path is complete")
  void completion() {
    assertFalse(service.isComplete(ADA.id(), bundle), "nothing done");
    complete(a);
    assertFalse(service.isComplete(ADA.id(), bundle), "only one member done");
    complete(b);
    assertTrue(service.isComplete(ADA.id(), bundle), "all members done");
  }

  @Test
  @DisplayName("claiming a complete bundle mints one credential with bundle evidence + recipient id")
  void claimMints() {
    complete(a);
    complete(b);
    Credential c = service.claim(ADA, bundle, "Vaadin Mastery").orElseThrow();
    assertEquals(Evidence.Type.BUNDLE_COMPLETED, c.evidence().type());
    assertEquals(bundle.id(), c.evidence().sourceId());
    assertEquals(bundle.version(), c.sourceVersion());
    assertEquals(1001L, c.recipientId());
    assertEquals(1, credentials.all().size());
  }

  @Test
  @DisplayName("an incomplete bundle cannot be claimed; a second claim is refused")
  void incompleteAndReclaim() {
    assertTrue(service.claim(ADA, bundle, "X").isEmpty(), "incomplete → no claim");
    complete(a);
    complete(b);
    service.claim(ADA, bundle, "X");
    assertTrue(service.claim(ADA, bundle, "X").isEmpty(), "already claimed → refused");
    assertEquals(1, credentials.all().size(), "still exactly one credential");
  }

  @Test
  @DisplayName("concurrent claims of a complete bundle mint exactly one credential (atomic edge)")
  void concurrentClaim() throws InterruptedException {
    complete(a);
    complete(b);
    int threads = 16;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(threads);
    var wins = new AtomicInteger();
    for (int i = 0; i < threads; i++) {
      pool.execute(() -> {
        try {
          start.await();
          if (service.claim(ADA, bundle, "X").isPresent()) {
            wins.incrementAndGet();
          }
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      });
    }
    start.countDown();
    assertTrue(done.await(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    assertEquals(1, wins.get(), "exactly one claim wins");
    assertEquals(1, credentials.all().size(), "exactly one credential minted");
  }
}
