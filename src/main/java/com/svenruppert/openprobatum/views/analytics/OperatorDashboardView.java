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

import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.MainLayout;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The academy-wide operator dashboard (concept §20.x): a read-only,
 * platform-level view for operators (PlatformAdmin) that aggregates the live
 * repositories — the credential mix, the editorial content pipeline, and
 * engagement totals. Where {@code MetricsView} shows per-content quality,
 * this shows the whole academy at a glance. Gated to {@code analytics:read}.
 *
 * @since V00.70.00
 */
@Route(value = OperatorDashboardView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.PLATFORM_ADMIN})
public class OperatorDashboardView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "operator";

  public OperatorDashboardView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("operator.heading", "Operator dashboard"),
        tr("operator.subtitle", "Academy-wide credentials, content pipeline and engagement.")));

    OperatorAnalyticsService analytics = new OperatorAnalyticsService();

    renderCredentials(root, analytics.credentialStats());
    renderPipelines(root, analytics);
    renderEngagement(root, analytics.engagement());
  }

  private void renderCredentials(VerticalLayout root,
                                 OperatorAnalyticsService.CredentialStats stats) {
    root.add(new H3(tr("operator.section.credentials", "Credentials")));
    Div box = new Div();
    box.getElement().setAttribute("data-cred-total", String.valueOf(stats.total()));
    stats.byStatus().forEach((status, count) ->
        box.add(chip("data-cred-status", status.name(), status.name() + ": " + count)));
    stats.byEvidence().forEach((type, count) ->
        box.add(chip("data-cred-evidence", type.name(), type.name() + ": " + count)));
    root.add(box);
  }

  private void renderPipelines(VerticalLayout root, OperatorAnalyticsService analytics) {
    root.add(new H3(tr("operator.section.pipeline", "Content pipeline")));
    analytics.contentPipelines().forEach(p -> {
      Div card = new Div();
      card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
      card.getStyle().set("padding", "var(--lumo-space-s)")
          .set("margin-bottom", "var(--lumo-space-s)");
      card.getElement().setAttribute("data-pipeline", p.type());
      card.getElement().setAttribute("data-pipeline-total", String.valueOf(p.total()));
      card.add(new H4(tr("operator.pipeline." + p.type().toLowerCase(java.util.Locale.ROOT),
          p.type())));
      p.byStatus().forEach((status, count) ->
          card.add(chip("data-pipeline-status", status.name(), status.name() + ": " + count)));
      root.add(card);
    });
  }

  private void renderEngagement(VerticalLayout root,
                                OperatorAnalyticsService.EngagementStats stats) {
    root.add(new H3(tr("operator.section.engagement", "Engagement")));
    Div box = new Div();
    box.getElement().setAttribute("data-engagement-users", String.valueOf(stats.registeredUsers()));
    box.getElement().setAttribute("data-engagement-submissions",
        String.valueOf(stats.labSubmissions()));
    box.getElement().setAttribute("data-engagement-enrolments",
        String.valueOf(stats.workshopEnrolments()));
    box.getElement().setAttribute("data-engagement-bookings",
        String.valueOf(stats.coachingBookings()));
    box.add(new Span(tr("operator.engagement.line",
        "{0} learners · {1} lab submissions · {2} workshop enrolments · {3} coaching bookings",
        stats.registeredUsers(), stats.labSubmissions(),
        stats.workshopEnrolments(), stats.coachingBookings())));
    root.add(box);
  }

  private Span chip(String attr, String value, String label) {
    Span chip = new Span(label);
    chip.getElement().setAttribute(attr, value);
    chip.getElement().getThemeList().add("badge pill contrast");
    chip.getStyle().set("margin-right", "var(--lumo-space-xs)");
    return chip;
  }
}
