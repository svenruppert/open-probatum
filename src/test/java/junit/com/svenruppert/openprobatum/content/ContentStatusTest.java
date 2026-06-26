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

package junit.com.svenruppert.openprobatum.content;

import com.svenruppert.openprobatum.content.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ContentStatus — editorial lifecycle transitions (P001)")
class ContentStatusTest {

  @Test
  @DisplayName("the happy path Draft → In Review → Approved → Published is allowed")
  void happyPath() {
    assertTrue(ContentStatus.DRAFT.canTransitionTo(ContentStatus.IN_REVIEW));
    assertTrue(ContentStatus.IN_REVIEW.canTransitionTo(ContentStatus.APPROVED));
    assertTrue(ContentStatus.APPROVED.canTransitionTo(ContentStatus.PUBLISHED));
    assertTrue(ContentStatus.PUBLISHED.canTransitionTo(ContentStatus.DEPRECATED));
    assertTrue(ContentStatus.DEPRECATED.canTransitionTo(ContentStatus.ARCHIVED));
  }

  @Test
  @DisplayName("review can reject back to Draft; published can be replaced")
  void rejectionAndReplacement() {
    assertTrue(ContentStatus.IN_REVIEW.canTransitionTo(ContentStatus.DRAFT));
    assertTrue(ContentStatus.PUBLISHED.canTransitionTo(ContentStatus.REPLACED));
  }

  @Test
  @DisplayName("illegal jumps are rejected")
  void illegalTransitions() {
    assertFalse(ContentStatus.DRAFT.canTransitionTo(ContentStatus.PUBLISHED), "no skipping review");
    assertFalse(ContentStatus.PUBLISHED.canTransitionTo(ContentStatus.DRAFT), "published cannot go back to draft");
    assertFalse(ContentStatus.APPROVED.canTransitionTo(ContentStatus.ARCHIVED), "approved is not archivable directly");
    assertFalse(ContentStatus.DRAFT.canTransitionTo(null));
  }

  @Test
  @DisplayName("ARCHIVED and REPLACED are terminal; only PUBLISHED is learner-visible")
  void terminalAndPublished() {
    assertTrue(ContentStatus.ARCHIVED.isTerminal());
    assertTrue(ContentStatus.REPLACED.isTerminal());
    assertFalse(ContentStatus.PUBLISHED.isTerminal());

    assertTrue(ContentStatus.PUBLISHED.isPublished());
    assertFalse(ContentStatus.DRAFT.isPublished());
    assertFalse(ContentStatus.DEPRECATED.isPublished());
  }
}
