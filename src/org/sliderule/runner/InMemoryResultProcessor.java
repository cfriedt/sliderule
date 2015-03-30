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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.sliderule.api.*;
import org.sliderule.model.*;

/**
 * <p><b>In-Memory Result Processor</b></p>
 *
 * <p>This class simply stores benchmarking results in a {@link TreeMap}
 * sorted by {@link UUID}.</p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 */
public class InMemoryResultProcessor implements ResultProcessor {

	protected static final TreeMap<UUID,ArrayList<Trial>> trial_set = new TreeMap<UUID,ArrayList<Trial>>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processTrial( Trial trial ) {

		ArrayList<Trial> alt;

		if ( ! trial_set.containsKey( trial.id() ) ) {
			alt = new ArrayList<Trial>();
			trial_set.put( trial.id(), alt );
		}

		alt = trial_set.get( trial.id() );
		alt.add( trial );

		for( Measurement m: trial.measurements() ) {
			if ( "warning".equals( m.description() ) ) {
				System.err.println( m.value() );
			}
		}
	}

	public static TreeMap<UUID,ArrayList<Trial>> trialSet() {
		return trial_set;
	}

	// XXX: I now understand why Caliper decided to use JPA
	// https://docs.oracle.com/html/E13981_01/ent30qry001.htm

	public static TreeMap<UUID,ArrayList<Trial>> filterByUUID( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<UUID> ids  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( UUID id: ids ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( e.getKey() == id ) {
					r.put( key, val );
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByClass( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<Class<?>> classes  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Class<?> clazz: classes ) {
			TreeMap<UUID,ArrayList<Trial>> more = filterByClass( trial_set, clazz );
			r.putAll( more );
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByClass( TreeMap<UUID,ArrayList<Trial>> trial_set, Class<?> clazz  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			UUID key = e.getKey();
			ArrayList<Trial> val = e.getValue();
			if ( 0 == val.size() ) {
				return r;
			}
			Trial prototype = val.get( 0 );
			if ( null == prototype ) {
				throw new IllegalStateException( "trials cannot be null!" );
			}
			if ( ! ( prototype instanceof SimpleTrial ) ) {
				continue;
			}
			SimpleTrial st = (SimpleTrial) prototype;
			SlideRuleAnnotations sra = st.getSlideRuleAnnotations();
			Class<?> annotated_class = sra.getAnnotatedClass();
			if ( annotated_class == clazz ) {
				r.put( key, val );
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByClassName( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<String> classes  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( String clazz: classes ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( 0 == val.size() ) {
					return r;
				}
				Trial prototype = val.get( 0 );
				if ( null == prototype ) {
					throw new IllegalStateException( "trials cannot be null!" );
				}
				if ( ! ( prototype instanceof SimpleTrial ) ) {
					continue;
				}
				SimpleTrial st = (SimpleTrial) prototype;
				SlideRuleAnnotations sra = st.getSlideRuleAnnotations();
				Class<?> annotated_class = sra.getAnnotatedClass();
				if ( annotated_class.getName().equals( clazz ) ) {
					r.put( key, val );
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByMethod( TreeMap<UUID,ArrayList<Trial>> trial_set, Method method ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			UUID key = e.getKey();
			ArrayList<Trial> val = e.getValue();
			if ( 0 == val.size() ) {
				return r;
			}
			Trial prototype = val.get( 0 );
			if ( null == prototype ) {
				throw new IllegalStateException( "trials cannot be null!" );
			}
			if ( ! ( prototype instanceof SimpleTrial ) ) {
				continue;
			}
			SimpleTrial st = (SimpleTrial) prototype;
			Method meth = st.getMethod();
			if ( meth.equals( method ) ) {
				r.put( key, val );
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByMethod( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<Method> methods  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Method method: methods ) {
			TreeMap<UUID,ArrayList<Trial>> more = filterByMethod( trial_set, method );
			r.putAll( more );
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByMethodName( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<String> methods  ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( String method: methods ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( 0 == val.size() ) {
					return r;
				}
				Trial prototype = val.get( 0 );
				if ( null == prototype ) {
					throw new IllegalStateException( "trials cannot be null!" );
				}
				if ( ! ( prototype instanceof SimpleTrial ) ) {
					continue;
				}
				SimpleTrial st = (SimpleTrial) prototype;
				Method meth = st.getMethod();
				if ( meth.getName().equals( method ) ) {
					r.put( key, val );
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByField( TreeMap<UUID,ArrayList<Trial>> trial_set, List<Field> fields ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( Field field: fields ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( 0 == val.size() ) {
					return r;
				}
				Trial prototype = val.get( 0 );
				if ( null == prototype ) {
					throw new IllegalStateException( "trials cannot be null!" );
				}
				if ( ! ( prototype instanceof SimpleTrial ) ) {
					continue;
				}
				SimpleTrial st = (SimpleTrial) prototype;
				Field[] fi = st.getParam();
				for( Field f: fi ) {
					if ( f.equals( field ) ) {
						r.put( key, val );
					}
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByFieldName( TreeMap<UUID,ArrayList<Trial>> trial_set, Set<String> fields ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( String field: fields ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( 0 == val.size() ) {
					return r;
				}
				Trial prototype = val.get( 0 );
				if ( null == prototype ) {
					throw new IllegalStateException( "trials cannot be null!" );
				}
				if ( ! ( prototype instanceof SimpleTrial ) ) {
					continue;
				}
				SimpleTrial st = (SimpleTrial) prototype;
				Field[] fi = st.getParam();
				for( Field f: fi ) {
					if ( f.getName().equals( field ) ) {
						r.put( key, val );
					}
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByParamValue( TreeMap<UUID,ArrayList<Trial>> trial_set, Field field, PolymorphicType value ) {

		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();

		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {

			UUID key = e.getKey();
			ArrayList<Trial> val = e.getValue();

			SimpleTrial st = (SimpleTrial) val.get( 0 );
			Field[] f = st.getParam();
			PolymorphicType[] pmt = st.getParamValue();

			for( int i = 0; i < pmt.length; i++ ) {
				if ( ! f[ i ].equals( field ) ) {
					continue;
				}
				if ( pmt[ i ].equals( value ) ) {
					r.put( key, val );
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByParamValue( TreeMap<UUID,ArrayList<Trial>> trial_set, List<Field> fields, List<PolymorphicType> values ) {

		if ( fields.size() != values.size() ) {
			throw new IllegalArgumentException( "fields and values must be same size" );
		}

		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();

		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {

			UUID key = e.getKey();
			ArrayList<Trial> val = e.getValue();

			SimpleTrial st = (SimpleTrial) val.get( 0 );
			Field[] f = st.getParam();
			PolymorphicType[] pmt = st.getParamValue();

			boolean fields_match = true;
			for( int i = 0; i < values.size(); i++ ) {
				if ( ! f[ i ].equals( fields.get( i ) ) ) {
					fields_match = false;
					break;
				}
			}
			if ( ! fields_match ) {
				continue;
			}



			boolean values_match = true;
			for( int i = 0; i < fields.size(); i++ ) {
				if ( ! pmt[ i ].equals( values.get( i ) ) ) {
					values_match = false;
					break;
				}
			}
			if ( ! values_match ) {
				continue;
			}

			r.put( key, val );
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByParamStringValue( TreeMap<UUID,ArrayList<Trial>> trial_set, List<String> fields, List<String> values ) {
		if ( fields.size() != values.size() ) {
			throw new IllegalArgumentException( "fields and values must be same size" );
		}
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			for( String field: fields ) {
				UUID key = e.getKey();
				ArrayList<Trial> val = e.getValue();
				if ( 0 == val.size() ) {
					return r;
				}
				Trial prototype = val.get( 0 );
				if ( null == prototype ) {
					throw new IllegalStateException( "trials cannot be null!" );
				}
				if ( ! ( prototype instanceof SimpleTrial ) ) {
					continue;
				}
				SimpleTrial st = (SimpleTrial) prototype;
				Field[] fi = st.getParam();
				PolymorphicType[] pmt = st.getParamValue();
				for( int i = 0; i < fi.length; i++ ) {
					if ( fi[ i ].equals( field ) ) {
						for( String v: values ) {
							if ( ( "" + pmt[ i ].value ).equals( v ) ) {
								r.put( key, val );
							}
						}
					}
				}
			}
		}
		return r;
	}
	public static TreeMap<UUID,ArrayList<Trial>> filterByMicro( TreeMap<UUID,ArrayList<Trial>> trial_set, boolean micro ) {
		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			UUID key = e.getKey();
			ArrayList<Trial> val = e.getValue();
			if ( 0 == val.size() ) {
				return r;
			}
			Trial prototype = val.get( 0 );
			if ( null == prototype ) {
				throw new IllegalStateException( "trials cannot be null!" );
			}
			if ( ! ( prototype instanceof SimpleTrial ) ) {
				continue;
			}
			SimpleTrial st = (SimpleTrial) prototype;
			if ( micro && st.isMicro() ) {
				r.put( key, val );
			} else if ( !( micro || st.isMicro() ) ) {
				r.put( key, val );
			}
		}
		return r;
	}
}
