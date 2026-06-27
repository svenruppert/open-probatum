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

package com.svenruppert.openprobatum.views.analytics;

import com.svenruppert.openprobatum.assessment.Question;
import com.svenruppert.openprobatum.assessment.QuestionRepositoryProvider;
import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.coaching.CoachingOffer;
import com.svenruppert.openprobatum.coaching.CoachingOfferRepositoryProvider;
import com.svenruppert.openprobatum.coaching.CoachingSlotRepositoryProvider;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.credential.Credential;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.EffectiveStatus;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.lab.Lab;
import com.svenruppert.openprobatum.lab.LabRepositoryProvider;
import com.svenruppert.openprobatum.lab.LabSubmissionRepositoryProvider;
import com.svenruppert.openprobatum.security.AppClock;
import com.svenruppert.openprobatum.security.model.UserDirectoryProvider;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Academy-wide operator analytics (concept §20.x): a read-only aggregation over the
 * existing repositories — no new tracking. It gives an operator the platform-level
 * picture the per-content {@code MetricsView} does not: the credential mix, the
 * editorial content pipeline, and engagement totals. All figures are zero-safe
 * (an empty academy yields empty maps / zero counts). Repositories are resolved via
 * their {@code *Provider}s — the established injection seam for tests.
 *
 * @since V00.70.00
 */
public final class OperatorAnalyticsService {

  /**
   * The issued-credential mix.
   *
   * @param total      credentials issued
   * @param byStatus   count by effective status (VALID / EXPIRED / REVOKED / …)
   * @param byEvidence count by issuance evidence type
   */
  public record CredentialStats(int total, Map<EffectiveStatus, Long> byStatus,
                                Map<Evidence.Type, Long> byEvidence) {
  }

  /**
   * The editorial pipeline of one content type.
   *
   * @param type     the content type label
   * @param total    items of this type (all versions)
   * @param byStatus count by {@link ContentStatus}
   */
  public record ContentPipeline(String type, int total, Map<ContentStatus, Long> byStatus) {
  }

  /**
   * Engagement totals.
   *
   * @param registeredUsers   registered users in the directory
   * @param labSubmissions    practical lab submissions made
   * @param workshopEnrolments workshop seats taken (all states)
   * @param coachingBookings  coaching slots booked or completed
   */
  public record EngagementStats(long registeredUsers, int labSubmissions,
                                int workshopEnrolments, int coachingBookings) {
  }

  /** The issued-credential mix by effective status + evidence type. */
  public CredentialStats credentialStats() {
    List<Credential> all = List.copyOf(CredentialRepositoryProvider.repository().all());
    Instant now = AppClock.now();
    Map<EffectiveStatus, Long> byStatus = all.stream()
        .collect(Collectors.groupingBy(c -> c.effectiveStatusAt(now), Collectors.counting()));
    Map<Evidence.Type, Long> byEvidence = all.stream()
        .collect(Collectors.groupingBy(c -> c.evidence().type(), Collectors.counting()));
    return new CredentialStats(all.size(), byStatus, byEvidence);
  }

  /** The editorial pipeline (count by {@link ContentStatus}) for every content type. */
  public List<ContentPipeline> contentPipelines() {
    return List.of(
        pipeline("Questions", QuestionRepositoryProvider.repository().all().stream()
            .map(Question::status)),
        pipeline("Offerings", CatalogRepositoryProvider.repository().all().stream()
            .map(Offering::status)),
        pipeline("Labs", LabRepositoryProvider.repository().all().stream()
            .map(Lab::status)),
        pipeline("Bundles", BundleRepositoryProvider.repository().all().stream()
            .map(Bundle::status)),
        pipeline("Workshops", WorkshopRepositoryProvider.repository().all().stream()
            .map(Workshop::status)),
        pipeline("Coaching", CoachingOfferRepositoryProvider.repository().all().stream()
            .map(CoachingOffer::status)));
  }

  private ContentPipeline pipeline(String type, Stream<ContentStatus> statuses) {
    List<ContentStatus> list = statuses.toList();
    Map<ContentStatus, Long> byStatus = list.stream()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    return new ContentPipeline(type, list.size(), byStatus);
  }

  /** Registered users + submission / enrolment / booking totals. */
  public EngagementStats engagement() {
    long users = UserDirectoryProvider.directory().all().count();
    int submissions = LabSubmissionRepositoryProvider.repository().all().size();
    int enrolments = WorkshopEnrolmentRepositoryProvider.repository().all().size();
    int bookings = (int) CoachingSlotRepositoryProvider.repository().all().stream()
        .filter(s -> s.isBooked() || s.isCompleted())
        .count();
    return new EngagementStats(users, submissions, enrolments, bookings);
  }
}
