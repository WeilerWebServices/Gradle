plugins {
    eclipse
    `eclipse-wtp`
    idea
    announce
    `build-announcements`
    antlr
    `compare-gradle-builds`
    `build-init`
    wrapper
    checkstyle
    codenarc
    findbugs
    jdepend
    pmd
    `help-tasks`
    `project-report`
    `project-reports`
    ear
    `visual-studio`
    xcode
    `play-ide`
    `ivy-publish`
    jacoco
    `coffeescript-base`
    envjs
    `javascript-base`
    jshint
    rhino
    `java-lang`
    `jvm-resources`
    `assembler-lang`
    assembler
    `c-lang`
    c
    // `cpp-application`
    `cpp-lang`
    // `cpp-library`
    cpp
    `objective-c-lang`
    `objective-c`
    `objective-cpp-lang`
    `objective-cpp`
    // `swift-application`
    // `swift-library`
    `windows-resource-script`
    `windows-resources`
    `scala-lang`
    `maven-publish`
    maven
    osgi
    `binary-base`
    `component-base`
    `component-model-base`
    `language-base`
    `lifecycle-base`
    `jvm-component`
    `clang-compiler`
    `gcc-compiler`
    `microsoft-visual-cpp-compiler`
    `native-component-model`
    `native-component`
    `standard-tool-chains`
    `play-application`
    `play-coffeescript`
    `play-javascript`
    play
    `java-gradle-plugin`
    application
    base
    distribution
    `groovy-base`
    groovy
    `java-base`
    `java-library-distribution`
    `java-library`
    java
    war
    publishing
    `build-dashboard`
    `reporting-base`
    `scala-base`
    scala
    signing
    `junit-test-suite`
    // `cpp-test-suite`
    `cunit-test-suite`
    cunit
    `google-test-test-suite`
    `google-test`
    // xctest
}

val clean: org.gradle.api.tasks.Delete by tasks
clean.delete("buildSrc")
