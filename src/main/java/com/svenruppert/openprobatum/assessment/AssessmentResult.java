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

package com.svenruppert.openprobatum.assessment;

/**
 * The outcome of grading an assessment attempt (concept §9.8). The full score
 * is internal; the public validation page never shows it (§10.6, §11.3).
 *
 * @param correct the number of correctly answered questions
 * @param total   the number of questions
 * @param score   the fraction correct, in {@code [0, 1]}
 * @param passed  whether the score met the assessment's threshold
 * @since V00.10.00
 */
public record AssessmentResult(int correct, int total, double score, boolean passed) {
}
