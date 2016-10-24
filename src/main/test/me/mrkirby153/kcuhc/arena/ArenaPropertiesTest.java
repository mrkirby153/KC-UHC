package me.mrkirby153.kcuhc.arena;

import org.junit.Assert;
import org.junit.Test;

public class ArenaPropertiesTest {


    @Test
    public void testProperties(){
        ArenaProperties.Property<Boolean> testProp = new ArenaProperties.Property<>("test", true);

        Assert.assertTrue(testProp.get());
        Assert.assertTrue(testProp.getDefault());
        Assert.assertEquals("test", testProp.getName());

        testProp.setValue(false);
        Assert.assertFalse(testProp.get());
        testProp.reset();
        Assert.assertTrue(testProp.get());
    }

}