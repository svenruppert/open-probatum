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

package junit.com.svenruppert.openprobatum.access;

import com.svenruppert.openprobatum.access.AccessDecision;
import com.svenruppert.openprobatum.access.EntitlementReason;
import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.access.InMemoryEntitlementRepository;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntitlementService — access decisions per visibility (P004)")
class EntitlementServiceTest {

  private final AppUser learner = new AppUser(1001L, "Learner", EnumSet.of(AuthorizationRole.LEARNER));
  private EntitlementService service;

  @BeforeEach
  void setUp() {
    service = new EntitlementService(new InMemoryEntitlementRepository());
  }

  private static LearningPath path() {
    return new LearningPath("P", List.of(Module.mandatory("M", "c")));
  }

  @Test
  @DisplayName("PUBLIC is granted to everyone, including anonymous")
  void publicIsOpen() {
    Offering o = Offering.publicPath("Open", "d", path());
    assertEquals(AccessDecision.GRANTED, service.canAccess(learner, o));
    assertEquals(AccessDecision.GRANTED, service.canAccess(null, o));
  }

  @Test
  @DisplayName("REGISTERED requires a logged-in user")
  void registeredRequiresLogin() {
    Offering o = Offering.registeredPath("Members", "d", path());
    assertEquals(AccessDecision.GRANTED, service.canAccess(learner, o));
    assertEquals(AccessDecision.LOGIN_REQUIRED, service.canAccess(null, o));
  }

  @Test
  @DisplayName("a CODE offering is reachable only after redeeming the correct code")
  void codeGate() {
    Offering o = Offering.codePath("Gated", "d", path(), "OPEN-SESAME");

    assertEquals(AccessDecision.CODE_REQUIRED, service.canAccess(learner, o));
    assertFalse(service.redeemCode(learner, o, "wrong"), "wrong code grants nothing");
    assertEquals(AccessDecision.CODE_REQUIRED, service.canAccess(learner, o));

    assertTrue(service.redeemCode(learner, o, "OPEN-SESAME"), "correct code grants access");
    assertEquals(AccessDecision.GRANTED, service.canAccess(learner, o));
  }

  @Test
  @DisplayName("redeemCode rejects an anonymous user and a non-CODE offering")
  void redeemCodeGuards() {
    Offering coded = Offering.codePath("Gated", "d", path(), "X");
    assertFalse(service.redeemCode(null, coded, "X"));

    Offering open = Offering.publicPath("Open", "d", path());
    assertFalse(service.redeemCode(learner, open, "X"));
  }

  @Test
  @DisplayName("a PREREQUISITE offering needs a grant; manual/prerequisite grant opens it")
  void prerequisiteGate() {
    Offering o = Offering.prerequisitePath("Advanced", "d", path(), UUID.randomUUID());

    assertEquals(AccessDecision.PREREQUISITE_REQUIRED, service.canAccess(learner, o));
    service.grant(learner, o, EntitlementReason.PREREQUISITE);
    assertEquals(AccessDecision.GRANTED, service.canAccess(learner, o));
  }

  @Test
  @DisplayName("a grant for one learner does not leak to another")
  void grantsArePerUser() {
    Offering o = Offering.codePath("Gated", "d", path(), "C");
    service.redeemCode(learner, o, "C");

    AppUser other = new AppUser(1002L, "Other", EnumSet.of(AuthorizationRole.LEARNER));
    assertEquals(AccessDecision.CODE_REQUIRED, service.canAccess(other, o));
  }
}
