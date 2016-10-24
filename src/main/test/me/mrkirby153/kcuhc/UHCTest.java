package me.mrkirby153.kcuhc;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UHC.class)
public class UHCTest {

    @Test
    public void adminTest() throws Exception {

        mockStatic(UHC.class);

        PowerMockito.when(UHC.isAdmin("admin")).thenReturn(true);
        PowerMockito.when(UHC.isAdmin("not-admin")).thenReturn(false);

        Assert.assertTrue(UHC.isAdmin("admin"));
        Assert.assertFalse(UHC.isAdmin("not-admin"));
    }

}