package com.netflix.archaius;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.visitor.PrintStreamVisitor;

public class AppConfigTest {
    @Test
    public void testAppAndLibraryLoading() throws ConfigException {
        Properties props = new Properties();
        props.setProperty("env", "prod");
        
        DefaultAppConfig config = DefaultAppConfig.builder()
            .withApplicationConfigName("application")
            .withProperties(props)
            .withDefaultCascadingStrategy(ConcatCascadeStrategy.from("${env}"))
            .build();
        
        System.out.println(config);
        
        Assert.assertTrue(config.getBoolean("application.loaded"));
        Assert.assertTrue(config.getBoolean("application-prod.loaded", false));
        
        Assert.assertFalse(config.getBoolean("libA.loaded", false));
        
        config.addConfigLast(config.newLoader().load("libA"));
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertFalse(config.getBoolean("libB.loaded", false));
        Assert.assertEquals("libA", config.getString("libA.overrideA"));
        
        config.addConfigFirst(config.newLoader().load("libB"));
        
        Assert.assertTrue(config.getBoolean("libA.loaded"));
        Assert.assertTrue(config.getBoolean("libB.loaded"));
        Assert.assertEquals("libB", config.getString("libA.overrideA"));
        
        config.accept(new PrintStreamVisitor());
    }
    
    @Test
    public void interpolationShouldWork() {
        System.setProperty("env", "prod");
        
        DefaultAppConfig config = DefaultAppConfig.builder()
            .withApplicationConfigName("application")
            .build();
        
        config.setProperty("replacement.env", "${env}");
        
        Assert.assertEquals("prod", config.getString("replacement.env"));
    }
    
    @Test(expected=IllegalStateException.class)
    public void infiniteInterpolationRecursionShouldFail() {
        System.setProperty("env", "${env}");
        
        DefaultAppConfig config = DefaultAppConfig.builder()
            .withApplicationConfigName("application")
            .build();
        
        config.setProperty("replacement.env", "${env}");
        
        Assert.assertEquals("prod", config.getString("replacement.env"));
    }
    
    @Test
    public void numericInterpolationShouldWork() {
        DefaultAppConfig config = DefaultAppConfig.builder()
                .withApplicationConfigName("application")
                .build();
            
        config.setProperty("default", "123");
        config.setProperty("value",   "${default}");
        
        Assert.assertEquals((long)123L, (long)config.getLong("value"));
    }
    
}
