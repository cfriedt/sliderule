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
