package org.codehaus.groovy.grails.domain

import groovy.util.logging.Commons
import org.codehaus.groovy.grails.commons.ComponentCapableDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent

import java.lang.reflect.Modifier

/**
 * Clears static Grails "instance api" instances from domain classes when 
 * ApplicationContext's ContextClosedEvent is received. 
 * 
 * 
 * @author Lari Hotari
 *
 */
@Commons
class GrailsDomainClassCleaner implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware  {
    protected GrailsApplication grailsApplication
    protected ApplicationContext applicationContext
    
    public GrailsDomainClassCleaner(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    public void onApplicationEvent(ContextClosedEvent event) {
        if(event.applicationContext == this.applicationContext || this.applicationContext == null) {
            clearAllStaticApiInstances()
            removeDomainClassMetaClasses()
        }
    }

    protected clearAllStaticApiInstances() {
        for (dc in grailsApplication.domainClasses) {
            clearStaticApiInstances(dc.clazz)
        }
    }

    protected clearStaticApiInstances(Class clazz) {
        clazz.metaClass.getProperties().each { MetaProperty metaProperty ->
            if(Modifier.isStatic(metaProperty.getModifiers()) && metaProperty.name ==~ /^(instance|static).+Api$/) {
                log.info("Clearing static property ${metaProperty.name} in ${clazz.name}")
                try {
                    metaProperty.setProperty(clazz, null)
                } catch (e) {
                    log.warn("Error clearing static property ${metaProperty.name} in ${clazz.name}", e)
                }
            }
        }
    }

    // clear static state added by DomainClassGrailsPlugin.enhanceDomainClasses
    protected removeDomainClassMetaClasses() {
        for (dc in grailsApplication.domainClasses) {
            def metaClassRegistry = GroovySystem.getMetaClassRegistry()
            if (dc instanceof ComponentCapableDomainClass) {
                for (GrailsDomainClass component in dc.getComponents()) {
                    metaClassRegistry.removeMetaClass(component.clazz)
                }
            }
            metaClassRegistry.removeMetaClass(dc.clazz)
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }
}
