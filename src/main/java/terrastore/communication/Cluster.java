package terrastore.communication;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Cluster interface, representing an actual cluster in the ensemble.
 * <br><br>
 * Every cluster is actually comprised of a set of Terrastore server nodes<br>
 * All clusters makes up an ensemble.
 *
 * @author Sergio Bossa
 */
public class Cluster {

    private final String name;
    private final boolean local;

    public Cluster(String name, boolean local) {
        this.name = name;
        this.local = local;
    }

    public String getName() {
        return name;
    }

    public boolean isLocal() {
        return local;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cluster) {
            Cluster other = (Cluster) obj;
            return new EqualsBuilder().append(this.name, other.name).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).toHashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}