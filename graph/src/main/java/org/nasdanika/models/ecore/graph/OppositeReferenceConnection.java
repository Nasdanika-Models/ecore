package org.nasdanika.models.ecore.graph;

import org.eclipse.emf.ecore.EReference;
import org.nasdanika.graph.emf.Connection;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.ncore.util.NcoreUtil;

/**
 * Connection between two opposite {@link EReference} {@link EObjectNode}s. 
 * Opposite connection is computed by {@link NcoreUtil}.getOpposite()   
 * @author Pavel
 *
 */
public class OppositeReferenceConnection extends Connection {

	protected OppositeReferenceConnection(EObjectNode source, EObjectNode target) {
		super(source, target, false);
	}
	
}
