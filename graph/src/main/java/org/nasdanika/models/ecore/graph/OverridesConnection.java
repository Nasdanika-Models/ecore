package org.nasdanika.models.ecore.graph;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.nasdanika.graph.Element;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * Connection between tow {@link EOperationNode} nodes - from the overriding operation is subclass to the overridden in superclass. 
 * @author Pavel
 *
 */
public class OverridesConnection implements org.nasdanika.graph.Connection {
	
	private EObjectNode source;
	private EObjectNode target;

	/**
	 * @param source
	 * @param target
	 */
	protected OverridesConnection(EObjectNode source, EObjectNode target) {
		this.source = source;
		this.target = target;
		source.addOutgoingConnection(this);
		target.addIncomingConnection(this);
	}

	@Override
	public EObjectNode getSource() {
		return source;
	}

	@Override
	public EObjectNode getTarget() {
		return target;
	}
	
	@Override
	public <T> T accept(BiFunction<? super Element, Map<? extends Element, T>, T> visitor) {
		return visitor.apply(this, Collections.emptyMap());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(), getTarget());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OverridesConnection other = (OverridesConnection) obj;
		return Objects.equals(getSource(), other.getSource()) && Objects.equals(getTarget(), other.getTarget());
	}
	
}
