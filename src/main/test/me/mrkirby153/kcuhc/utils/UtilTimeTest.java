package me.mrkirby153.kcuhc.utils;

import org.junit.Assert;
import org.junit.Test;

public class UtilTimeTest {

    @Test
    public void format() throws Exception {
        Assert.assertEquals("6.0 Seconds", UtilTime.format(1, 6000, UtilTime.TimeUnit.SECONDS));
    }

    @Test
    public void trim() throws Exception {
        Assert.assertEquals(6.0, UtilTime.trim(1, 6.01), 0.01);
    }

}