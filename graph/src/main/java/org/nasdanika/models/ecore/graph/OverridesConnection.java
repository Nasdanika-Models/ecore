package org.nasdanika.models.ecore.graph;

import org.nasdanika.graph.emf.Connection;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * Connection between tow {@link EOperationNode} nodes - from the overriding operation is subclass to the overridden in superclass. 
 * @author Pavel
 *
 */
public class OverridesConnection extends Connection {

	protected OverridesConnection(EObjectNode source, EObjectNode target) {
		super(source, target);
	}
	
}
