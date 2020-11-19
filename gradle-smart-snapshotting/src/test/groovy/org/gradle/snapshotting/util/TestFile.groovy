package org.gradle.snapshotting.util

import org.codehaus.groovy.runtime.ResourceGroovyMethods

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static org.junit.Assert.assertTrue

class TestFile extends File {
    TestFile(File file, Object... path) {
        super(join(file, path).getAbsolutePath())
    }

    TestFile file(Object... path) {
        try {
            return new TestFile(this, path)
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Could not locate file '%s' relative to '%s'.", Arrays.toString(path), this), e)
        }
    }

    List<TestFile> files(Object... paths) {
        List<TestFile> files = new ArrayList<>()
        for (Object path : paths) {
            files.add(file(path))
        }
        return files
    }

    TestFile leftShift(Object content) {
        getParentFile().mkdirs()
        try {
            ResourceGroovyMethods.leftShift(this, content)
            return this
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not append to test file '%s'", this), e)
        }
    }

    TestFile setText(String content) {
        getParentFile().mkdirs()
        try {
            ResourceGroovyMethods.setText(this, content)
            return this
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not append to test file '%s'", this), e)
        }
    }

    TestFile createFile() {
        new TestFile(getParentFile()).createDir()
        try {
            assertTrue(isFile() || createNewFile())
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
        return this
    }

    TestFile createFile(Object path) {
        return file(path).createFile()
    }

    TestFile createDir() {
        if (mkdirs()) {
            return this
        }
        if (isDirectory()) {
            return this
        }
        throw new AssertionError("Problems creating dir: " + this
                + ". Diagnostics: exists=" + this.exists() + ", isFile=" + this.isFile() + ", isDirectory=" + this.isDirectory())
    }

    TestFile createDir(Object path) {
        return new TestFile(this, path).createDir()
    }

    TestFile deleteDir() {
        super.deleteDir()
        return this
    }

    TestFile createZip(Object path) throws FileNotFoundException {
        createZip(file(path))
    }

    TestFile createZip(TestFile zipFile) throws FileNotFoundException {
        new ZipOutputStream(new FileOutputStream(zipFile)).withStream { zipOutputStream ->
            this.eachFileRecurse() { file ->
                zipOutputStream.putNextEntry(zipEntry(this.toPath().relativize(file.toPath()).toString(), file))
                if (!file.isDirectory()) {
                    Files.copy(file.toPath(), zipOutputStream)
                }
                zipOutputStream.closeEntry()
            }
        }
        return zipFile
    }

    TestFile makeOlder() {
        lastModified = lastModified() - 2000
        return this
    }

    private static ZipEntry zipEntry(String path, File file) {
        def entry = new ZipEntry(path + (file.isDirectory() ? '/' : ''))
        entry.setTime(file.lastModified())
        return entry
    }

    /**
     * Creates a directory structure specified by the given closure.
     * <pre>
     * dir.create {
     *     subdir1 {
     *        file 'somefile.txt'
     *     }
     *     subdir2 { nested { file 'someFile' } }
     * }
     * </pre>
     */
    TestFile create(@DelegatesTo(TestWorkspaceBuilder.class) Closure structure) {
        assertTrue(isDirectory() || mkdirs())
        new TestWorkspaceBuilder(this).apply(structure)
        return this
    }

    private static File join(File file, Object[] path) {
        File current = file.getAbsoluteFile()
        for (Object p : path) {
            current = new File(current, p.toString())
        }
        try {
            return current.getCanonicalFile()
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not canonicalise '%s'.", current), e)
        }
    }
}
