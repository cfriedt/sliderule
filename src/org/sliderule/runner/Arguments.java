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
import java.util.*;

final class Arguments {
	static final String HOME;

	int debug = -1;
	boolean help = false;
	boolean dry_run = false;
	List<String> benchmark = new ArrayList<String>();
	List<String> vm = new ArrayList<String>();
	List<String> instrument = new ArrayList<String>();
	int max_trials = 30;
	int time_limit = 30;
	String run_name;
	boolean print_config;
	String delimiter = ",";
	File config = new File( HOME + File.separator + ".sliderule" + File.separator + "config.properties" );
	File directory = new File( HOME + File.separator + ".sliderule" );
	Properties parameters = new Properties();
	Properties config_properties = new Properties();

	List<Class<?>> bench_classes = new ArrayList<Class<?>>();

	static {
		HOME = "" + System.getenv().get( "HOME" );
	}
}
