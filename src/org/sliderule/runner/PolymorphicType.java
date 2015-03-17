package org.sliderule.runner;

final class PolymorphicType {
	final Class<?> f;
	final Object value;
	
	public PolymorphicType( Class<?> f, Object value ) {
		this.f = f;
		this.value = value;
		if ( ! f.isAssignableFrom( value.getClass() ) ) {
			throw new IllegalArgumentException( "type '" + f + "' is not assignable from '" + value.getClass() + "'" );
		}
	}
}
