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

package org.sliderule.model;

import java.lang.reflect.*;
import java.util.*;

public final class PolymorphicType {

	public final Class<?> klass;
	public final Object value;

	public PolymorphicType( Class<?> klass, Object value ) {
		this.klass = klass;
		this.value = value;
		if ( null == klass ) {
			throw new IllegalArgumentException( "klass may not be null" );
		}
		if ( !( null == value || klass.isPrimitive() || Number.class.isAssignableFrom( klass ) || klass.isAssignableFrom( value.getClass() ) ) ) {
			throw new IllegalArgumentException( "class '" + klass + "' is not assignable from '" + value.getClass() + "'" );
		}
	}

	public static PolymorphicType infer( Class<?> klass, String s )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{

		PolymorphicType pmt = null;
		Object o = null;

		if ( Byte.class == klass || byte.class == klass ) {
			o = new Byte( Byte.parseByte( s ) );
		} else if ( Short.class == klass || short.class == klass ) {
			o = new Short( Short.parseShort( s ) );
		} else if ( Integer.class == klass || int.class == klass ) {
			o = new Integer( Integer.parseInt( s ) );
		} else if ( Long.class == klass || long.class == klass ) {
			o = new Long( Long.parseLong( s ) );
		} else if ( Float.class == klass || float.class == klass ) {
			o = new Float( Float.parseFloat( s ) );
		} else if ( Double.class == klass || double.class == klass ) {
			o = new Double( Double.parseDouble( s ) );
		} else if ( Character.class == klass || char.class == klass ) {
			o = new Character( s.charAt( 0 ) );
		} else if ( String.class == klass ) {
			o = s;
		} else {
			// special case if s == null or s.equals( "(null)" )
			if ( null == s || "null".equals( s ) || "(null)".equals( s ) ) {
				o = null;
			} else {
				// some other type of object
				// must provide a constructor that accepts a single String argument
				Constructor<?>[] cs = klass.getConstructors();
				for( Constructor<?> c: cs ) {
					Class<?>[] p = c.getParameterTypes();
					if ( 1 == p.length && java.lang.String.class == p[0] ) {
						c.setAccessible( true );
						o = c.newInstance( s );
					}
				}
			}
		}
		pmt = new PolymorphicType( klass, o );
		return pmt;
	}

	@Override
	public String toString() {
		return "" + klass + ":" + ( null == value ? "(null)" : "" + value );
	}
}
