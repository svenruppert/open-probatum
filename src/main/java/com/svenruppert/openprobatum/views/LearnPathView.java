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

import com.svenruppert.openprobatum.access.EntitlementService;
import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.LearningResource;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The learning surface (concept §8.4): a learner works through an offering's
 * modules + resources and marks each complete; the progress bar updates and the
 * path reports 100 % once every mandatory module is done. Reachable only when
 * the learner is entitled to the offering (§12).
 *
 * @since V00.20.00
 */
@Route(value = LearnPathView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class LearnPathView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "learn";

  private final CatalogRepository catalog = CatalogRepositoryProvider.repository();

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String id) {
    VerticalLayout root = getContent();
    root.removeAll();

    Optional<Offering> found = parse(id).flatMap(catalog::findById);
    if (found.isEmpty()) {
      Span unknown = new Span(tr("learn.unknown", "This offering does not exist."));
      unknown.getElement().setAttribute("data-learn-result", "UNKNOWN");
      root.add(new PageHeader(tr("learn.heading", "Learn")), unknown);
      return;
    }
    render(root, found.get());
  }

  private void render(VerticalLayout root, Offering offering) {
    AppUser user = currentUser();
    if (!new EntitlementService().canAccess(user, offering).isGranted()) {
      Span denied = new Span(tr("learn.denied", "You do not have access to this offering yet."));
      denied.getElement().setAttribute("data-learn-result", "DENIED");
      denied.getElement().getThemeList().add("badge error pill");
      root.add(new PageHeader(offering.title()), denied);
      return;
    }

    root.add(new PageHeader(offering.title(), offering.description()));
    root.add(progressBar(user, offering));

    Long userId = user == null ? null : user.id();
    Set<UUID> done = new ProgressService().completedModules(userId, offering.id());
    for (Module module : offering.path().modules()) {
      root.add(moduleSection(offering, module, done.contains(module.id()), root));
    }
  }

  private ProgressBar progressBar(AppUser user, Offering offering) {
    int percent = new ProgressService().percentComplete(user == null ? null : user.id(), offering);
    ProgressBar bar = new ProgressBar(0, 100, percent);
    bar.getElement().setAttribute("data-percent", Integer.toString(percent));
    return bar;
  }

  private Div moduleSection(Offering offering, Module module, boolean complete, VerticalLayout root) {
    Div section = new Div();
    section.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    section.getElement().setAttribute("data-module", module.id().toString());

    H4 title = new H4(module.title() + (module.mandatory() ? "" : " (optional)"));
    section.add(title, new Paragraph(module.content()));

    for (LearningResource resource : module.resources()) {
      section.add(resourceLine(resource));
    }

    if (complete) {
      Span badge = new Span(tr("learn.module.done", "Completed"));
      badge.getElement().setAttribute("data-module-state", "DONE");
      badge.getElement().getThemeList().add("badge success pill");
      section.add(badge);
    } else {
      AppUser user = currentUser();
      Button mark = new Button(tr("learn.module.complete", "Mark complete"), e -> {
        new ProgressService().markModuleComplete(user == null ? null : user.id(), offering.id(), module.id());
        // Completing the last mandatory module may finish the path — unlock any
        // offering that lists this one as a prerequisite (P003).
        new com.svenruppert.openprobatum.access.PathCompletionService()
            .unlockDependents(user, offering);
        setParameter(null, offering.id().toString()); // re-render with updated progress
      });
      mark.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      mark.getElement().setAttribute("data-module-state", "OPEN");
      section.add(mark);
    }
    return section;
  }

  private Div resourceLine(LearningResource resource) {
    Div line = new Div();
    line.getElement().setAttribute("data-resource", resource.type().name());
    if (resource.type().isUrlPayload()) {
      line.add(new Anchor(resource.payload(), resource.title()));
    } else {
      line.add(new Span(resource.title() + ": "), new Span(resource.payload()));
    }
    return line;
  }

  private static Optional<UUID> parse(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(id.trim()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static AppUser currentUser() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class).orElse(null);
  }
}
