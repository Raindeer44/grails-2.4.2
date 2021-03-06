/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.web.api;

import grails.util.Environment;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.util.List;

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher;
import org.codehaus.groovy.grails.web.util.TagLibraryMetaUtils;
import org.codehaus.groovy.grails.web.util.WithCodecHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Enhances controller classes with a method missing implementation for tags at compile time.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class ControllerTagLibraryApi extends CommonWebApi {

    private static final long serialVersionUID = 1;

    private transient TagLibraryLookup tagLibraryLookup;
    private boolean developmentMode = Environment.isDevelopmentMode();

    public ControllerTagLibraryApi(GrailsPluginManager pluginManager) {
        super(pluginManager);
    }

    public ControllerTagLibraryApi() {
        super(null);
    }

    @Autowired
    public void setTagLibraryLookup(TagLibraryLookup lookup) {
        tagLibraryLookup = lookup;
    }

    @Autowired
    public void setGspTagLibraryLookup(TagLibraryLookup lookup) {
        tagLibraryLookup = lookup;
    }

    /**
     * Method missing implementation that handles tag invocation by method name
     *
     * @param instance The instance
     * @param methodName The method name
     * @param argsObject The arguments
     * @return The result
     */
    public Object methodMissing(Object instance, String methodName, Object argsObject) {
        Object[] args = argsObject instanceof Object[] ? (Object[])argsObject : new Object[]{argsObject};
        if (shouldHandleMethodMissing(instance, methodName, args)) {
            TagLibraryLookup lookup = getTagLibraryLookup();
            if (lookup != null) {
                GroovyObject tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, methodName);
                if (tagLibrary != null) {
                    if (!developmentMode) {
                        MetaClass controllerMc = GrailsMetaClassUtils.getMetaClass(instance);
                        TagLibraryMetaUtils.registerMethodMissingForTags(controllerMc, lookup,
                                GroovyPage.DEFAULT_NAMESPACE, methodName);
                    }
                    List<MetaMethod> respondsTo = tagLibrary.getMetaClass().respondsTo(tagLibrary, methodName, args);
                    if (respondsTo.size() > 0) {
                        return respondsTo.get(0).invoke(tagLibrary, args);
                    }
                }
            }
        }
        throw new MissingMethodException(methodName, instance.getClass(), args);
    }

    protected boolean shouldHandleMethodMissing(Object instance, String methodName, Object[] args) {
        return !"render".equals(methodName);
    }

    /**
     * Looks up namespaces on missing property
     *
     * @param instance The instance
     * @param propertyName The property name
     * @return The namespace or a MissingPropertyException
     */
    public Object propertyMissing(Object instance, String propertyName) {
        TagLibraryLookup lookup = getTagLibraryLookup();
        NamespacedTagDispatcher namespacedTagDispatcher = lookup.lookupNamespaceDispatcher(propertyName);
        if (namespacedTagDispatcher != null) {
            if (!developmentMode) {
                TagLibraryMetaUtils.registerPropertyMissingForTag(GrailsMetaClassUtils.getMetaClass(instance),propertyName, namespacedTagDispatcher);
            }
            return namespacedTagDispatcher;
        }

        throw new MissingPropertyException(propertyName, instance.getClass());
    }

    public TagLibraryLookup getTagLibraryLookup() {
        if (tagLibraryLookup == null) {
            ApplicationContext applicationContext = getApplicationContext(null);
            if (applicationContext != null) {
                try {
                    tagLibraryLookup = applicationContext.getBean(TagLibraryLookup.class);
                } catch (BeansException e) {
                    return null;
                }
            }
        }
        return tagLibraryLookup;
    }

    public Object withCodec(Object instance, Object codecInfo, Closure<?> body) {
        return WithCodecHelper.withCodec(getGrailsApplication(null), codecInfo, body);
    }
}
