package com.ca.cdd.plugins.gradletesting.utils;

import com.ca.cdd.plugins.gradletesting.GradleTestSuite;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by menyo01 on 02/01/2018.
 *
 *
 */

public class ExecutionUtilsTest{

    @Test
    public void testParseTestSuiteId() {
        List<String> list = ExecutionUtils.parseTestSuiteId(":::test::test1.java");
        assertEquals(3, list.size());
        assertEquals(":", list.get(0));
        assertEquals("test" , list.get(1));
        assertEquals("test1.java", list.get(2) );
    }

    @Test
    public void testParseTestSuiteIdWithProject() {
        List<String> list = ExecutionUtils.parseTestSuiteId(":project::test::test1.java");
        assertEquals(3, list.size());
        assertEquals(":project", list.get(0));
        assertEquals("test" , list.get(1));
        assertEquals("test1.java", list.get(2) );
    }

    @Test
    public void testParseEmptyTestSuiteId(){
        List<String> list = ExecutionUtils.parseTestSuiteId("");
        assertEquals(0 ,list.size());
        list = ExecutionUtils.parseTestSuiteId(null);
        assertEquals(0 ,list.size());
    }


    @Test
    public void testCreateGradleTestSuiteRootProject() {
        GradleTestSuite gradleTestSuite = ExecutionUtils.toGradleTestSuite(":::test::test1.java");
        assertEquals(":::test::test1.java", gradleTestSuite.getId());
        assertEquals(":", gradleTestSuite.getProject());
        assertEquals("test", gradleTestSuite.getTask());
        assertEquals("test1.java", gradleTestSuite.getTestClass());
    }


    @Test
    public void testCreateGradleTestSuite() {
        GradleTestSuite gradleTestSuite = ExecutionUtils.toGradleTestSuite(":project::test::test1.java");
        assertEquals(":project::test::test1.java", gradleTestSuite.getId());
        assertEquals(":project", gradleTestSuite.getProject());
        assertEquals("test", gradleTestSuite.getTask());
        assertEquals("test1.java", gradleTestSuite.getTestClass());
    }

    @Test(expected = IllegalStateException.class)
    public void testNegativeCreateGradleTestSuite() {
        ExecutionUtils.toGradleTestSuite("illegalTestSuiteId");
    }

}
