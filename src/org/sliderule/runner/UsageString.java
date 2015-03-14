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
import java.net.*;

final class UsageString {

	static final String usage_string;
	
	static {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		String pkg = SlideRuleMain.class.getPackage().getName().replace( ".", "/" );
		String usage_string_resource = pkg + "/" + "usage.txt";
		URL u = cl.getResource( usage_string_resource );
		BufferedReader br;
		try {
			br = new BufferedReader( new InputStreamReader( u.openStream() ) );
		} catch ( IOException e ) {
			throw new IllegalStateException( "unable to get open " + usage_string_resource );
		}
		String s = "";
		try {
			for( String line = br.readLine(); null != line; s += ( line + "\n" ), line = br.readLine() );
		} catch ( IOException e ) {
			throw new IllegalStateException( "unable to get read from " + usage_string_resource );
		}
		
		s = s.replace( "Caliper", "SlideRule" );
		s = s.replace( "caliper", "sliderule" );
		s = s.replace( "com.google", "org" );
		// we support benchmarking multiple classes with the same parameters and configuration, one after another
		s = s.replace( "<benchmark_class_name> [options...]", "[options...] <benchmark_class_names>" );
		// obviously a different website is required
		s = s.replace( "http://code.google.com/p/", "http://github.com/cfriedt/sliderule/" );
		usage_string = s;
	}
	
	private UsageString() {}
}
