/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven

import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.maven.aether.AetherDependencyManager
import org.codehaus.groovy.grails.resolve.maven.aether.AetherExcludeResolver
import spock.lang.Specification
import spock.lang.Issue

/**
 * @author Graeme Rocher
 * @since 2.3
 */
class AetherExcludeResolverSpec extends Specification{
    void "Test that the IvyExcludeResolver resolves excludes"() {
        given:"An IvyDependencyManager with some dependencies"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
            }
            dependencies {
                compile "commons-validator:commons-validator:1.4.0"
            }

        }
        def excludeResolver = new AetherExcludeResolver(dependencyManager)

        when:"The excludes are resolved"
        final excludes = excludeResolver.resolveExcludes()
        def validatorExcludes = !excludes.isEmpty() ? excludes.values().iterator().next() : null

        then:"They are valid"
        !excludes.isEmpty()
        validatorExcludes.size() == 3
        validatorExcludes.find { Dependency d -> d.name == 'commons-beanutils'}

    }

    @Issue('GPRELEASE-59')
    void "Test that an excluded dependency that isn't available is excluded"() {
        given:"A dependency with an exclusion of an unavailable artifact"
            def dependencyManager = new AetherDependencyManager()
            dependencyManager.parseDependencies {
                repositories {
                    mavenCentral()
                }
                dependencies {
                    compile('com.octo.captcha:jcaptcha:1.0') {
                        excludes 'javax.servlet:servlet-api', 'com.jhlabs:imaging'
                    }
                }
            }
        def excludeResolver = new AetherExcludeResolver(dependencyManager)

        when:"The excludes are resolved"
        final excludes = excludeResolver.resolveExcludes()
        def validatorExcludes = !excludes.isEmpty() ? excludes.values().iterator().next() : null

        then:"They are valid"
        !excludes.isEmpty()
        validatorExcludes.size() == 5
        validatorExcludes.find { Dependency d -> d.name == 'commons-collections'}
        validatorExcludes.find { Dependency d -> d.name == 'commons-logging'}
        validatorExcludes.find { Dependency d -> d.name == 'jcaptcha-api'}
    }

    void "Test that dependency can be used as key in map returned by resolveExcludes method"() {
        given:"An dependency manager with some dependencies"
        def dependencyManager = new AetherDependencyManager()
        dependencyManager.parseDependencies {
            repositories {
                mavenCentral()
            }
            dependencies {
                compile "commons-validator:commons-validator:1.4.0", {
                    exclude 'commons-logging'
                }
            }

        }
        def excludeResolver = new AetherExcludeResolver(dependencyManager)

        def appDeps = dependencyManager.getApplicationDependencies('compile')

        def commonsValidatorDep = appDeps.find { it.name == 'commons-validator' }

        when:"The excludes are resolved"
        final excludes = excludeResolver.resolveExcludes()

        then:"They are valid"
        !excludes.isEmpty()

        def exclusions = excludes[commonsValidatorDep]
        exclusions != null
        exclusions.size() == 3
    }
}
