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

package com.svenruppert.openprobatum.views.main;
import com.svenruppert.openprobatum.i18n.I18nSupport;
import com.svenruppert.openprobatum.security.roles.AuthorizationRole;
import com.svenruppert.openprobatum.security.roles.VisibleFor;
import com.svenruppert.openprobatum.views.MainLayout;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Route(value = PushDemoView.PATH, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.LEARNER)
public class PushDemoView extends VerticalLayout implements I18nSupport {

  public static final String PATH = "pushDemo";

  private static final String K_STATUS_IDLE = "push.status.idle";
  private static final String K_BUTTON_START = "push.button.start";

  private final Paragraph status = new Paragraph();
  private final Button startButton = new Button();

  private ScheduledExecutorService executor;
  private ScheduledFuture<?> task;

  public PushDemoView() {
    status.setText(tr(K_STATUS_IDLE, "No push yet"));
    startButton.setText(tr(K_BUTTON_START, "Start push"));
    add(status, startButton);

    startButton.addClickListener(event -> startPushDemo());
  }

  private void startPushDemo() {
    if (executor != null && !executor.isShutdown()) {
      return;
    }

    UI ui = UI.getCurrent();

    executor = Executors.newSingleThreadScheduledExecutor();
    task = executor.scheduleAtFixedRate(() -> {
      if (!ui.isAttached()) {
        // UI gone (e.g. delayed detach / session timeout) — stop firing
        // immediately instead of spinning at 1 Hz until onDetach runs.
        if (task != null) {
          task.cancel(false);
        }
        executor.shutdown();
        return;
      }
      ui.access(() -> status.setText("Server Push: " + LocalTime.now()));
    }, 0, 1, TimeUnit.SECONDS);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    if (executor != null) {
      executor.shutdownNow();
    }
    super.onDetach(detachEvent);
  }
}
