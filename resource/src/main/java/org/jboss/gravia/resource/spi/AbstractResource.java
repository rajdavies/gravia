package org.jboss.gravia.resource.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;

/**
 * An abstract implementation of a {@link Resource}
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
public class AbstractResource implements Resource {

    private final List<AbstractCapability> capabilities = new ArrayList<AbstractCapability>();
    private final List<AbstractRequirement> requirements = new ArrayList<AbstractRequirement>();
    private final AtomicBoolean mutable = new AtomicBoolean(true);
    private Capability identityCapability;
    private ResourceIdentity identity;

    void addCapability(AbstractCapability cap) {
        synchronized (capabilities) {
            assertMutable();
            capabilities.add(cap);
        }
    }
    
    void addRequirement(AbstractRequirement req) {
        synchronized (requirements) {
            assertMutable();
            requirements.add(req);
        }
    }
    
    @Override
    public Capability getIdentityCapability() {
        if (identityCapability == null) {
            List<Capability> icaps = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
            if (icaps.size() > 1)
                throw new IllegalStateException("Multiple identity capabilities");
            if (icaps.size() < 1)
                throw new IllegalStateException("No identity capability");
            Capability icap = icaps.get(0);
            Object version = icap.getAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            if (!(version instanceof Version)) {
                version = version == null ? Version.emptyVersion : Version.parseVersion(version.toString());
                icap.getAttributes().put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
            }
            identityCapability = icap;
        }
        return identityCapability;
    }

    @Override
    public ResourceIdentity getIdentity() {
        if (identity == null) {
            Capability icap = getIdentityCapability();
            String symbolicName = (String) icap.getAttribute(IdentityNamespace.IDENTITY_NAMESPACE);
            Version version = (Version) icap.getAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            identity = ResourceIdentity.create(symbolicName, version);
        }
        return identity;
    }

    protected void setMutable(boolean flag) {
        mutable.set(flag);
    }

    protected boolean isMutable() {
        return mutable.get();
    }

    void assertMutable() {
        if (!isMutable())
            throw new IllegalStateException("Invalid access to immutable resource");
    }
    
    void assertImmutable() {
        if (isMutable())
            throw new IllegalStateException("Invalid access to mutable resource");
    }

    @Override
    public List<Capability> getCapabilities(String namespace) {
        List<Capability> result = new ArrayList<Capability>();
        synchronized (capabilities) {
            for (Capability cap : capabilities) {
                if (namespace == null || namespace.equals(cap.getNamespace())) {
                    result.add(cap);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Requirement> getRequirements(String namespace) {
        List<Requirement> result = new ArrayList<Requirement>();
        synchronized (requirements) {
            for (Requirement req : requirements) {
                if (namespace == null || namespace.equals(req.getNamespace())) {
                    result.add(req);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    void validate() {

        // Make sure we have an identity
        getIdentity();

        // Validate the capabilities
        for (Capability cap : getCapabilities(null)) {
            ((AbstractCapability) cap).validate();
        }

        // Validate the requirements
        for (Requirement req : getRequirements(null)) {
            ((AbstractRequirement) req).validate();
        }
    }

    @Override
    public String toString() {
        ResourceIdentity id = identity;
        String idstr = (id != null ? id.getSymbolicName() + ":" + id.getVersion() : "anonymous");
        String prefix = getClass() != AbstractResource.class ? getClass().getSimpleName() : Resource.class.getSimpleName();
        return prefix + "[" + idstr + "]";
    }
}
