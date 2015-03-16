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

public final class SlideRuleMain {
	private SlideRuleMain() {}
	private Arguments arguments = new Arguments();
	private Context context = new Context();
	
	@SuppressWarnings("serial")
	private static class HelpException extends Exception {}
	@SuppressWarnings("serial")
	private static class SpecificClassNotFoundException extends Exception {
		public String name;
	}
	@SuppressWarnings("serial")
	private static class SpecificIllegalArgumentException extends Exception {
		public String option_given;
	}
	@SuppressWarnings("serial")
	private static class SpecificMissingArgumentException extends Exception {
		public String option_given;
	}
	@SuppressWarnings("serial")
	private static class SpecificFileNotFoundException extends Exception {
		public String name;
	}
	@SuppressWarnings("serial")
	private static class SpecificIllegalPropertyException extends Exception {
		public String option_given;
	}
	@SuppressWarnings("serial")
	private static class NonUniformBenchmarkClassesException extends Exception {}
	
	private void parseArguments( String[] arg )
	throws SpecificMissingArgumentException, SpecificIllegalArgumentException, SpecificFileNotFoundException, SpecificIllegalPropertyException, SpecificClassNotFoundException, HelpException
	{
		boolean parse_options = true;
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		for( int i=0; i<arg.length; i++ ) {
			if ( parse_options ) {
				if ( "-h".equals( arg[i] ) || "--help".equals( arg[i] ) ) {
					arguments.help = true;
					throw new HelpException();
				} else if ( "-n".equals( arg[i] ) || "--dry-run".equals( arg[i] ) ) {
					arguments.dry_run = true;
					continue;
				} else if ( "-b".equals( arg[i] ) || "--benchmark".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.benchmark = Arrays.asList( arg[i].split( arguments.delimiter ) );
					continue;
				} else if ( "-m".equals( arg[i] ) || "--vm".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.vm = Arrays.asList( arg[i].split( arguments.delimiter ) );
					continue;
				} else if ( "-i".equals( arg[i] ) || "--instrument".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.instrument = Arrays.asList( arg[i].split( arguments.delimiter ) );
					continue;
				} else if ( "-t".equals( arg[i] ) || "--trials".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					try {
						arguments.trials = Integer.parseInt( arg[i] );
					} catch( NumberFormatException e ) {
						SpecificIllegalArgumentException siae = new SpecificIllegalArgumentException();
						siae.option_given = arg[i];
						throw siae;
					}
					continue;
				} else if ( "-l".equals( arg[i] ) || "--time-limit".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					try {
						arguments.time_limit = Integer.parseInt( arg[i] );
					} catch( NumberFormatException e ) {
						SpecificIllegalArgumentException siae = new SpecificIllegalArgumentException();
						siae.option_given = arg[i];
						throw siae;
					}
					continue;
				} else if ( "-r".equals( arg[i] ) || "--run-name".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.run_name = arg[i];
					continue;
				} else if ( "-p".equals( arg[i] ) || "--print-config".equals( arg[i] ) ) {
					arguments.print_config = true;
					continue;
				} else if ( "-d".equals( arg[i] ) || "--delimiter".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.delimiter = arg[i];
					continue;
				} else if ( "-c".equals( arg[i] ) || "--config".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.config = new File( arg[i] );
					if ( ! ( arguments.config.exists() && arguments.config.isFile() && arguments.directory.canRead() && arguments.directory.canWrite() ) ) {
						SpecificFileNotFoundException sfnfe = new SpecificFileNotFoundException();
						sfnfe.name = arg[i];
						throw sfnfe;
					}
				} else if ( "--directory".equals( arg[i] ) ) {
					if ( i+1 >= arg.length ) {
						SpecificMissingArgumentException smae = new SpecificMissingArgumentException();
						smae.option_given = arg[i];
						throw smae;
					}
					i++;
					arguments.directory = new File( arg[i] );
					if ( ! ( arguments.directory.exists() && arguments.directory.isDirectory() && arguments.directory.canRead() && arguments.directory.canWrite() && arguments.directory.canExecute() ) ) {
						SpecificFileNotFoundException sfnfe = new SpecificFileNotFoundException();
						sfnfe.name = arg[i];
						throw sfnfe;
					}
					continue;
				} else if ( arg[i].startsWith( "-D" ) ) {
					String[] prop = arg[i].substring( "-D".length() ).split( "=" );
					if ( prop.length != 2 ) {
						SpecificIllegalPropertyException sipe = new SpecificIllegalPropertyException();
						sipe.option_given = arg[i];
						throw sipe;
					}
					arguments.parameters.put( prop[0], prop[1] );
					continue;
				} else if ( arg[i].startsWith( "-C" ) ) {
					String[] prop = arg[i].substring( "-C".length() ).split( "=" );
					if ( prop.length != 2 ) {
						SpecificIllegalPropertyException sipe = new SpecificIllegalPropertyException();
						sipe.option_given = arg[i];
						throw sipe;
					}
					arguments.config_properties.put( prop[0], prop[1] );
					continue;
				} else {
					parse_options = false;
				}
			}
			if ( ! parse_options ) {
				// The only arguments that can appear now are names of classes.
				// Each class must be uniform with the others in terms of annotations.
				// It helps if all test classes extend one common base class.
				Class<?> klass;
				try {
					klass = cl.loadClass( arg[i] );
				} catch( ClassNotFoundException e ) {
					SpecificClassNotFoundException scnfe = new SpecificClassNotFoundException();
					scnfe.name = arg[i];
					throw scnfe;
				}
				arguments.bench_classes.add( klass );
			}
		}
	}

	private static boolean fieldSetsEqual( Set<Field> a, Set<Field> b ) {
		if ( a.size() != b.size() ) {
			return false;
		}
		// FIXME: verify field types 
		for( Field f: a) {
			boolean found = false;
			for( Field g: b ) {
				if ( g.getName().equals( f.getName() ) ) {
					found = true;
					break;
				}
			}
			if ( !found ) {
				return false;
			}
		}
		return true;
	}

	private static boolean methodSetsEqual( Set<Method> a, Set<Method> b ) {
		if ( a.size() != b.size() ) {
			return false;
		}
		// FIXME: verify field types 
		for( Method f: a) {
			boolean found = false;
			for( Method g: b ) {
				if ( g.getName().equals( f.getName() ) ) {
					found = true;
					break;
				}
			}
			if ( !found ) {
				return false;
			}
		}
		return true;
	}

	private void setup()
	throws NonUniformBenchmarkClassesException
	{
		AnnotatedClass prev_ac = null;
		
		for( Class<?> klass: arguments.bench_classes ) {

			AnnotatedClass ac = new AnnotatedClass( klass );

			ArrayList<Field> fs = new ArrayList<Field>();
			fs.addAll( Arrays.asList( klass.getFields() ) );
			fs.addAll( Arrays.asList( klass.getDeclaredFields() ) );

			for( Field f: fs ) {
				f.setAccessible( true );
				ac.filterField( f );
			}

			ArrayList<Method> ms = new ArrayList<Method>();
			ms.addAll( Arrays.asList( klass.getMethods() ) );
			ms.addAll( Arrays.asList( klass.getDeclaredMethods() ) );

			for( Method m: ms ) {
				m.setAccessible( true );
				ac.filterMethod( m );
			}
			
			if ( null == prev_ac ) {
				if ( ! ac.getBenchmarkMethods().isEmpty() ) {
					prev_ac = ac;
					context.addAnnotatedClass( ac );	
				}
			} else {
				if ( !(
					fieldSetsEqual( ac.getParamFields(), prev_ac.getParamFields() ) &&
					methodSetsEqual( ac.getAfterExperimentMethods(), prev_ac.getAfterExperimentMethods() ) &&
					methodSetsEqual( ac.getBeforeExperimentMethods(), prev_ac.getBeforeExperimentMethods() ) &&
					methodSetsEqual( ac.getBenchmarkMethods(), prev_ac.getBenchmarkMethods() ) &&
					methodSetsEqual( ac.getAfterRepMethods(), prev_ac.getAfterRepMethods() ) &&
					methodSetsEqual( ac.getBeforeRepMethods(), prev_ac.getBeforeRepMethods() ) &&
					methodSetsEqual( ac.getMacrobenchmarkMethods(), prev_ac.getMacrobenchmarkMethods() )
				) ) {
					throw new NonUniformBenchmarkClassesException();
				}
			}
		}
	}
	private void go()
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException
	{
		Algorithm.evaluate( arguments, context );
	}
	private static void usage() {
		System.out.print( UsageString.usage_string );
	}

	public static void main( String[] arg ) {
		int return_val = -1;
		SlideRuleMain srm = new SlideRuleMain();
		try {
			srm.parseArguments( arg );
			srm.setup();
			srm.go();
			return_val = 0;
		} catch ( SpecificClassNotFoundException e ) {
			System.err.println( "could not load class '" + e.name + "'" );
		} catch ( SpecificMissingArgumentException e ) {
			System.err.println( "argument '" + e.option_given + "' requires parameter" );
		} catch ( SpecificIllegalArgumentException e ) {
			System.err.println( "argument '" + e.option_given + "' has invalid parameter" );
		} catch ( SpecificFileNotFoundException e ) {
			System.err.println( "argument '" + e.name + "' does not exist, is not a directory or file, or has wrong mode" );
		} catch ( SpecificIllegalPropertyException e ) {
			System.err.println( "argument '" + e.option_given + "' is not in property format" );
		} catch ( HelpException e ) {
			return_val = 0;
		} catch ( NonUniformBenchmarkClassesException e ) {
			System.err.println( "benchmark classes must be uniform when trying to compare multiple benchmark classes" );
		} catch ( InstantiationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IllegalAccessException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IllegalArgumentException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( InvocationTargetException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ( 0 != return_val || srm.arguments.help ) {
			usage();
		}
		System.exit( return_val );
	}
}
