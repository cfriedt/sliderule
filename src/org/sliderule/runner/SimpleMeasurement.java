/*
 * Copyright (C) 2015 Christopher Friedt <chrisfriedt@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sliderule.runner;

import org.sliderule.model.*;

final class SimpleMeasurement implements Measurement {

	private final String description;
	private final PolymorphicType value;

	public SimpleMeasurement( String description, PolymorphicType value ) {
		this.description = description;
		this.value = value;
	}

	@Override
	public PolymorphicType value() {
		return value;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public String toString() {
		return description + ":" + value;
	}
}
