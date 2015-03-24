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
import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;

import javax.management.*;

import org.sliderule.model.*;

import com.sun.management.*;

final class VM {

	private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

	private VM() {}

	static String getOption( String key ) throws IOException  {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy( mbs, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class );
		VMOption vmo = bean.getVMOption( key );
		return vmo.getValue();
	}
	static void setOption( String key, String value ) throws IOException  {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy( mbs, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class );
		bean.setVMOption( key, value );
	}
	static boolean getInline() throws IOException {
		return Boolean.parseBoolean( getOption( "Inline" ) );
	}
	static void setInline( boolean on ) throws IOException {
		setOption( "Inline", "" + on );
	}
	static int getMinInliningThreshold() throws NumberFormatException, IOException {
		return Integer.parseInt( getOption( "MinInliningThreshold" ) );
	}
	static void setMinInliningThreshold( int threshold ) throws IOException {
		setOption( "MinInliningThreshold", "" + threshold );
	}
	static int getCompileThreshold() throws NumberFormatException, IOException  {
		return Integer.parseInt( getOption( "CompileThreshold" ) );
	}
	static void setCompilethreshold( int threshold ) throws IOException {
		setOption( "CompileThreshold", "" + threshold );
	}
	static boolean getTieredCompilation() throws IOException {
		return Boolean.parseBoolean( getOption( "TieredCompilation" ) );
	}
	static void setTieredCompilation( boolean on ) throws IOException {
		setOption( "TieredCompilation", "" + on );
	}
	static int getTier2CompileThreshold() throws NumberFormatException, IOException {
		return Integer.parseInt( getOption( "Tier2CompileThreshold" ) );
	}
	static void setTier2CompileThreshold( int threshold ) throws IOException {
		setOption( "Tier2CompileThreshold", "" + threshold );
	}
	static boolean getUseCompiler() throws IOException {
		return Boolean.parseBoolean( getOption( "UseCompiler" ) );
	}
	static void setUseCompiler( boolean on ) throws IOException {
		setOption( "TieredCompilation", "" + on );
	}
	static List<String> getInputArguments() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		return arguments;
	}
}
