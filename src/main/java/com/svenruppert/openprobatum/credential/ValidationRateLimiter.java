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

package com.svenruppert.openprobatum.credential;

import com.svenruppert.openprobatum.security.AppTenant;
import com.svenruppert.jsentinel.authorization.api.JSentinelServiceResolver;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitPolicy;
import com.svenruppert.jsentinel.ratelimiting.InMemoryRateLimitStore;
import com.svenruppert.jsentinel.ratelimiting.RateLimitDecision;
import com.svenruppert.jsentinel.ratelimiting.RateLimitKey;
import com.svenruppert.jsentinel.ratelimiting.RateLimitPolicy;

import java.time.Duration;
import java.util.Objects;

/**
 * Per-client-IP rate limit on the public validation lookup (concept §11.5) —
 * the second defence line, alongside the random non-enumerable id (§10.4),
 * against enumeration and abuse of the publicly-reachable page.
 *
 * <p>Wraps the jSentinel {@code ratelimiting} API: an in-memory
 * {@link RateLimitPolicy} that records each lookup keyed by
 * {@code (AppTenant.ID, "validate:<ip>")} and emits a {@code RateLimitExceeded}
 * audit event when the window limit is passed.
 *
 * @since V00.10.00
 */
public final class ValidationRateLimiter {

  /** Default: at most this many lookups per IP per window. */
  public static final int DEFAULT_LIMIT = 30;
  /** Default sliding window. */
  public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

  private final RateLimitPolicy policy;

  public ValidationRateLimiter(RateLimitPolicy policy) {
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  /** The production limiter: in-memory store + the security audit sink. */
  public static ValidationRateLimiter create() {
    return new ValidationRateLimiter(new InMemoryRateLimitPolicy(
        new InMemoryRateLimitStore(),
        JSentinelServiceResolver.securityAuditService(),
        DEFAULT_LIMIT, DEFAULT_WINDOW));
  }

  /**
   * Whether a lookup from {@code clientIp} is allowed; {@code false} when the
   * IP has exceeded the window limit (the policy audits the breach).
   */
  public boolean allow(String clientIp) {
    String scope = "validate:" + (clientIp == null || clientIp.isBlank() ? "anon" : clientIp);
    RateLimitDecision decision = policy.tryAcquire(new RateLimitKey(AppTenant.ID, scope));
    return decision instanceof RateLimitDecision.Allowed;
  }
}
