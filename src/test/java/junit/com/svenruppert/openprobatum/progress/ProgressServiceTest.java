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

package junit.com.svenruppert.openprobatum.progress;

import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.progress.InMemoryProgressRepository;
import com.svenruppert.openprobatum.progress.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProgressService — module completion + path progress (P008)")
class ProgressServiceTest {

  private static final long ALICE = 1001L;
  private static final long BOB = 1002L;

  private final Module core1 = Module.mandatory("Core 1", "c");
  private final Module core2 = Module.mandatory("Core 2", "c");
  private final Module bonus = Module.optional("Bonus", "c");
  private final Offering offering = Offering.publicPath("Course", "d",
      new LearningPath("P", List.of(core1, core2, bonus)));

  private ProgressService service;

  @BeforeEach
  void setUp() {
    service = new ProgressService(new InMemoryProgressRepository());
  }

  @Test
  @DisplayName("marking modules accrues progress and drives the completion %")
  void marksAccrue() {
    assertEquals(0, service.percentComplete(ALICE, offering));
    assertFalse(service.isPathComplete(ALICE, offering));

    service.markModuleComplete(ALICE, offering.id(), core1.id());
    assertEquals(50, service.percentComplete(ALICE, offering));
    assertFalse(service.isPathComplete(ALICE, offering));

    service.markModuleComplete(ALICE, offering.id(), core2.id());
    assertEquals(100, service.percentComplete(ALICE, offering));
    assertTrue(service.isPathComplete(ALICE, offering));
  }

  @Test
  @DisplayName("an optional module does not advance the mandatory completion %")
  void optionalDoesNotBlockOrAdvance() {
    service.markModuleComplete(ALICE, offering.id(), bonus.id());
    assertEquals(0, service.percentComplete(ALICE, offering));
    assertFalse(service.isPathComplete(ALICE, offering));
    assertTrue(service.completedModules(ALICE, offering.id()).contains(bonus.id()));
  }

  @Test
  @DisplayName("marking the same module twice is idempotent")
  void idempotentMark() {
    service.markModuleComplete(ALICE, offering.id(), core1.id());
    service.markModuleComplete(ALICE, offering.id(), core1.id());
    assertEquals(1, service.completedModules(ALICE, offering.id()).size());
  }

  @Test
  @DisplayName("progress is isolated per learner")
  void perLearnerIsolation() {
    service.markModuleComplete(ALICE, offering.id(), core1.id());
    assertTrue(service.completedModules(BOB, offering.id()).isEmpty());
    assertEquals(0, service.percentComplete(BOB, offering));
  }
}
