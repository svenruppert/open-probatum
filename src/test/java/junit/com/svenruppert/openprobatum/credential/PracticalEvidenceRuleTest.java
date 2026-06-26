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

package junit.com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.credential.CredentialRule;
import com.svenruppert.openprobatum.credential.CredentialType;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.lab.LabSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Practical-lab evidence + credential rule (P007)")
class PracticalEvidenceRuleTest {

  private static LabSubmission submission(UUID labId) {
    return LabSubmission.submit(labId, 3, 1001L, "Ada", "did it", null);
  }

  @Test
  @DisplayName("labVerified evidence carries the lab id + version (PRACTICAL_LAB_VERIFIED)")
  void evidence() {
    UUID labId = UUID.randomUUID();
    Evidence e = Evidence.labVerified(labId, 3);
    assertEquals(Evidence.Type.PRACTICAL_LAB_VERIFIED, e.type());
    assertEquals(labId, e.sourceId());
    assertEquals(3, e.sourceVersion());
  }

  @Test
  @DisplayName("a LAB_VERIFIED rule is satisfied only by a verified submission to its lab")
  void ruleHappyPath() {
    UUID labId = UUID.randomUUID();
    CredentialRule rule = CredentialRule.labVerified(labId, "Practitioner",
        CredentialType.COMPLETION_CERTIFICATE);

    assertFalse(rule.isSatisfiedBy(submission(labId)), "SUBMITTED is not yet evidence");
    assertTrue(rule.isSatisfiedBy(submission(labId).verified("ok")), "verified satisfies it");
    assertFalse(rule.isSatisfiedBy(submission(labId).rejected("no")), "rejected does not");
  }

  @Test
  @DisplayName("a LAB_VERIFIED rule ignores a verified submission to a different lab")
  void wrongLab() {
    CredentialRule rule = CredentialRule.labVerified(UUID.randomUUID(), "X",
        CredentialType.COMPLETION_CERTIFICATE);
    assertFalse(rule.isSatisfiedBy(submission(UUID.randomUUID()).verified("ok")));
  }

  @Test
  @DisplayName("rule kinds do not cross-evaluate (an assessment rule ignores a submission)")
  void kindIsolation() {
    CredentialRule assessmentRule = CredentialRule.assessmentPassed(UUID.randomUUID(), 0.0,
        "X", CredentialType.COMPLETION_CERTIFICATE);
    assertFalse(assessmentRule.isSatisfiedBy(submission(UUID.randomUUID()).verified("ok")));
  }
}
