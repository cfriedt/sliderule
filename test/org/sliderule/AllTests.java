package org.sliderule;

import org.junit.runner.*;
import org.junit.runners.*;
import org.sliderule.stats.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ChiSquaredTest.class, NormalTest.class, StudentsTTest.class, FactorialTest.class
})
public class AllTests {
}
