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

import com.svenruppert.openprobatum.bundle.Bundle;
import com.svenruppert.openprobatum.bundle.BundleAccessService;
import com.svenruppert.openprobatum.bundle.BundleService;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.progress.ProgressService;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.EmptyState;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.openprobatum.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.List;

/**
 * The learner's bundle surface (concept §7.x): browse published {@link Bundle}s
 * and join one — joining entitles the learner to every member offering at once.
 * Each member shows the learner's progress towards the bundle credential.
 *
 * @since V00.50.00
 */
@Route(value = BundleView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class BundleView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "bundles";

  public BundleView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("bundles.heading", "Bundles"),
        tr("bundles.subtitle", "Join a package and earn its credential by completing every offering.")));

    List<Bundle> published = new BundleService().published().stream()
        .sorted(Comparator.comparing(Bundle::title))
        .toList();
    if (published.isEmpty()) {
      root.add(new EmptyState(VaadinIcon.PACKAGE,
          tr("bundles.empty.title", "No bundles yet"),
          tr("bundles.empty.body", "Published bundles will appear here.")));
      return;
    }
    published.forEach(b -> root.add(card(b)));
  }

  private Div card(Bundle bundle) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("max-width", "420px").set("padding", "var(--lumo-space-m)")
        .set("margin-bottom", "var(--lumo-space-m)");
    card.getElement().setAttribute("data-bundle", bundle.id().toString());

    card.add(new H3(bundle.title()), new Paragraph(bundle.description()));

    BundleAccessService access = new BundleAccessService();
    List<Offering> members = access.members(bundle);
    ProgressService progress = new ProgressService();
    Long me = currentLearnerId();
    Div memberList = new Div();
    for (Offering m : members) {
      int pct = me == null ? 0 : progress.percentComplete(me, m);
      Span line = new Span(m.title() + " — " + pct + "%");
      line.getElement().setAttribute("data-member", m.id().toString());
      line.getElement().setAttribute("data-progress", String.valueOf(pct));
      line.getStyle().set("display", "block");
      memberList.add(line);
    }
    card.add(memberList);

    Button join = new Button(tr("bundles.action.join", "Join bundle"), e -> {
      AppUser user = currentUser();
      if (user != null) {
        new BundleAccessService().grant(user, bundle);
      }
      render();
    });
    join.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    join.getElement().setAttribute("data-action", "join");
    card.add(join);
    return card;
  }

  private static AppUser currentUser() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class).orElse(null);
  }

  private static Long currentLearnerId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
