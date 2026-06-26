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

package com.svenruppert.openprobatum.lab;

/**
 * The state of a learner's {@link LabSubmission} (concept §16.3): freshly
 * submitted and awaiting a verdict, verified by an assessor (the practical
 * evidence that can earn a credential), or rejected with feedback.
 *
 * @since V00.40.00
 */
public enum SubmissionStatus {
  /** Submitted by the learner, awaiting an assessor's verdict. */
  SUBMITTED,
  /** Verified by an assessor — valid practical evidence. */
  VERIFIED,
  /** Rejected by an assessor, with feedback for the learner. */
  REJECTED
}
