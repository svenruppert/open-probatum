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

package com.svenruppert.openprobatum.views.metrics;

import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleRepository;
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.credential.CredentialRepository;
import com.svenruppert.openprobatum.credential.CredentialRepositoryProvider;
import com.svenruppert.openprobatum.credential.Evidence;
import com.svenruppert.openprobatum.workshop.EnrolmentStatus;
import com.svenruppert.openprobatum.workshop.Workshop;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolment;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepository;
import com.svenruppert.openprobatum.workshop.WorkshopEnrolmentRepositoryProvider;
import com.svenruppert.openprobatum.workshop.WorkshopRepository;
import com.svenruppert.openprobatum.workshop.WorkshopRepositoryProvider;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Quality metrics for the V00.50.00 packaging shapes (concept §20.2): per-bundle
 * completion counts and per-workshop fill + attendance rates. Kept apart from
 * {@code QualityMetricsService} so neither constructor grows unwieldy.
 *
 * @since V00.50.00
 */
public final class PackagingMetricsService {

  private final BundleRepository bundles;
  private final CredentialRepository credentials;
  private final WorkshopRepository workshops;
  private final WorkshopEnrolmentRepository enrolments;

  public PackagingMetricsService(BundleRepository bundles, CredentialRepository credentials,
                                 WorkshopRepository workshops,
                                 WorkshopEnrolmentRepository enrolments) {
    this.bundles = Objects.requireNonNull(bundles, "bundles");
    this.credentials = Objects.requireNonNull(credentials, "credentials");
    this.workshops = Objects.requireNonNull(workshops, "workshops");
    this.enrolments = Objects.requireNonNull(enrolments, "enrolments");
  }

  public PackagingMetricsService() {
    this(BundleRepositoryProvider.repository(), CredentialRepositoryProvider.repository(),
        WorkshopRepositoryProvider.repository(), WorkshopEnrolmentRepositoryProvider.repository());
  }

  /** How many learners have earned the completion credential of a bundle. */
  public record BundleMetrics(UUID bundleId, String title, int completions) {
  }

  /**
   * Fill + attendance of a workshop.
   *
   * @param enrolled       seats taken (not cancelled)
   * @param attended       how many attended
   * @param capacity       the seat capacity
   * @param fillRate       {@code enrolled / capacity} in {@code [0, 1]}
   * @param attendanceRate {@code attended / enrolled} in {@code [0, 1]} (0 when none enrolled)
   */
  public record WorkshopMetrics(UUID workshopId, String title, int enrolled, int attended,
                                int capacity, double fillRate, double attendanceRate) {
  }

  /** Completion count per bundle, by title. */
  public List<BundleMetrics> allBundleMetrics() {
    return bundles.all().stream()
        .map(b -> new BundleMetrics(b.id(), b.title(), completions(b)))
        .sorted((x, y) -> x.title().compareToIgnoreCase(y.title()))
        .toList();
  }

  private int completions(Bundle bundle) {
    return (int) credentials.all().stream()
        .filter(c -> c.evidence().type() == Evidence.Type.BUNDLE_COMPLETED
            && bundle.id().equals(c.evidence().sourceId()))
        .count();
  }

  /** Fill + attendance per workshop, by title. */
  public List<WorkshopMetrics> allWorkshopMetrics() {
    return workshops.all().stream()
        .map(this::metricsFor)
        .sorted((x, y) -> x.title().compareToIgnoreCase(y.title()))
        .toList();
  }

  private WorkshopMetrics metricsFor(Workshop workshop) {
    List<WorkshopEnrolment> all = enrolments.forWorkshop(workshop.id());
    int enrolled = (int) all.stream().filter(e -> e.status() != EnrolmentStatus.CANCELLED).count();
    int attended = (int) all.stream().filter(WorkshopEnrolment::isAttended).count();
    double fillRate = workshop.capacity() == 0 ? 0.0 : (double) enrolled / workshop.capacity();
    double attendanceRate = enrolled == 0 ? 0.0 : (double) attended / enrolled;
    return new WorkshopMetrics(workshop.id(), workshop.title(), enrolled, attended,
        workshop.capacity(), fillRate, attendanceRate);
  }
}
