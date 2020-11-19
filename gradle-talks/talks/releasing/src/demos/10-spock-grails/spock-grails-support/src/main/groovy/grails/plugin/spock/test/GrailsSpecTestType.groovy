/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.spock.test

import grails.plugin.spock.test.listener.OverallRunListener

import org.junit.runner.JUnitCore
import org.codehaus.groovy.grails.test.GrailsTestTypeResult
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory

import org.spockframework.runtime.SpecUtil

import spock.config.RunnerConfiguration

class GrailsSpecTestType extends GrailsTestTypeSupport {
  public static final TEST_SUFFIXES = ["Spec", "Specification"].asImmutable()
  
  private final List<Class> specClasses = []
  private int featureCount = 0
  
  GrailsSpecTestType(String name, String relativeSourcePath) {
    super(name, relativeSourcePath)
  }
  
  protected List<String> getTestExtensions() {
    ["groovy"]
  }

  protected List<String> getTestSuffixes() { 
    TEST_SUFFIXES
  }
  
  JUnitReportsFactory createJUnitReportsFactory() {
    JUnitReportsFactory.createFromBuildBinding(buildBinding)
  }
  
  protected int doPrepare() {
    eachSourceFile { testTargetPattern, specSourceFile ->
      def specClass = sourceFileToClass(specSourceFile)
      if (SpecUtil.isRunnableSpec(specClass)) specClasses << specClass
    }

    optimizeSpecRunOrderIfEnabled()

    featureCount = specClasses.sum(0) { SpecUtil.getFeatureCount(it) }
    featureCount
  }

  protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
    def junit = new JUnitCore()
    def result = new GrailsSpecTestTypeResult()
    
    junit.addListener(new OverallRunListener(eventPublisher,
        createJUnitReportsFactory(), createSystemOutAndErrSwapper(), result,
        createGrails2TerminalListenerIfCan()))
    
    junit.run(specClasses as Class[])
    result
  }

  private void optimizeSpecRunOrderIfEnabled() {
    if (!SpecUtil.getConfiguration(RunnerConfiguration).optimizeRunOrder) return

    def specNames = specClasses.collect { it.name }
    def reordered = SpecUtil.optimizeRunOrder(specNames)
    def reordedPositions = [:]
    reordered.eachWithIndex { name, idx -> reordedPositions[name] = idx }
    specClasses.sort { reordedPositions[it.name] }
  }
  
  // Override to workaround GRAILS-7296
  protected File getSourceDir() {
    new File(buildBinding.grailsSettings.testSourceDir, relativeSourcePath)
  }
  
  protected createGrails2TerminalListenerIfCan() {
    try {
      def clazz = buildBinding.classLoader.loadClass("org.codehaus.groovy.grails.test.event.GrailsTestRunNotifier")
      clazz.newInstance(featureCount)
    } catch (ClassNotFoundException e) {
      null
    }
  }
}