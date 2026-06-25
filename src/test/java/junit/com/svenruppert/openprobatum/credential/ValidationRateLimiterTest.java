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

import com.svenruppert.openprobatum.credential.ValidationRateLimiter;
import com.svenruppert.jsentinel.audit.AuditEvent;
import com.svenruppert.jsentinel.audit.AuditQuery;
import com.svenruppert.jsentinel.audit.JSentinelAuditService;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitPolicy;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ValidationRateLimiter — per-IP throttle + audit (P010)")
class ValidationRateLimiterTest {

  private RecordingAudit audit;
  private ValidationRateLimiter limiter;

  @BeforeEach
  void setUp() {
    audit = new RecordingAudit();
    limiter = new ValidationRateLimiter(new InMemoryRateLimitPolicy(
        new InMemoryRateLimitStore(), audit, 3, Duration.ofMinutes(1)));
  }

  @Test
  @DisplayName("allows up to the limit, then throttles the same IP")
  void allowsThenThrottles() {
    assertTrue(limiter.allow("1.2.3.4"));
    assertTrue(limiter.allow("1.2.3.4"));
    assertTrue(limiter.allow("1.2.3.4"));
    assertFalse(limiter.allow("1.2.3.4"), "the 4th lookup from the same IP is throttled");
  }

  @Test
  @DisplayName("each IP has its own bucket")
  void perIpBuckets() {
    for (int i = 0; i < 3; i++) {
      limiter.allow("1.1.1.1");
    }
    assertFalse(limiter.allow("1.1.1.1"));
    assertTrue(limiter.allow("2.2.2.2"), "a different IP is unaffected");
  }

  @Test
  @DisplayName("a throttle breach is audited")
  void throttleEmitsAudit() {
    for (int i = 0; i < 4; i++) {
      limiter.allow("9.9.9.9");
    }
    assertTrue(audit.events.stream().anyMatch(
            e -> e.getClass().getSimpleName().toLowerCase().contains("ratelimit")),
        "exceeding the limit must publish a rate-limit audit event");
  }

  private static final class RecordingAudit implements JSentinelAuditService {
    final List<AuditEvent> events = new ArrayList<>();
    @Override public void publish(AuditEvent e) { events.add(e); }
    @Override public List<AuditEvent> query(AuditQuery q) { return List.copyOf(events); }
  }
}
