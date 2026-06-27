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
import com.svenruppert.openprobatum.bundle.BundleRepositoryProvider;
import com.svenruppert.openprobatum.bundle.BundleService;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
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
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The bundle-authoring surface (concept §7.x / §16.3): an author curates a bundle
 * by title, description and a set of member offerings, then submits it for review.
 * Approval + publication happen in the reviewer's surface.
 *
 * @since V00.50.00
 */
@Route(value = BundleAuthorView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.AUTHOR, AuthorizationRole.PLATFORM_ADMIN})
public class BundleAuthorView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "bundles-author";

  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final MultiSelectComboBox<Offering> members = new MultiSelectComboBox<>();
  private final TextField tags = new TextField();
  private final Span status = new Span();

  public BundleAuthorView() {
    render();
  }

  private void render() {
    VerticalLayout root = getContent();
    root.removeAll();
    root.add(new PageHeader(tr("bundlesauthor.heading", "Bundle authoring"),
        tr("bundlesauthor.subtitle", "Curate a package of offerings.")));
    root.add(buildForm());
    root.add(buildList());
  }

  private Div buildForm() {
    Div form = new Div();
    form.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    form.getStyle().set("padding", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-l)");

    title.setLabel(tr("bundlesauthor.field.title", "Title"));
    title.setWidthFull();
    description.setLabel(tr("bundlesauthor.field.description", "Description"));
    description.setWidthFull();
    members.setLabel(tr("bundlesauthor.field.members", "Member offerings"));
    members.setItems(CatalogRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing(Offering::title)).toList());
    members.setItemLabelGenerator(Offering::title);
    members.setWidthFull();
    tags.setLabel(tr("bundlesauthor.field.tags", "Tags (comma-separated)"));
    tags.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("bundlesauthor.action.create", "Create bundle"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    form.add(title, description, members, tags, create, status);
    return form;
  }

  void create() {
    String t = value(title);
    java.util.Set<UUID> memberIds = members.getValue().stream()
        .map(Offering::id).collect(Collectors.toSet());
    if (t.isBlank() || memberIds.isEmpty()) {
      showStatus("INVALID", tr("bundlesauthor.error.invalid",
          "A title and at least one member offering are required."));
      return;
    }
    Bundle bundle = Bundle.draft(t, areaValue(description), memberIds)
        .withTags(java.util.Set.copyOf(splitCsv(value(tags))));
    new BundleService().create(bundle);
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(bundle.lineageId(), currentAuthorId());
    showStatus("CREATED", tr("bundlesauthor.success", "Bundle created as a draft."));
    clear();
    render();
  }

  private Div buildList() {
    Div list = new Div();
    var all = BundleRepositoryProvider.repository().all().stream()
        .sorted(Comparator.comparing((Bundle b) -> b.title()).thenComparingInt(Bundle::version))
        .toList();
    if (all.isEmpty()) {
      list.add(new EmptyState(VaadinIcon.PACKAGE,
          tr("bundlesauthor.empty.title", "No bundles yet"),
          tr("bundlesauthor.empty.body", "Create your first bundle above.")));
      return list;
    }
    all.forEach(b -> list.add(row(b)));
    return list;
  }

  private Div row(Bundle bundle) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_HERO_SURFACE);
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-s)");
    card.getElement().setAttribute("data-bundle", bundle.id().toString());

    H4 heading = new H4(bundle.title() + "  (v" + bundle.version() + ", "
        + bundle.offeringIds().size() + " offerings)");
    Span statusBadge = new Span(bundle.status().name());
    statusBadge.getElement().setAttribute("data-status", bundle.status().name());
    statusBadge.getElement().getThemeList().add(
        "badge pill " + (bundle.status() == ContentStatus.PUBLISHED ? "success" : "contrast"));

    card.add(heading, statusBadge);
    if (bundle.status() == ContentStatus.DRAFT) {
      Button submit = new Button(tr("bundlesauthor.action.submit", "Submit for review"), e -> {
        new BundleService().submitForReview(bundle.id());
        render();
      });
      submit.addThemeVariants(ButtonVariant.LUMO_SMALL);
      submit.getElement().setAttribute("data-action", "submit");
      card.add(submit);
    }
    return card;
  }

  private void clear() {
    title.clear();
    description.clear();
    members.clear();
    tags.clear();
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge pill " + ("CREATED".equals(marker) ? "success" : "error"));
    status.setVisible(true);
  }

  private static List<String> splitCsv(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private static String value(TextField field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static String areaValue(TextArea field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static Long currentAuthorId() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::id).orElse(null);
  }
}
