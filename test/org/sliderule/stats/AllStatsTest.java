package org.sliderule.stats;

import org.junit.runner.*;
import org.junit.runners.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ChiSquaredTest.class, NormalTest.class, StudentsTTest.class,
})
public class AllStatsTest {
}
