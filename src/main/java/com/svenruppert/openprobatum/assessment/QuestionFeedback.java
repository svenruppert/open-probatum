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
 * Immediate practice feedback for one answered question (concept §9.5): whether
 * the chosen options were correct and the didactic explanation. Practice mode
 * shows this and issues no credential.
 *
 * @param correct     whether the answer was exactly right
 * @param explanation the didactic note (may be empty)
 * @since V00.20.00
 */
public record QuestionFeedback(boolean correct, String explanation) {
}
