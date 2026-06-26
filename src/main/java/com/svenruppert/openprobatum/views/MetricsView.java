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

package com.svenruppert.openprobatum.views;

import com.svenruppert.openprobatum.assessment.QualityMetricsService;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The quality-metrics surface for authors and reviewers (concept §20.2):
 * per-assessment pass rates + average scores, and the question bank's
 * composition by lifecycle status and difficulty. Read-only.
 *
 * @since V00.30.00
 */
@Route(value = MetricsView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.AUTHOR, AuthorizationRole.REVIEWER})
public class MetricsView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "metrics";

  public MetricsView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("metrics.heading", "Quality metrics"),
        tr("metrics.subtitle", "Assessment pass rates and question-bank composition.")));

    QualityMetricsService metrics = new QualityMetricsService();

    root.add(new H3(tr("metrics.section.assessments", "Assessments")));
    var assessmentMetrics = metrics.allAssessmentMetrics();
    if (assessmentMetrics.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.CHART,
          tr("metrics.empty.title", "No assessments yet"),
          tr("metrics.empty.body", "Pass-rate metrics appear once assessments exist.")));
    } else {
      assessmentMetrics.forEach(m -> root.add(assessmentRow(m)));
    }

    root.add(new H3(tr("metrics.section.bank", "Question bank")));
    Div bank = new Div();
    bank.getElement().setAttribute("data-bank-total",
        String.valueOf(metrics.bankByStatus().values().stream().mapToLong(Long::longValue).sum()));
    metrics.bankByStatus().forEach((status, count) ->
        bank.add(chip("data-bank-status", status.name(), status.name() + ": " + count)));
    metrics.bankByDifficulty().forEach((difficulty, count) ->
        bank.add(chip("data-bank-difficulty", difficulty.name(), difficulty.name() + ": " + count)));
    root.add(bank);

    root.add(new H3(tr("metrics.section.labs", "Labs")));
    var labMetrics = metrics.allLabMetrics();
    if (labMetrics.isEmpty()) {
      root.add(new Span(tr("metrics.labs.empty", "No labs yet.")));
    } else {
      labMetrics.forEach(m -> root.add(labRow(m)));
    }
  }

  private Div labRow(QualityMetricsService.LabMetrics m) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-lab", m.labId().toString());
    card.getElement().setAttribute("data-verify-rate", String.valueOf(Math.round(m.verifyRate() * 100)));
    card.getElement().setAttribute("data-submissions", String.valueOf(m.submissions()));

    card.add(new H4(m.title()));
    card.add(new Span(tr("metrics.verifyrate", "Verify rate: {0}% ({1}/{2}); {3} rejected",
        Math.round(m.verifyRate() * 100), m.verified(), m.submissions(), m.rejected())));
    return card;
  }

  private Div assessmentRow(QualityMetricsService.AssessmentMetrics m) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-assessment", m.assessmentId().toString());
    card.getElement().setAttribute("data-pass-rate", String.valueOf(Math.round(m.passRate() * 100)));
    card.getElement().setAttribute("data-attempts", String.valueOf(m.attempts()));

    card.add(new H4(m.title()));
    card.add(new Span(tr("metrics.passrate", "Pass rate: {0}% ({1}/{2})",
        Math.round(m.passRate() * 100), m.passed(), m.attempts())));
    card.add(new Span(" · " + tr("metrics.avgscore", "Avg score: {0}%",
        Math.round(m.averageScore() * 100))));
    return card;
  }

  private Span chip(String attr, String value, String label) {
    Span chip = new Span(label);
    chip.getElement().setAttribute(attr, value);
    chip.getElement().getThemeList().add("badge pill contrast");
    chip.getStyle().set("margin-right", "var(--lumo-space-xs)");
    return chip;
  }
}
