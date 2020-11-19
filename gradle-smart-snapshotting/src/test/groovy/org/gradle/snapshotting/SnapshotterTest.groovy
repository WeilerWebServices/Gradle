package org.gradle.snapshotting

import com.google.common.base.Charsets
import com.google.common.collect.ImmutableCollection
import com.google.common.collect.ImmutableList
import com.google.common.hash.HashCode
import com.google.common.io.ByteStreams
import org.gradle.snapshotting.cache.NoOpPhysicalHashCache
import org.gradle.snapshotting.cache.SimplePhysicalHashCache
import org.gradle.snapshotting.contexts.AbstractContext
import org.gradle.snapshotting.contexts.CompareStrategy
import org.gradle.snapshotting.contexts.Context
import org.gradle.snapshotting.contexts.NormalizationStrategy
import org.gradle.snapshotting.contexts.PhysicalSnapshotCollector
import org.gradle.snapshotting.files.Directoryish
import org.gradle.snapshotting.files.Fileish
import org.gradle.snapshotting.files.FileishWithContents
import org.gradle.snapshotting.files.MissingPhysicalFile
import org.gradle.snapshotting.files.PhysicalDirectory
import org.gradle.snapshotting.files.PhysicalSnapshot
import org.gradle.snapshotting.operations.ProcessDirectory
import org.gradle.snapshotting.operations.ProcessZip
import org.gradle.snapshotting.operations.SetContext
import org.gradle.snapshotting.rules.ProcessPropertyFile
import org.gradle.snapshotting.rules.Rule
import org.gradle.snapshotting.util.TestFile
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static org.gradle.snapshotting.rules.RuleBuilder.rule

class SnapshotterTest extends Specification {
    // Context for JVM classpaths
    static class ClasspathContext extends AbstractContext {
        ClasspathContext() {
            super(NormalizationStrategy.NONE, CompareStrategy.ORDER_SENSITIVE)
        }
    }

    // Context for JVM classpath entries (JAR files and directories)
    static class ClasspathEntryContext extends AbstractContext {
        ClasspathEntryContext() {
            super(NormalizationStrategy.RELATIVE, CompareStrategy.ORDER_INSENSITIVE)
        }
    }

    // No-fluff context for regular property snapshotting with relative path sensitivity
    static class DefaultContext extends AbstractContext {
        DefaultContext() {
            super(NormalizationStrategy.RELATIVE, CompareStrategy.ORDER_INSENSITIVE)
        }
    }

    // A list of WAR files (with potentially a single element)
    // This is kind of a workaround, as in most cases a property will only have a single WAR file
    // but when snapshotting we only see FileCollections (even if the property's type if File).
    static class WarList extends AbstractContext {
        WarList() {
            super(NormalizationStrategy.RELATIVE, CompareStrategy.ORDER_INSENSITIVE)
        }
    }

    // A WAR file
    static class War extends AbstractContext {
        War() {
            super(NormalizationStrategy.RELATIVE, CompareStrategy.ORDER_INSENSITIVE)
        }
    }

    private static final List<Rule> COMMON_RULES = ImmutableList.<Rule> builder()
        // Hash files in any context
        .add(rule().withType(Fileish) { file, context, operations ->
            if (file instanceof FileishWithContents) {
                context.recordSnapshot(file, file.getContentHash())
            } else if (file instanceof Directoryish) {
                context.recordSnapshot(file, Directoryish.HASH)
            } else if (file instanceof MissingPhysicalFile) {
                context.recordSnapshot(file, MissingPhysicalFile.HASH)
            } else {
                throw new IllegalStateException("Unknown file type: $file (${file.getClass().name})")
            }
        })
        .build()

    private static final List<Rule> COMMON_CLASSPATH_RULES = ImmutableList.<Rule> builder()
        // Treat JAR files as classpath entries inside the classpath
            .add(rule().in(ClasspathContext).withType(FileishWithContents) { file, context, operations ->
            def subContext = context.recordSubContext(file, ClasspathEntryContext)
            operations.add(new ProcessZip(file, subContext))
        })
        // Treat directories as classpath entries inside the classpath
            .add(rule().in(ClasspathContext).withType(PhysicalDirectory) { directory, context, operations ->
            def subContext = context.recordSubContext(directory, ClasspathEntryContext)
            operations.add(new ProcessDirectory(directory, subContext))
        })
        // Ignore empty directories inside classpath entries
            .add(rule().in(ClasspathEntryContext).withType(Directoryish) { directory, context, operations ->
        })
        .build()

    private static final List<Rule> RUNTIME_CLASSPATH_RULES = ImmutableList.<Rule> builder()
        .addAll(COMMON_CLASSPATH_RULES)
        .addAll(COMMON_RULES)
        .build()

    private static final List<Rule> COMPILE_CLASSPATH_RULES = ImmutableList.<Rule> builder()
        .addAll(COMMON_CLASSPATH_RULES)
        .add(rule().in(ClasspathEntryContext).withType(FileishWithContents).withExtension(".class") { file, context, operations ->
            // Generate ABI in real implementation
            HashCode abiHash = file.getContentHash()
            context.recordSnapshot(file, abiHash)
        })
        // Ignore everything that's not a .class
        .add(rule().in(ClasspathEntryContext).withType(FileishWithContents) { file, context, operations ->
        })
        .addAll(COMMON_RULES)
        .build()

    private static final List<Rule> WAR_FILE_RULES = ImmutableList.<Rule> builder()
        // Handle WAR files as WAR files
        .add(rule().in(WarList).withType(FileishWithContents) { file, context, operations ->
            def subContext = context.recordSubContext(file, War)
            operations.add(new ProcessZip(file, subContext))
        })
        // Handle directories as exploded WAR files
        // TODO: We could allow directories in zips, too
        .add(rule().in(WarList).withType(PhysicalDirectory) { directory, context, operations ->
            def subContext = context.recordSubContext(directory, War)
            operations.add(new ProcessDirectory(directory, subContext))
        })
        // Handle WEB-INF/lib as a runtime classpath
        .add(rule().in(War).withType(Directoryish).matching("WEB-INF/lib") { directory, context, operations ->
            def subContext = context.recordSubContext(directory, ClasspathContext)
            operations.add(new SetContext(subContext))
        })
        // Ignore empty directories in WAR files
        .add(rule().in(War).withType(Directoryish) { directory, context, operations ->
        })
        // Handle runtime classpaths as usual
        .addAll(RUNTIME_CLASSPATH_RULES)
        .build()

    @org.junit.Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()
    Snapshotter defaultSnapshotter = new Snapshotter(new NoOpPhysicalHashCache())

    def "snapshots simple files"() {
        when:
        def (hash, events, physicalSnapshots) = snapshot(DefaultContext, COMMON_RULES,
            file('firstFile.txt').setText("Some text"),
            file('secondFile.txt').setText("Second File"),
            file('missingFile.txt'),
            file('subdir').createDir()
                .file('someOtherFile.log').setText("File in subdir"),
            file('emptydir').createDir(),
        )
        then:
        events == [
            "Snapshot taken: firstFile.txt - 9db5682a4d778ca2cb79580bdb67083f",
            "Snapshot taken: secondFile.txt - 82e72efeddfca85ddb625e88af3fe973",
            "Snapshot taken: missingFile.txt - $MissingPhysicalFile.HASH",
            "Snapshot taken: someOtherFile.log - a9cca315f4b8650dccfa3d93284998ef",
            "Snapshot taken: emptydir - $Directoryish.HASH",
        ]
        hash == "8e1c2e889e3e4c0b851883e16b5383ee"
        physicalSnapshots == [
                "emptydir: $Directoryish.HASH",
                "firstFile.txt: 9db5682a4d778ca2cb79580bdb67083f",
                "missingFile.txt: $MissingPhysicalFile.HASH",
                "secondFile.txt: 82e72efeddfca85ddb625e88af3fe973",
                "someOtherFile.log: a9cca315f4b8650dccfa3d93284998ef",
        ]
    }

    def "snapshots runtime classpath files"() {
        def zipFile = file('zipContents').create {
            file('firstFile.txt').text = "Some text"
            file('secondFile.txt').text = "Second File"
            subdir {
                file('someOtherFile.log').text = "File in subdir"
            }
        }.createZip(file('library.jar'))
        def classes = file('classes').create {
            file('thirdFile.txt').text = "Third file"
            file('fourthFile.txt').text = "Fourth file"
            subdir {
                file('build.log').text = "File in subdir"
            }
        }

        when:
        def (hash, events, physicalSnapshots) = snapshot(ClasspathContext, RUNTIME_CLASSPATH_RULES, zipFile, classes)
        then:
        hash == "2bf4bccdda496564bd760f1fa35d9ab4"
        events == [
                "Snapshot taken: library.jar!firstFile.txt - 9db5682a4d778ca2cb79580bdb67083f",
                "Snapshot taken: library.jar!secondFile.txt - 82e72efeddfca85ddb625e88af3fe973",
                "Snapshot taken: library.jar!subdir/someOtherFile.log - a9cca315f4b8650dccfa3d93284998ef",
                "Snapshot taken: classes!fourthFile.txt - 6c99cb370b82c9c527320b35524213e6",
                "Snapshot taken: classes!subdir/build.log - a9cca315f4b8650dccfa3d93284998ef",
                "Snapshot taken: classes!thirdFile.txt - 3f1d3e7fb9620156f8e911fb90d89c42",
        ]
        physicalSnapshots == [
            "library.jar (''): 429be5439dc0cf3eacb9a48563f00a52",
            "fourthFile.txt: 6c99cb370b82c9c527320b35524213e6",
            "subdir/build.log: a9cca315f4b8650dccfa3d93284998ef",
            "thirdFile.txt: 3f1d3e7fb9620156f8e911fb90d89c42",
            "classes (''): $Directoryish.HASH",
        ]
    }

    def "caches physical file snapshots"() {
        def cachingSnapshotter = new Snapshotter(new SimplePhysicalHashCache())

        def zipFile = file('zipContents').create {
            file('firstFile.txt').text = "Some text"
            file('secondFile.txt').text = "Second File"
            subdir {
                file('someOtherFile.log').text = "File in subdir"
            }
        }.createZip(file('library.jar'))
        def classes = file('classes').create {
            file('thirdFile.txt').text = "Third file"
            file('fourthFile.txt').text = "Fourth file"
            subdir {
                file('build.log').text = "File in subdir"
            }
        }

        def expectedHash = "2bf4bccdda496564bd760f1fa35d9ab4"
        def expectedEventsForClassesDir = [
            "Snapshot taken: classes!fourthFile.txt - 6c99cb370b82c9c527320b35524213e6",
            "Snapshot taken: classes!subdir/build.log - a9cca315f4b8650dccfa3d93284998ef",
            "Snapshot taken: classes!thirdFile.txt - 3f1d3e7fb9620156f8e911fb90d89c42",
        ]
        def expectedPhysicalSnapshots = [
            "library.jar (''): 429be5439dc0cf3eacb9a48563f00a52",
            "fourthFile.txt: 6c99cb370b82c9c527320b35524213e6",
            "subdir/build.log: a9cca315f4b8650dccfa3d93284998ef",
            "thirdFile.txt: 3f1d3e7fb9620156f8e911fb90d89c42",
            "classes (''): $Directoryish.HASH",
        ]

        when:
        def (hash, events, physicalSnapshots) = snapshot(cachingSnapshotter, ClasspathContext, RUNTIME_CLASSPATH_RULES, zipFile, classes)
        then:
        hash == expectedHash
        events == [
            "Snapshot taken: library.jar!firstFile.txt - 9db5682a4d778ca2cb79580bdb67083f",
            "Snapshot taken: library.jar!secondFile.txt - 82e72efeddfca85ddb625e88af3fe973",
            "Snapshot taken: library.jar!subdir/someOtherFile.log - a9cca315f4b8650dccfa3d93284998ef",
            *expectedEventsForClassesDir
        ]
        physicalSnapshots == expectedPhysicalSnapshots

        when:
        def (hash2, events2, physicalSnapshots2) = snapshot(cachingSnapshotter, ClasspathContext, RUNTIME_CLASSPATH_RULES, zipFile, classes)
        then:
        hash2 == expectedHash
        events2 == [
            "Snapshot taken: library.jar - 429be5439dc0cf3eacb9a48563f00a52",
            *expectedEventsForClassesDir
        ]
        physicalSnapshots2 == expectedPhysicalSnapshots
    }

    def "can ignore file in runtime classpath"() {
        def zipFile = file('zipContents').create {
            file('firstFile.txt').text = "Some text"
            file('secondFile.txt').text = "Second File"
            subdir {
                file('someOtherFile.log').text = "File in subdir"
            }
        }.createZip(file('library.jar'))

        def rules = ImmutableList.builder()
            // Ignore *.log files inside classpath entries
            .add(rule().in(ClasspathEntryContext).withExtension("log") { file, context, operations ->
            })
            .addAll(RUNTIME_CLASSPATH_RULES)
            .build()

        when:
        def (hash, events, physicalSnapshots) = snapshot(ClasspathContext, rules, zipFile)
        then:
        hash == "2414c546f76ce381e2019fbb6ea7b988"
        events == [
                "Snapshot taken: library.jar!firstFile.txt - 9db5682a4d778ca2cb79580bdb67083f",
                "Snapshot taken: library.jar!secondFile.txt - 82e72efeddfca85ddb625e88af3fe973",
        ]
        physicalSnapshots == [
                "library.jar (''): dbd9b70c18768d3199c41efef40c73c0",
        ]
    }

    @Ignore("Error handling is not yet implemented")
    def "can snapshot JAR as raw content when cannot process file itself"() {
        def workingZipFile = file('zipContents').create {
            file('firstFile.txt').text = "Some text"
            file('secondFile.txt').text = "Second File"
            subdir {
                file('someOtherFile.log').text = "File in subdir"
            }
        }.createZip(file('library.jar'))

        def brokenZipFile = file('broken.jar')
        println "Length: ${workingZipFile.length()}"
        brokenZipFile.withOutputStream { output ->
            workingZipFile.withInputStream { input ->
                ByteStreams.copy(ByteStreams.limit(input, 120), output)
            }
        }

        when:
        def (hash, events, physicalSnapshots) = snapshot(ClasspathContext, RUNTIME_CLASSPATH_RULES, brokenZipFile)
        then:
        hash == "2414c546f76ce381e2019fbb6ea7b988"
        events == [
                "Snapshot taken: broken.jar - 9db5682a4d778ca2cb79580bdb67083f",
        ]
        physicalSnapshots == [
                "broken.jar (''): dbd9b70c18768d3199c41efef40c73c0",
        ]
    }

    def "recognizes runtime classpath inside war file"() {
        def guavaJar = file('guavaContents').create {
            "com" {
                "google" {
                    "common" {
                        "collection" {
                            file('Lists.class').text = "Lists"
                            file('Sets.class').text = "Sets"
                        }
                    }
                }
            }
            file('version.properties').text = "version=1.0"
        }.createZip(file('guava.jar'))
        def coreJar = file('coreContents').create {
            "org" {
                "gradle" {
                    file('Util.class').text = "Util"
                }
            }
        }.createZip(file('core.jar'))

        // Create WAR file like this so we can break up the runtime classpath in WEB-INF/lib
        def warFile = file("web-app.war")
        def warFileOut = new ZipOutputStream(new FileOutputStream(warFile))
        warFileOut.putNextEntry(new ZipEntry("WEB-INF/"))
        warFileOut.putNextEntry(new ZipEntry("WEB-INF/web.xml"))
        warFileOut << "<web/>".bytes
        warFileOut.putNextEntry(new ZipEntry("WEB-INF/lib/"))
        warFileOut.putNextEntry(new ZipEntry("WEB-INF/lib/guava.jar"))
        warFileOut << guavaJar.bytes
        warFileOut.putNextEntry(new ZipEntry("README.md"))
        warFileOut << "README".bytes
        warFileOut.putNextEntry(new ZipEntry("WEB-INF/lib/core.jar"))
        warFileOut << coreJar.bytes
        warFileOut.close()

        when:
        def (hash, events, physicalSnapshots) = snapshot(WarList, WAR_FILE_RULES, warFile)
        then:
        events == [
                "Snapshot taken: web-app.war!WEB-INF/web.xml - 672d3ef8a00bcece517a3fed0f06804b",
                "Snapshot taken: web-app.war!WEB-INF/lib!WEB-INF/lib/guava.jar!com/google/common/collection/Lists.class - 691d1860ec58dd973e803e209697d065",
                "Snapshot taken: web-app.war!WEB-INF/lib!WEB-INF/lib/guava.jar!com/google/common/collection/Sets.class - 86f5baf708c6c250204451eb89736947",
                "Snapshot taken: web-app.war!WEB-INF/lib!WEB-INF/lib/guava.jar!version.properties - 9a0de96b30c230abc8d5263b4c9e22a4",
                "Snapshot taken: web-app.war!README.md - c47c7c7383225ab55ff591cb59c41e6b",
                "Snapshot taken: web-app.war!WEB-INF/lib!WEB-INF/lib/core.jar!org/gradle/Util.class - 23e8a4b4f7cc1898ef12b4e6e48852bb",
        ]
        hash == "d2f44e877e9046985a76dc430cbb9fe3"
        physicalSnapshots == [
            "web-app.war: 52e44df24761891f759ffebd613f2f1c"
        ]
    }

    def "can ignore ordering, comments and commitId in properties"() {
        def rules = ImmutableList.builder()
                .add(new ProcessPropertyFile(Context.class, ['commitId'] as Set, Charsets.ISO_8859_1))
                .addAll(RUNTIME_CLASSPATH_RULES)
                .build()

        def versionProperties = file('guavaContents', 'version.properties')
        versionProperties.text = "version=1.0\ncommitId=2b5ed06"
        def guavaJar = file('guavaContents').create {
            "com" {
                "google" {
                    "common" {
                        "collection" {
                            file('Lists.class').text = "Lists"
                            file('Sets.class').text = "Sets"
                        }
                    }
                }
            }
        }.createZip(file('guava.jar'))
        def directory = file('classpathEntry').createDir()
        def versionInfo = directory.file('version-info.properties')
        versionInfo.text = """
            #Fri Apr 07 13:59:46 CEST 2017
            baseVersion=3.4
            commitId=c8824b6
            
            """.stripIndent()

        when:
        def (hash, events, physicalSnapshots) = snapshot(ClasspathContext, rules, guavaJar, directory)

        then:
        def expectedHash = "c2bb9f896e5429aa6aa318f57d98f511"
        def expectedEvents = [
                "Snapshot taken: guava.jar!com/google/common/collection/Lists.class - 691d1860ec58dd973e803e209697d065",
                "Snapshot taken: guava.jar!com/google/common/collection/Sets.class - 86f5baf708c6c250204451eb89736947",
                "Snapshot taken: guava.jar!version.properties - 5f21ccb853dfd07a63268f2d00a3a4bd",
                "Snapshot taken: classpathEntry!version-info.properties - 78705221d74193e66807b0a962508a63",
        ]
        def expectedPhysicalSnapshots = [
                "guava.jar (''): 3ebcd5148c24358c5420a8e7ec23d9ff",
                "version-info.properties: 78705221d74193e66807b0a962508a63",
                "classpathEntry (''): 28766b4be065d0c806c2e9c9d914af48"
        ]
        events == expectedEvents
        hash == expectedHash
        physicalSnapshots == expectedPhysicalSnapshots

        when:
        versionProperties.text = """
            commitId=784bcde
            version=1.0
        """.stripIndent()
        guavaJar.delete()
        file('guavaContents').createZip(guavaJar)
        versionInfo.text = """
            baseVersion=3.4
            
            #Tue Apr 04 07:32:24 CEST 2017
            commitId=ab53f34
        """.stripIndent()

        (hash, events, physicalSnapshots) = snapshot(ClasspathContext, rules, guavaJar, directory)

        then:
        hash == expectedHash
        events == expectedEvents
        physicalSnapshots == expectedPhysicalSnapshots
    }

    private def snapshot(Snapshotter snapshotter = defaultSnapshotter, Class<? extends Context> contextType, Iterable<Rule<? extends Fileish, ? extends Context>> rules, File... files) {
        List<String> events = []
        ImmutableCollection.Builder<PhysicalSnapshot> physicalSnapshots = ImmutableList.builder()
        def context = new RecordingContextWrapper(null, events, contextType.newInstance())
        def hash = snapshotter.snapshot(files as List, context, rules, physicalSnapshots)
        return [hash.toString(), events, physicalSnapshots.build()*.toString()]
    }

    private static class RecordingContextWrapper implements Context {
        List<String> events
        String path
        Context delegate
        Map<Context, RecordingContextWrapper> wrappers = [:]

        RecordingContextWrapper() {
        }

        RecordingContextWrapper(String path, List<String> events, Context delegate) {
            this.events = events
            this.path = path
            this.delegate = delegate
        }

        @Override
        void recordSnapshot(Fileish file, HashCode hash) {
            report("Snapshot taken", file.relativePath, hash)
            delegate.recordSnapshot(file, hash)
        }

        private void report(String type, String filePath, HashCode hash) {
            def event = "$type: ${getFullPath(filePath)} - $hash"
            events.add(event)
            println event
        }

        private String getFullPath(String filePath) {
            return path ? "$path!$filePath" : filePath
        }

        @Override
        <C extends Context> C recordSubContext(Fileish file, Class<C> type) {
            def subContext = delegate.recordSubContext(file, type)
            def wrapper = wrappers.get(subContext)
            if (wrapper == null) {
                wrapper = new RecordingContextWrapper(getFullPath(file.relativePath), events, subContext)
                wrappers.put(subContext, wrapper)
            }
            return (C) wrapper
        }

        @Override
        HashCode fold(PhysicalSnapshotCollector physicalSnapshots) {
            return delegate.fold(physicalSnapshots)
        }

        @Override
        Class<? extends Context> getType() {
            return delegate.getType()
        }
    }

    TestFile file(Object... path) {
        return new TestFile(temporaryFolder.root, path)
    }
}
