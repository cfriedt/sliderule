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

public final class PolymorphicType implements Comparable<PolymorphicType> {

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public int compareTo( PolymorphicType o ) {
		Class<?> klass = this.klass;
		if ( null == o ) {
			return -1;
		}
		if ( o.klass == klass ) {
			if ( klass.isPrimitive() ) {
				if ( byte.class == klass ) {
					klass = Byte.class;
				} else if ( short.class == klass ) {
					klass = Short.class;
				} else if ( int.class == klass ) {
					klass = Integer.class;
				} else if ( long.class == klass ) {
					klass = Long.class;
				} else if ( float.class == klass ) {
					klass = Float.class;
				} else if ( double.class == klass ) {
					klass = Double.class;
				}
			}
			if ( Number.class.isAssignableFrom( klass ) ) {
				if ( value == null && o.value == null ) {
					return 0;
				}
				if ( o.value == null ) {
					return -1;
				}
				if ( value == null ) {
					return 1;
				}
				if ( Byte.class == klass ) {
					return Byte.compare( (byte)value, (byte)o.value );
				} else if ( Short.class == klass ) {
					return Short.compare( (short)value, (short)o.value );
				} else if ( Integer.class == klass ) {
					return Integer.compare( (int)value, (int)o.value );
				} else if ( Long.class == klass ) {
					return Long.compare( (long)value, (long)o.value );
				} else if ( Float.class == klass ) {
					return Float.compare( (float)value, (float)o.value );
				} else if ( Double.class == klass ) {
					return Double.compare( (double)value, (double)o.value );
				} else {
					throw new IllegalStateException();
				}
			} else if ( Comparable.class.isAssignableFrom( klass ) ) {
				Comparable c = (Comparable) value;
				return c.compareTo( o.value );
			}
		}
		// in the case that classes are not equal or do not implement Comparable
		if ( o.klass.hashCode() < klass.hashCode() ) {
			return -1;
		} else if ( o.klass.hashCode() > klass.hashCode() ) {
			return 1;
		} else {
			if ( o.klass == klass ) {
				return 0;
			} else {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public boolean equals( Object o ) {
		if ( null == o || PolymorphicType.class != o.getClass() ) {
			return false;
		}
		PolymorphicType pmt = (PolymorphicType) o; 
		return 0 == compareTo( pmt );
	}

	@Override
	public String toString() {
		return "" + klass + ":" + ( null == value ? "(null)" : "" + value );
	}

	public static String nameParams( Field[] param, PolymorphicType[] param_value ) {
		String r = "[";
		if ( !( null == param || 0 == param.length ) ) {
			for( int i=0; i<param.length; i++ ) {
				r += param[ i ].getName();
				r += ":";
				r += param_value[ i ];
				if ( i < param.length - 1 ) {
					r += ",";
				}
			}
		}
		r += "]";
		return r;
	}
}
