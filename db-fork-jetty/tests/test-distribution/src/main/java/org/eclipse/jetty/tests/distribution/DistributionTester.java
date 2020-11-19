//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.tests.distribution;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * <p>Helper class to test the Jetty Distribution</p>.
 * <p>API can change without any further notice.</p>
 * <p>Usage:</p>
 * <pre>
 * // Create the distribution.
 * String jettyVersion = "9.4.14.v20181114";
 * DistributionTester distribution = DistributionTester.Builder.newInstance()
 *         .jettyVersion(jettyVersion)
 *         .jettyBase(Paths.get("demo-base"))
 *         .build();
 *
 * // The first run initializes the Jetty Base.
 * try (DistributionTester.Run run1 = distribution.start("--create-startd", "--add-to-start=http2c,jsp,deploy"))
 * {
 *     assertTrue(run1.awaitFor(5, TimeUnit.SECONDS));
 *     assertEquals(0, run1.getExitValue());
 *
 *     // Install a web application.
 *     File war = distribution.resolveArtifact("org.eclipse.jetty.tests:test-simple-webapp:war:" + jettyVersion);
 *     distribution.installWarFile(war, "test");
 *
 *     // The second run starts the distribution.
 *     int port = 9090;
 *     try (DistributionTester.Run run = distribution.start("jetty.http.port=" + port))
 *     {
 *         // Wait for Jetty to be fully started.
 *         assertTrue(run1.awaitConsoleLogsFor("Started @", 20, TimeUnit.SECONDS));
 *
 *         // Make a HTTP request to the web application.
 *         HttpClient client = new HttpClient();
 *         client.start();
 *         ContentResponse response = client.GET("http://localhost:" + port + "/test/index.jsp");
 *         assertEquals(HttpStatus.OK_200, response.getStatus());
 *     }
 * }
 * </pre>
 */
public class DistributionTester
{
    private static final Logger LOGGER = Log.getLogger(DistributionTester.class);

    private Config config;

    private DistributionTester(Config config)
    {
        this.config = config;
    }

    /**
     * Starts the distribution with the given arguments
     *
     * @param args arguments to use to start the distribution
     */
    public DistributionTester.Run start(String... args) throws Exception
    {
        return start(Arrays.asList(args));
    }

    /**
     * Start the distribution with the arguments
     *
     * @param args arguments to use to start the distribution
     */
    public DistributionTester.Run start(List<String> args) throws Exception
    {
        File jettyBaseDir = config.jettyBase.toFile();
        Path workDir = Files.createDirectories(jettyBaseDir.toPath().resolve("work"));

        List<String> commands = new ArrayList<>();
        commands.add(getJavaExecutable());
        commands.add("-Djava.io.tmpdir=" + workDir.toAbsolutePath().toString());
        commands.add("-jar");
        commands.add(config.jettyHome.toAbsolutePath() + "/start.jar");
        commands.addAll(args);

        LOGGER.info("Executing: {}", commands);
        LOGGER.info("Working Dir: {}", jettyBaseDir.getAbsolutePath());

        ProcessBuilder pbCmd = new ProcessBuilder(commands);
        pbCmd.directory(jettyBaseDir);
        Process process = pbCmd.start();

        return new Run(process);
    }

    /**
     * @return a free port chosen by the OS that can be used to listen to
     * @throws IOException if a free port is not available
     */
    public int freePort() throws IOException
    {
        try (ServerSocket server = new ServerSocket())
        {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress("localhost", 0));
            return server.getLocalPort();
        }
    }

    /**
     * Installs in {@code ${jetty.base}/webapps} the given war file under the given context path.
     *
     * @param warFile the war file to install
     * @param context the context path
     * @throws IOException if the installation fails
     */
    public void installWarFile(File warFile, String context) throws IOException
    {
        //webapps
        Path webapps = Paths.get(config.jettyBase.toString(), "webapps", context);
        if (!Files.exists(webapps))
            Files.createDirectories(webapps);
        unzip(warFile, webapps.toFile());
    }

    /**
     * Resolves an artifact given its Maven coordinates.
     *
     * @param coordinates &lt;groupId>:&lt;artifactId>[:&lt;extension>[:&lt;classifier>]]:&lt;version>
     * @return the artifact
     * @see #installWarFile(File, String)
     */
    public File resolveArtifact(String coordinates) throws ArtifactResolutionException
    {
        RepositorySystem repositorySystem = newRepositorySystem();

        Artifact artifact = new DefaultArtifact(coordinates);

        RepositorySystemSession session = newRepositorySystemSession(repositorySystem);

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(newRepositories());
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);

        artifact = artifactResult.getArtifact();
        return artifact.getFile();
    }

    private void init() throws Exception
    {
        if (config.jettyHome == null)
            config.jettyHome = resolveDistribution(config.jettyVersion);

        if (config.jettyBase == null)
        {
            config.jettyBase = Files.createTempDirectory("jetty_base_");
        }
        else
        {
            if (!config.jettyBase.isAbsolute())
                config.jettyBase = config.jettyHome.resolve(config.jettyBase);
        }
    }

    private String getJavaExecutable()
    {
        String[] javaExecutables = new String[]{"java", "java.exe"};
        File javaHomeDir = new File(System.getProperty("java.home"));
        for (String javaExecutable : javaExecutables)
        {
            File javaFile = new File(javaHomeDir, "bin" + File.separator + javaExecutable);
            if (javaFile.exists() && javaFile.isFile())
                return javaFile.getAbsolutePath();
        }
        return "java";
    }

    private void unzip(File zipFile, File output) throws IOException
    {
        try (InputStream fileInputStream = Files.newInputStream(zipFile.toPath());
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream))
        {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null)
            {
                if (entry.isDirectory())
                {
                    File dir = new File(output, entry.getName());
                    if (!Files.exists(dir.toPath()))
                    {
                        Files.createDirectories(dir.toPath());
                    }
                }
                else
                {
                    // Read zipEntry and write to a file.
                    File file = new File(output, entry.getName());
                    if (!Files.exists(file.getParentFile().toPath()))
                    {
                        Files.createDirectories(file.getParentFile().toPath());
                    }
                    try (OutputStream outputStream = Files.newOutputStream(file.toPath()))
                    {
                        IOUtil.copy(zipInputStream, outputStream);
                    }
                }
                // Get next entry
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private Path resolveDistribution(String version) throws Exception
    {
        File artifactFile = resolveArtifact("org.eclipse.jetty:jetty-distribution:zip:" + version);

        // create tmp directory to unzip distribution
        Path tmp = Files.createTempDirectory("jetty_home_");
        File tmpFile = tmp.toFile();

        unzip(artifactFile, tmpFile);

        return tmp.resolve("jetty-distribution-" + version);
    }

    private RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception)
            {
                LOGGER.warn("Service creation failed for {} implementation {}: {}",
                        type, impl, exception.getMessage(), exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    private List<RemoteRepository> newRepositories()
    {
        List<RemoteRepository> remoteRepositories = new ArrayList<>(config.mavenRemoteRepositories.size() + 1);
        config.mavenRemoteRepositories.forEach((key, value) -> remoteRepositories.add(new RemoteRepository.Builder(key, "default", value).build()));
        remoteRepositories.add(newCentralRepository());
        return remoteRepositories;
    }

    private static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    }

    private DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system)
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(config.mavenLocalRepository);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        session.setTransferListener(new LogTransferListener());
        session.setRepositoryListener(new LogRepositoryListener());

        return session;
    }

    private static class Config
    {
        private Path jettyBase;
        private Path jettyHome;
        private String jettyVersion;
        private String mavenLocalRepository = System.getProperty("user.home") + "/.m2/repository";
        private Map<String, String> mavenRemoteRepositories = new HashMap<>();

        @Override
        public String toString()
        {
            return String.format("%s@%x{jettyBase=%s, jettyHome=%s, jettyVersion=%s, mavenLocalRepository=%s, mavenRemoteRepositories=%s}",
                    getClass().getSimpleName(),
                    hashCode(),
                    jettyBase,
                    jettyHome,
                    jettyVersion,
                    mavenLocalRepository,
                    mavenRemoteRepositories);
        }
    }

    private static class LogTransferListener extends AbstractTransferListener
    {
        // no op
    }

    private static class LogRepositoryListener extends AbstractRepositoryListener
    {
        @Override
        public void artifactDownloaded(RepositoryEvent event)
        {
            LOGGER.debug("distribution downloaded to {}", event.getFile());
        }

        @Override
        public void artifactResolved(RepositoryEvent event)
        {
            LOGGER.debug("distribution resolved to {}", event.getFile());
        }
    }

    /**
     * A distribution run wraps the process that started the Jetty distribution.
     */
    public static class Run implements Closeable
    {
        private final Process process;
        private final List<ConsoleStreamer> consoleStreamers = new ArrayList<>();
        private final Queue<String> logs = new ConcurrentLinkedQueue<>();

        private Run(Process process)
        {
            this.process = process;
            consoleStreamers.add(startPump("STDOUT", process.getInputStream()));
            consoleStreamers.add(startPump("STDERR", process.getErrorStream()));
        }

        private ConsoleStreamer startPump(String mode, InputStream stream)
        {
            ConsoleStreamer pump = new ConsoleStreamer(stream);
            Thread thread = new Thread(pump, "ConsoleStreamer/" + mode);
            thread.start();
            return pump;
        }

        /**
         * Waits for the given time for the distribution process to stop.
         *
         * @param time the time to wait
         * @param unit the unit of time
         * @return true if the distribution process is terminated, false if the timeout elapsed
         * @throws InterruptedException if the wait is interrupted
         */
        public boolean awaitFor(long time, TimeUnit unit) throws InterruptedException
        {
            boolean result = process.waitFor(time, unit);
            if (result)
                stopConsoleStreamers();
            return result;
        }

        /**
         * @return the distribution process exit value
         * @throws IllegalThreadStateException if the distribution process is not terminated yet
         */
        public int getExitValue() throws IllegalThreadStateException
        {
            return process.exitValue();
        }

        /**
         * Stops the distribution process.
         *
         * @see #awaitFor(long, TimeUnit)
         */
        public void stop()
        {
            process.destroy();
            stopConsoleStreamers();
        }

        /**
         * Forcibly destroys the distribution process.
         */
        public void destroy()
        {
            process.destroyForcibly();
            stopConsoleStreamers();
        }

        private void stopConsoleStreamers()
        {
            consoleStreamers.forEach(ConsoleStreamer::stop);
        }

        /**
         * @see #destroy()
         */
        @Override
        public void close()
        {
            destroy();
        }

        /**
         * Awaits the console logs to contain the given text, for the given amount of time.
         *
         * @param txt the text that must be present in the console logs
         * @param time the time to wait
         * @param unit the unit of time
         * @return true if the text was found, false if the timeout elapsed
         * @throws InterruptedException if the wait is interrupted
         */
        public boolean awaitConsoleLogsFor(String txt, long time, TimeUnit unit) throws InterruptedException
        {
            long end = System.nanoTime() + unit.toNanos(time);
            while (System.nanoTime() < end)
            {
                boolean result = logs.stream().anyMatch(s -> s.contains(txt));
                if (result)
                    return true;
                Thread.sleep(250);
            }
            return false;
        }

        /**
         * Simple streamer for the console output from a Process
         */
        private class ConsoleStreamer implements Runnable
        {
            private final BufferedReader reader;
            private volatile boolean stop;

            public ConsoleStreamer(InputStream stream)
            {
                this.reader = new BufferedReader(new InputStreamReader(stream));
            }

            @Override
            public void run()
            {
                try
                {
                    String line;
                    while ((line = reader.readLine()) != null && !stop)
                    {
                        LOGGER.info("{}", line);
                        logs.add(line);
                    }
                }
                catch (IOException ignore)
                {
                    // ignore
                }
                finally
                {
                    IO.close(reader);
                }
            }

            public void stop()
            {
                stop = true;
                IO.close(reader);
            }
        }
    }

    public static class Builder
    {
        private Config config = new Config();

        private Builder()
        {
        }

        /**
         * @param jettyVersion the version to use (format: 9.4.14.v20181114 9.4.15-SNAPSHOT).
         *                     The distribution will be downloaded from local repository or remote
         * @return the {@link Builder}
         */
        public Builder jettyVersion(String jettyVersion)
        {
            config.jettyVersion = jettyVersion;
            return this;
        }

        /**
         * @param jettyHome Path to the local exploded jetty distribution
         *                  if configured the jettyVersion parameter will not be used
         * @return the {@link Builder}
         */
        public Builder jettyHome(Path jettyHome)
        {
            config.jettyHome = jettyHome;
            return this;
        }

        /**
         * <p>Sets the path for the Jetty Base directory.</p>
         * <p>If the path is relative, it will be resolved against the Jetty Home directory.</p>
         *
         * @param jettyBase Path to the local Jetty Base directory
         * @return the {@link Builder}
         */
        public Builder jettyBase(Path jettyBase)
        {
            config.jettyBase = jettyBase;
            return this;
        }

        /**
         * @param mavenLocalRepository Path to the local maven repository
         * @return the {@link Builder}
         */
        public Builder mavenLocalRepository(String mavenLocalRepository)
        {
            config.mavenLocalRepository = mavenLocalRepository;
            return this;
        }

        /**
         * If needed to resolve the Jetty distribution from another Maven remote repositories
         *
         * @param id  the id
         * @param url the Maven remote repository url
         * @return the {@link Builder}
         */
        public Builder addRemoteRepository(String id, String url)
        {
            config.mavenRemoteRepositories.put(id, url);
            return this;
        }

        /**
         * @return an empty instance of {@link Builder}
         */
        public static Builder newInstance()
        {
            return new Builder();
        }

        /**
         * @return a new configured instance of {@link DistributionTester}
         */
        public DistributionTester build() throws Exception
        {
            DistributionTester tester = new DistributionTester(config);
            tester.init();
            return tester;
        }
    }
}
