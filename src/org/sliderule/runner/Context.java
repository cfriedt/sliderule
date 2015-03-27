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

import java.util.*;

import org.sliderule.api.*;

class Context {

	final HashSet<SlideRuleAnnotations> bench_classes;
	ResultProcessor results_processor;

	public Context() {
		bench_classes = new HashSet<SlideRuleAnnotations>();
		results_processor = new ConsoleResultProcessor();
	}

	public void addAnnotatedClass( SlideRuleAnnotations ac ) {
		bench_classes.add( ac );
	}

	public Set<SlideRuleAnnotations> getAnnotatedClasses() {
		return bench_classes;
	}

	public void setResultProcessor( ResultProcessor p ) {
		results_processor = p;
	}
}
