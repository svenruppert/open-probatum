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

import com.svenruppert.openprobatum.catalog.CatalogRepository;
import com.svenruppert.openprobatum.catalog.CatalogRepositoryProvider;
import com.svenruppert.openprobatum.catalog.LearningPath;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * Minimal authoring surface (concept §16.1): an Author creates a complete
 * offering — title, description, visibility (+ access code) and one module —
 * without a developer. The offering is persisted to the catalog and appears for
 * learners immediately.
 *
 * @since V00.20.00
 */
@Route(value = AuthorView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.AUTHOR, AuthorizationRole.PLATFORM_ADMIN})
public class AuthorView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "author";

  private final CatalogRepository catalog = CatalogRepositoryProvider.repository();

  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final ComboBox<OfferingVisibility> visibility = new ComboBox<>();
  private final TextField accessCode = new TextField();
  private final TextField moduleTitle = new TextField();
  private final TextArea moduleContent = new TextArea();
  private final Span status = new Span();

  public AuthorView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("author.heading", "Create an offering"),
        tr("author.subtitle", "Publish a learning path for your learners.")));

    title.setLabel(tr("author.title", "Offering title"));
    title.setWidthFull();
    description.setLabel(tr("author.description", "Description"));
    description.setWidthFull();
    visibility.setLabel(tr("author.visibility", "Visibility"));
    visibility.setItems(OfferingVisibility.PUBLIC, OfferingVisibility.REGISTERED,
        OfferingVisibility.CODE);
    visibility.setValue(OfferingVisibility.PUBLIC);
    accessCode.setLabel(tr("author.code", "Access code (for code-gated)"));
    accessCode.setWidthFull();
    moduleTitle.setLabel(tr("author.module.title", "Module title"));
    moduleTitle.setWidthFull();
    moduleContent.setLabel(tr("author.module.content", "Module content"));
    moduleContent.setWidthFull();
    status.setVisible(false);

    Button create = new Button(tr("author.create", "Create offering"), e -> create());
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

    root.add(title, description, visibility, accessCode, moduleTitle, moduleContent, create, status);
  }

  void create() {
    String t = value(title);
    String mTitle = value(moduleTitle);
    String mContent = value(moduleContent);
    if (t.isBlank() || mTitle.isBlank() || mContent.isBlank()) {
      showStatus("INVALID", tr("author.error.required", "Title and a module are required."));
      return;
    }
    OfferingVisibility vis = visibility.getValue();
    if (vis == OfferingVisibility.CODE && value(accessCode).isBlank()) {
      showStatus("INVALID", tr("author.error.code", "A code-gated offering needs an access code."));
      return;
    }

    LearningPath path = new LearningPath(t, List.of(Module.mandatory(mTitle, mContent)));
    String desc = value(description);
    Offering offering = switch (vis) {
      case REGISTERED -> Offering.registeredPath(t, desc, path);
      case CODE -> Offering.codePath(t, desc, path, value(accessCode));
      default -> Offering.publicPath(t, desc, path);
    };
    catalog.save(offering);
    // The offering starts as a DRAFT and is submitted for review (§16.3); a
    // reviewer approves + publishes it before learners can reach it.
    new com.svenruppert.openprobatum.catalog.CatalogLifecycleService().submitForReview(offering.id());
    // Record authorship (§17.2) for the segregation-of-duties rule on review.
    com.svenruppert.openprobatum.content.ContentAuthorshipProvider.registry()
        .recordAuthor(offering.lineageId(), currentAuthorId());

    showStatus("CREATED", tr("author.success",
        "Offering ''{0}'' created and submitted for review.", t));
    status.getElement().setAttribute("data-offering-id", offering.id().toString());
    clearForm();
  }

  private void clearForm() {
    title.clear();
    description.clear();
    accessCode.clear();
    moduleTitle.clear();
    moduleContent.clear();
    visibility.setValue(OfferingVisibility.PUBLIC);
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    status.getElement().getThemeList().add(
        "badge pill " + ("CREATED".equals(marker) ? "success" : "error"));
    status.setVisible(true);
  }

  private static String value(TextField field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static String value(TextArea field) {
    return field.getValue() == null ? "" : field.getValue().trim();
  }

  private static Long currentAuthorId() {
    return com.svenruppert.jsentinel.authorization.api.SubjectStores.subjectStore()
        .currentSubject(com.svenruppert.openprobatum.security.model.AppUser.class)
        .map(com.svenruppert.openprobatum.security.model.AppUser::id)
        .orElse(null);
  }
}
