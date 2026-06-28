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

import com.svenruppert.openprobatum.catalog.CatalogIntegrityService;
import com.svenruppert.openprobatum.catalog.LearningResource;
import com.svenruppert.openprobatum.catalog.Module;
import com.svenruppert.openprobatum.catalog.Offering;
import com.svenruppert.openprobatum.catalog.OfferingAuthoringService;
import com.svenruppert.openprobatum.catalog.OfferingVisibility;
import com.svenruppert.openprobatum.catalog.ResourceType;
import com.svenruppert.openprobatum.content.ContentStatus;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.model.AppUser;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The offering-authoring surface (concept §16.1): an author manages their own
 * offerings end-to-end — an inventory with per-offering actions, and an editor
 * that builds the learning path from <em>multiple</em> modules in a
 * drag-reorderable, inline-editable table. Create produces a DRAFT (submitted for
 * review explicitly); editing a DRAFT updates it in place, editing a PUBLISHED
 * offering branches a fresh draft version. All logic lives in
 * {@link OfferingAuthoringService}; this view is the UI over it.
 *
 * @since V00.20.00
 */
@Route(value = AuthorView.NAV, layout = MainLayout.class)
@VisibleFor({AuthorizationRole.AUTHOR, AuthorizationRole.PLATFORM_ADMIN})
public class AuthorView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "author";

  /** A mutable editor row backing the module table (assembled into a {@link Module} on save). */
  static final class ModuleRow {
    private String title = "";
    private String content = "";
    private boolean mandatory = true;
    private final List<ResourceRow> resources = new ArrayList<>();
  }

  /** A mutable editor row backing a module's resource table (→ {@link LearningResource}). */
  static final class ResourceRow {
    private ResourceType type = ResourceType.ARTICLE;
    private String title = "";
    private String payload = "";
  }

  private final OfferingAuthoringService service = new OfferingAuthoringService();

  private final TextField title = new TextField();
  private final TextArea description = new TextArea();
  private final ComboBox<OfferingVisibility> visibility = new ComboBox<>();
  private final TextField accessCode = new TextField();
  private final ComboBox<Offering> prerequisite = new ComboBox<>();
  private final List<ModuleRow> moduleRows = new ArrayList<>();
  private final Grid<ModuleRow> moduleGrid = new Grid<>(ModuleRow.class, false);
  private final Grid<ResourceRow> resourceGrid = new Grid<>(ResourceRow.class, false);
  private final Div resourceSection = new Div();
  private final H3 resourceHeading = new H3();
  private final Span status = new Span();
  private final Div inventory = new Div();
  private final H3 editorHeading = new H3();

  /** The offering currently being edited, or {@code null} when creating a new one. */
  private Offering editing;
  private ModuleRow dragged;
  /** The module whose resources are being edited below the module table, or {@code null}. */
  private ModuleRow selectedModule;

  public AuthorView() {
    VerticalLayout root = getContent();
    root.add(new PageHeader(tr("author.heading", "Your offerings"),
        tr("author.subtitle", "Create and manage learning paths for your learners.")));

    root.add(new H3(tr("author.inventory.heading", "Your offerings")));
    inventory.setWidthFull();
    root.add(inventory);

    root.add(editorHeading);
    root.add(buildEditor());
    status.setVisible(false);
    root.add(status);

    resetForm();
    refreshInventory();
  }

  private VerticalLayout buildEditor() {
    VerticalLayout form = new VerticalLayout();
    form.setPadding(false);
    form.setSpacing(true);

    title.setLabel(tr("author.title", "Offering title"));
    title.setWidthFull();
    description.setLabel(tr("author.description", "Description"));
    description.setWidthFull();

    visibility.setLabel(tr("author.visibility", "Who can reach it"));
    visibility.setItems(OfferingVisibility.values());
    visibility.setItemLabelGenerator(this::visibilityLabel);
    visibility.setValue(OfferingVisibility.PUBLIC);
    visibility.addValueChangeListener(e -> updateGateFields());

    accessCode.setLabel(tr("author.code", "Access code"));
    accessCode.setWidthFull();
    prerequisite.setLabel(tr("author.prerequisite", "Prerequisite offering"));
    prerequisite.setItemLabelGenerator(Offering::title);
    prerequisite.setWidthFull();

    configureModuleGrid();
    configureResourceSection();

    Button addModule = new Button(tr("author.module.add", "Add module"), e -> addModuleRow());
    Button save = new Button(tr("author.save", "Save offering"), e -> saveOffering());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancel = new Button(tr("author.cancel", "New / clear"), e -> resetForm());

    form.add(title, description, visibility, accessCode, prerequisite,
        new H3(tr("author.modules.heading", "Modules (drag to reorder, edit inline)")),
        moduleGrid, addModule, resourceSection, new HorizontalLayout(save, cancel));
    updateGateFields();
    return form;
  }

  // ── Module table: inline-edit + drag-reorder ──────────────────────

  private void configureModuleGrid() {
    moduleGrid.addColumn(row -> moduleRows.indexOf(row) + 1)
        .setHeader(tr("author.module.order", "#")).setWidth("3em").setFlexGrow(0);
    moduleGrid.addComponentColumn(row -> {
      TextField field = new TextField();
      field.setValue(row.title);
      field.setWidthFull();
      field.addValueChangeListener(e -> row.title = e.getValue() == null ? "" : e.getValue());
      return field;
    }).setHeader(tr("author.module.title", "Title (lesson name)")).setFlexGrow(2);
    moduleGrid.addComponentColumn(row -> {
      TextField field = new TextField();
      field.setValue(row.content);
      field.setWidthFull();
      field.addValueChangeListener(e -> row.content = e.getValue() == null ? "" : e.getValue());
      return field;
    }).setHeader(tr("author.module.content", "Content (learning material)")).setFlexGrow(3);
    moduleGrid.addComponentColumn(row -> {
      Checkbox box = new Checkbox(row.mandatory);
      box.addValueChangeListener(e -> row.mandatory = e.getValue());
      return box;
    }).setHeader(tr("author.module.mandatory", "Mandatory")).setFlexGrow(0);
    moduleGrid.addComponentColumn(row -> {
      Button resources = new Button(
          tr("author.module.resources", "Resources ({0})", row.resources.size()),
          e -> selectModuleForResources(row));
      resources.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      resources.getElement().setAttribute("data-module-resources", String.valueOf(row.resources.size()));
      return resources;
    }).setHeader("").setFlexGrow(0);
    moduleGrid.addComponentColumn(row -> {
      Button remove = new Button(tr("author.module.remove", "Remove"), e -> {
        moduleRows.remove(row);
        if (selectedModule == row) {
          selectedModule = null;
          resourceSection.setVisible(false);
        }
        refreshModules();
      });
      remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      return remove;
    }).setHeader("").setFlexGrow(0);
    moduleGrid.setAllRowsVisible(true);

    moduleGrid.setRowsDraggable(true);
    moduleGrid.setDropMode(GridDropMode.BETWEEN);
    moduleGrid.addDragStartListener(e -> dragged = e.getDraggedItems().get(0));
    moduleGrid.addDropListener(this::onModuleDrop);
    moduleGrid.addDragEndListener(e -> dragged = null);
  }

  private void onModuleDrop(com.vaadin.flow.component.grid.dnd.GridDropEvent<ModuleRow> event) {
    ModuleRow target = event.getDropTargetItem().orElse(null);
    if (dragged == null || target == null || dragged == target) {
      return;
    }
    moduleRows.remove(dragged);
    int index = moduleRows.indexOf(target);
    if (event.getDropLocation() == GridDropLocation.BELOW) {
      index++;
    }
    moduleRows.add(index, dragged);
    refreshModules();
  }

  private void addModuleRow() {
    moduleRows.add(new ModuleRow());
    refreshModules();
  }

  private void refreshModules() {
    moduleGrid.setItems(new ArrayList<>(moduleRows));
  }

  // ── Resources per module (master-detail, P005) ────────────────────

  private void configureResourceSection() {
    resourceGrid.addComponentColumn(row -> {
      ComboBox<ResourceType> type = new ComboBox<>();
      type.setItems(ResourceType.values());
      type.setItemLabelGenerator(this::resourceTypeLabel);
      type.setValue(row.type);
      type.addValueChangeListener(e ->
          row.type = e.getValue() == null ? ResourceType.ARTICLE : e.getValue());
      return type;
    }).setHeader(tr("author.resource.type", "Type")).setFlexGrow(1);
    resourceGrid.addComponentColumn(row -> {
      TextField field = new TextField();
      field.setValue(row.title);
      field.setWidthFull();
      field.addValueChangeListener(e -> row.title = e.getValue() == null ? "" : e.getValue());
      return field;
    }).setHeader(tr("author.resource.title", "Title")).setFlexGrow(2);
    resourceGrid.addComponentColumn(row -> {
      TextField field = new TextField();
      field.setValue(row.payload);
      field.setWidthFull();
      field.addValueChangeListener(e -> row.payload = e.getValue() == null ? "" : e.getValue());
      return field;
    }).setHeader(tr("author.resource.payload", "Text or URL")).setFlexGrow(3);
    resourceGrid.addComponentColumn(row -> {
      Button remove = new Button(tr("author.resource.remove", "Remove"), e -> {
        if (selectedModule != null) {
          selectedModule.resources.remove(row);
          refreshResources();
          refreshModules();
        }
      });
      remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      return remove;
    }).setHeader("").setFlexGrow(0);
    resourceGrid.setAllRowsVisible(true);

    Button addResource = new Button(tr("author.resource.add", "Add resource"), e -> addResource());
    resourceSection.add(resourceHeading, resourceGrid, addResource);
    resourceSection.setVisible(false);
  }

  private void selectModuleForResources(ModuleRow row) {
    selectedModule = row;
    String name = row.title == null || row.title.isBlank()
        ? tr("author.resources.untitled", "(untitled module)") : row.title;
    resourceHeading.setText(tr("author.resources.heading", "Resources for module: {0}", name));
    resourceSection.setVisible(true);
    refreshResources();
  }

  private void addResource() {
    if (selectedModule == null) {
      return;
    }
    selectedModule.resources.add(new ResourceRow());
    refreshResources();
    refreshModules();
  }

  private void refreshResources() {
    resourceGrid.setItems(selectedModule == null
        ? List.of() : new ArrayList<>(selectedModule.resources));
  }

  private String resourceTypeLabel(ResourceType type) {
    return switch (type) {
      case ARTICLE -> tr("author.resourcetype.article", "Article (text)");
      case CHECKLIST -> tr("author.resourcetype.checklist", "Checklist (text)");
      case VIDEO_REFERENCE -> tr("author.resourcetype.video", "Video (URL)");
      case DOWNLOAD -> tr("author.resourcetype.download", "Download (URL)");
      case EXTERNAL_LINK -> tr("author.resourcetype.link", "External link (URL)");
    };
  }

  // ── Inventory ─────────────────────────────────────────────────────

  private void refreshInventory() {
    inventory.removeAll();
    List<Offering> mine = service.myOfferings(currentAuthorId());
    if (mine.isEmpty()) {
      inventory.add(new Span(tr("author.inventory.empty", "No offerings yet — create one below.")));
      return;
    }
    mine.forEach(o -> inventory.add(offeringCard(o)));
  }

  private Div offeringCard(Offering offering) {
    Div card = new Div();
    card.getElement().setAttribute("data-offering", offering.id().toString());
    card.getElement().setAttribute("data-offering-status", offering.status().name());
    card.getStyle().set("padding", "var(--lumo-space-s)").set("margin-bottom", "var(--lumo-space-xs)")
        .set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "var(--lumo-border-radius-m)");

    Span name = new Span(offering.title());
    name.getStyle().set("font-weight", "600").set("margin-right", "var(--lumo-space-s)");
    Span state = new Span(offering.status().name());
    state.getElement().getThemeList().add("badge pill");

    HorizontalLayout actions = new HorizontalLayout();
    actions.add(action("edit", tr("author.action.edit", "Edit"), e -> loadForEdit(offering)));
    if (offering.status() == ContentStatus.DRAFT) {
      actions.add(action("submit", tr("author.action.submit", "Submit for review"), e -> {
        service.submitForReview(offering.id());
        showStatus("SUBMITTED", tr("author.submitted", "''{0}'' submitted for review.", offering.title()));
        refreshInventory();
      }));
      Button delete = action("delete", tr("author.action.delete", "Delete"),
          e -> deleteOffering(offering));
      delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
      actions.add(delete);
    }
    if (offering.isPublished()) {
      actions.add(action("deactivate", tr("author.action.deactivate", "Deactivate"), e -> {
        service.deactivate(offering.id());
        showStatus("DEACTIVATED", tr("author.deactivated", "''{0}'' deactivated.", offering.title()));
        refreshInventory();
      }));
    }
    card.add(name, state, actions);
    return card;
  }

  private static Button action(String marker, String label,
                               com.vaadin.flow.component.ComponentEventListener<
                                   com.vaadin.flow.component.ClickEvent<Button>> onClick) {
    Button button = new Button(label, onClick);
    button.getElement().setAttribute("data-action", marker);
    return button;
  }

  private void deleteOffering(Offering offering) {
    CatalogIntegrityService.IntegrityVerdict verdict = service.delete(offering.id());
    if (verdict.allowed()) {
      showStatus("DELETED", tr("author.deleted", "''{0}'' deleted.", offering.title()));
    } else {
      showStatus("BLOCKED", tr("author.delete.blocked", "Cannot delete ''{0}'': {1}",
          offering.title(), String.join("; ", verdict.blockers())));
    }
    refreshInventory();
  }

  // ── Create / edit ─────────────────────────────────────────────────

  private void loadForEdit(Offering offering) {
    editing = offering;
    title.setValue(offering.title());
    description.setValue(offering.description());
    visibility.setValue(offering.visibility());
    accessCode.setValue(offering.accessCodeOpt().orElse(""));
    refreshPrerequisiteItems();
    offering.prerequisiteOfferingIdOpt().ifPresent(id ->
        prerequisite.setValue(service.myOfferings(currentAuthorId()).stream()
            .filter(o -> o.id().equals(id)).findFirst().orElse(null)));
    selectedModule = null;
    resourceSection.setVisible(false);
    moduleRows.clear();
    offering.path().modules().forEach(m -> {
      ModuleRow row = new ModuleRow();
      row.title = m.title();
      row.content = m.content();
      row.mandatory = m.mandatory();
      m.resources().forEach(res -> {
        ResourceRow rr = new ResourceRow();
        rr.type = res.type();
        rr.title = res.title();
        rr.payload = res.payload();
        row.resources.add(rr);
      });
      moduleRows.add(row);
    });
    refreshModules();
    updateGateFields();
    editorHeading.setText(tr("author.editor.edit", "Edit offering"));
  }

  void saveOffering() {
    String t = title.getValue() == null ? "" : title.getValue().trim();
    List<Module> modules;
    try {
      modules = collectModules();
    } catch (IllegalArgumentException invalid) {
      showStatus("INVALID", tr("author.error.resource",
          "A resource needs a title and a valid payload (a URL for video/download/link)."));
      return;
    }
    if (t.isBlank() || modules.isEmpty()) {
      showStatus("INVALID", tr("author.error.required", "A title and at least one module are required."));
      return;
    }
    OfferingVisibility vis = visibility.getValue();
    String code = accessCode.getValue() == null ? "" : accessCode.getValue().trim();
    if (vis == OfferingVisibility.CODE && code.isBlank()) {
      showStatus("INVALID", tr("author.error.code", "A code-gated offering needs an access code."));
      return;
    }
    UUID prereqId = prerequisite.getValue() == null ? null : prerequisite.getValue().id();
    if (vis == OfferingVisibility.PREREQUISITE && prereqId == null) {
      showStatus("INVALID", tr("author.error.prerequisite", "Pick the prerequisite offering."));
      return;
    }
    String desc = description.getValue() == null ? "" : description.getValue().trim();
    String codeOrNull = vis == OfferingVisibility.CODE ? code : null;
    UUID prereqOrNull = vis == OfferingVisibility.PREREQUISITE ? prereqId : null;

    Offering saved = editing == null
        ? service.createDraft(t, desc, vis, codeOrNull, prereqOrNull, modules, currentAuthorId())
        : service.saveEdit(editing, t, desc, vis, codeOrNull, prereqOrNull, modules);
    status.getElement().setAttribute("data-offering-id", saved.id().toString());
    showStatus("SAVED", tr("author.saved", "''{0}'' saved as a draft.", t));
    resetForm();
    refreshInventory();
  }

  private List<Module> collectModules() {
    List<Module> modules = new ArrayList<>();
    for (ModuleRow row : moduleRows) {
      String mt = row.title == null ? "" : row.title.trim();
      String mc = row.content == null ? "" : row.content.trim();
      if (mt.isBlank() || mc.isBlank()) {
        continue;
      }
      List<LearningResource> resources = collectResources(row);
      modules.add(row.mandatory
          ? Module.mandatory(mt, mc, resources) : Module.optional(mt, mc, resources));
    }
    return modules;
  }

  /**
   * Builds a module's resources, skipping blank rows. Throws
   * {@link IllegalArgumentException} on an invalid resource (e.g. a non-URL payload
   * for a video/download/link) via the {@link LearningResource} invariants.
   */
  private List<LearningResource> collectResources(ModuleRow row) {
    List<LearningResource> resources = new ArrayList<>();
    for (ResourceRow r : row.resources) {
      String rt = r.title == null ? "" : r.title.trim();
      String rp = r.payload == null ? "" : r.payload.trim();
      if (rt.isBlank() || rp.isBlank()) {
        continue;
      }
      resources.add(new LearningResource(r.type, rt, rp));
    }
    return resources;
  }

  private void resetForm() {
    editing = null;
    selectedModule = null;
    resourceSection.setVisible(false);
    title.clear();
    description.clear();
    accessCode.clear();
    prerequisite.clear();
    visibility.setValue(OfferingVisibility.PUBLIC);
    moduleRows.clear();
    moduleRows.add(new ModuleRow());
    refreshModules();
    refreshPrerequisiteItems();
    updateGateFields();
    editorHeading.setText(tr("author.editor.create", "Create an offering"));
  }

  private void refreshPrerequisiteItems() {
    List<Offering> candidates = service.myOfferings(currentAuthorId()).stream()
        .filter(o -> editing == null || !o.lineageId().equals(editing.lineageId()))
        .toList();
    prerequisite.setItems(candidates);
  }

  private void updateGateFields() {
    OfferingVisibility vis = visibility.getValue();
    accessCode.setVisible(vis == OfferingVisibility.CODE);
    prerequisite.setVisible(vis == OfferingVisibility.PREREQUISITE);
  }

  private String visibilityLabel(OfferingVisibility vis) {
    return switch (vis) {
      case PUBLIC -> tr("author.visibility.public", "Anyone (public)");
      case REGISTERED -> tr("author.visibility.registered", "Registered users");
      case CODE -> tr("author.visibility.code", "With an access code");
      case PREREQUISITE -> tr("author.visibility.prerequisite", "After a prerequisite");
    };
  }

  private void showStatus(String marker, String message) {
    status.setText(message);
    status.getElement().setAttribute("data-result", marker);
    status.getElement().getThemeList().clear();
    boolean ok = !"INVALID".equals(marker) && !"BLOCKED".equals(marker);
    status.getElement().getThemeList().add("badge pill " + (ok ? "success" : "error"));
    status.setVisible(true);
  }

  private static Long currentAuthorId() {
    return SubjectStores.subjectStore()
        .currentSubject(AppUser.class)
        .map(AppUser::id)
        .orElse(null);
  }
}
