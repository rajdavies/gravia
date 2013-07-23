package org.jboss.test.gravia.repository;
/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
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
 * #L%
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.jboss.gravia.repository.ContentCapability;
import org.jboss.gravia.repository.ContentNamespace;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.Repository.ConfigurationPropertyProvider;
import org.jboss.gravia.repository.RepositoryContent;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.repository.spi.FileBasedRepositoryStorage;
import org.jboss.gravia.repository.spi.RepositoryContentHelper;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.DefaultRequirementBuilder;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test the {@link FileBasedRepositoryStorage}
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
@Ignore
public class FileBasedRepositoryStorageTestCase extends AbstractRepositoryTest {

    private File storageDir;
    private Repository repository;
    private RepositoryStorage storage;
    private File bundleAjar;
    private File bundleAtxt;

    @Before
    public void setUp() throws Exception {
        storageDir = new File("./target/repository/" + System.currentTimeMillis()).getCanonicalFile();
        repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getName()).thenReturn("MockedRepo");
        storage = new FileBasedRepositoryStorage(repository, storageDir, Mockito.mock(ConfigurationPropertyProvider.class));

        // Write the bundle to the location referenced by repository-testA.xml
        bundleAjar = new File("./target/bundleA.jar");
        getBundleA().as(ZipExporter.class).exportTo(bundleAjar, true);

        // Write some text to the location referenced by repository-testB.xml
        bundleAtxt = new File("./target/bundleA.txt");
        PrintWriter bw = new PrintWriter(new FileWriter(bundleAtxt));
        bw.print("some text");
        bw.close();
    }

    @After
    public void tearDown() {
        deleteRecursive(storageDir);
        bundleAjar.delete();
        bundleAtxt.delete();
    }

    @Test
    public void testAddResourceFromXML() throws Exception {
        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        Resource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);
    }

    @Test
    public void testAddResourceWithMultipleContent() throws Exception {
        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testB.xml");
        storage.addResource(reader.nextResource());

        Requirement req = new DefaultRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, "bundleA").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        Capability cap = (Capability) providers.iterator().next();
        Assert.assertEquals("bundleA", cap.getAttribute(IdentityNamespace.IDENTITY_NAMESPACE));

        Resource resource = cap.getResource();
        verifyDefaultContent(resource);

        InputStream input = ((RepositoryContent)resource).getContent();
        String digest = RepositoryContentHelper.getDigest(input);
        Assert.assertNotNull("RepositoryContent not null", input);
        input.close();

        List<Capability> ccaps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        Assert.assertEquals(2, ccaps.size());
        ContentCapability ccap = ((Capability) ccaps.get(0)).adapt(ContentCapability.class);
        Assert.assertEquals(digest, ccap.getDigest());
        Assert.assertEquals("application/vnd.osgi.bundle", ccap.getMimeType());
        Assert.assertEquals(new Long(400), ccap.getSize());
        File contentFile = new File(new URL(ccap.getContentURL()).getPath()).getCanonicalFile();
        Assert.assertTrue("File exists: " + contentFile, contentFile.exists());
        Assert.assertTrue("Path starts with: " + storageDir.getPath(), contentFile.getPath().startsWith(storageDir.getPath()));

        ccap = ((Capability) ccaps.get(1)).adapt(ContentCapability.class);
        Assert.assertFalse(digest.equals(ccap.getDigest()));
        Assert.assertEquals("text/plain", ccap.getMimeType());
        Assert.assertEquals(new Long("[bundleA:0.0.0]".length()), ccap.getSize());
        contentFile = new File(new URL(ccap.getContentURL()).getPath()).getCanonicalFile();
        Assert.assertTrue("File exists: " + contentFile, contentFile.exists());
        Assert.assertTrue("Path starts with: " + storageDir.getPath(), contentFile.getPath().startsWith(storageDir.getPath()));

        BufferedReader br = new BufferedReader(new FileReader(contentFile));
        Assert.assertEquals("[bundleA:0.0.0]", br.readLine());
        br.close();
    }

    @Test
    public void testFileStorageRestart() throws Exception {

        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        Resource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);

        RepositoryStorage other = new FileBasedRepositoryStorage(repository, storageDir, Mockito.mock(ConfigurationPropertyProvider.class));
        verifyProviders(other);
    }

    @Test
    public void testCustomNamespace() throws Exception {

        // Add a resource from XML
        RepositoryReader reader = getRepositoryReader("xml/repository-testA.xml");
        Resource resource = storage.addResource(reader.nextResource());

        verifyResource(resource);
        verifyProviders(storage);

        List<Capability> allcaps = resource.getCapabilities(null);
        Assert.assertEquals("Six capabilities", 6, allcaps.size());

        Requirement req = new DefaultRequirementBuilder("custom.namespace", "custom.value").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertEquals("One provider", 1, providers.size());

        req = new DefaultRequirementBuilder("custom.namespace", "bogus").getRequirement();
        providers = storage.findProviders(req);
        Assert.assertEquals("No provider", 0, providers.size());
    }

    private void verifyResource(Resource resource) throws Exception {
        verifyDefaultContent(resource);
        Assert.assertEquals(6, resource.getCapabilities(null).size());
    }

    private void verifyDefaultContent(Resource resource) throws Exception {
        InputStream input = ((RepositoryContent)resource).getContent();
        String digest = RepositoryContentHelper.getDigest(input);
        Assert.assertNotNull("RepositoryContent not null", input);
        input.close();

        Capability cap = (Capability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
        ContentCapability ccap = cap.adapt(ContentCapability.class);
        Assert.assertEquals(digest, ccap.getDigest());
        Assert.assertEquals(digest, cap.getAttribute(ContentNamespace.CONTENT_NAMESPACE));
        Assert.assertEquals("application/vnd.osgi.bundle", ccap.getMimeType());
        Assert.assertEquals("application/vnd.osgi.bundle", cap.getAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE));
        Assert.assertEquals(new Long(400), ccap.getSize());
        Assert.assertEquals(new Long(400), cap.getAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
        String contentURL = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        File contentFile = new File(new URL(contentURL).getPath()).getCanonicalFile();
        Assert.assertTrue("File exists: " + contentFile, contentFile.exists());
        Assert.assertTrue("Path starts with: " + storageDir.getPath(), contentFile.getPath().startsWith(storageDir.getPath()));
    }

    private void verifyProviders(RepositoryStorage storage) throws Exception {
        /*
        Requirement req = RequirementBuilder.create(PACKAGE_NAMESPACE, "org.acme.foo").getRequirement();
        Collection<Capability> providers = storage.findProviders(req);
        Assert.assertNotNull(providers);
        Assert.assertEquals(1, providers.size());

        Capability cap = (Capability) providers.iterator().next();
        XPackageCapability pcap = cap.adapt(XPackageCapability.class);
        Assert.assertNotNull(pcap);
        Assert.assertEquals("org.acme.foo", pcap.getPackageName());

        verifyResource(pcap.getResource());
        */
    }

    private JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bundleA");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability("org.acme.foo", "1.0.0");
                builder.addIdentityRequirement("org.acme.foo", "[1.0,2.0)");
                builder.addGenericCapabilities("custom.namespace;custom.namespace=custom.value");
                return builder.openStream();
            }
        });
        return archive;
    }
}