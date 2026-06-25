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

package junit.com.svenruppert.flow.security.bootstrap;

import com.svenruppert.flow.security.bootstrap.BootstrapExtension;
import com.svenruppert.jsentinel.credential.password.bouncycastle.BouncyCastleHashingServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the SPI ordering contract end-to-end: with every registered
 * {@link BootstrapExtension} applied in {@code order()} sequence (the same
 * aggregation {@code BootstrapBuilder} performs), the <em>effective</em>
 * (last-wins) password hashing must be the modern Argon2id service.
 *
 * <p>A future / third-party extension at {@code order() >= 20} that
 * silently downgraded hashing would make a later, non-Argon2id call the
 * last credential config — and fail this test.
 */
@DisplayName("Bootstrap SPI — effective hashing is Argon2id (R11)")
class BootstrapHashingAggregateTest {

  private static List<BootstrapExtension> registeredInOrder() {
    List<BootstrapExtension> extensions = new ArrayList<>();
    ServiceLoader.load(BootstrapExtension.class).forEach(extensions::add);
    extensions.sort(Comparator.comparingInt(BootstrapExtension::order));
    return extensions;
  }

  @Test
  @DisplayName("the last credential config across all registered extensions is the Argon2id hashing")
  void effectiveHashingIsArgon2id() {
    RecordingBootstraps.RecCredentials cred = new RecordingBootstraps.RecCredentials();
    registeredInOrder().forEach(e -> e.contributeCredentials(cred));

    assertFalse(cred.configOrder.isEmpty(),
        "at least one extension must configure hashing");
    String expectedLast = "hashing:" + BouncyCastleHashingServices.modern().getClass().getName();
    assertEquals(expectedLast, cred.configOrder.get(cred.configOrder.size() - 1),
        "the effective (last-wins) hashing must be the modern Argon2id service — "
            + "a later extension downgrading it would fail here");
  }

  @Test
  @DisplayName("the highest-order extension is the one that sets Argon2id (downgrade tripwire)")
  void highestOrderSetsModernHashing() {
    List<BootstrapExtension> ordered = registeredInOrder();
    BootstrapExtension highest = ordered.get(ordered.size() - 1);

    RecordingBootstraps.RecCredentials cred = new RecordingBootstraps.RecCredentials();
    highest.contributeCredentials(cred);

    assertEquals(1, cred.hashingCalls.size(),
        "the highest-order extension is expected to set the effective hashing");
    assertEquals(BouncyCastleHashingServices.modern().getClass(),
        cred.hashingCalls.get(0).getClass(),
        "the highest-order extension must set the modern Argon2id hashing");
  }
}
