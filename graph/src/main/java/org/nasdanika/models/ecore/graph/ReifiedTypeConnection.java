package org.nasdanika.models.ecore.graph;

import org.eclipse.emf.ecore.EGenericType;
import org.nasdanika.graph.emf.Connection;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * Connection from an {@link EClassNode} to a generic type qualified by a typed element for which its type is reified in the context of the source EClass.
 * 
 * @author Pavel
 *
 */
public class ReifiedTypeConnection extends Connection {

	private EGenericType genericType;
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param eReference
	 * @param index -1 for single references.
	 */
	ReifiedTypeConnection(EClassNode source, EObjectNode target, EGenericType genericType) {
		super(source, target, false);
		this.genericType = genericType;
	}

	public EGenericType getGenericType() {
		return genericType;
	}
	
	@Override
	public String toString() {
		return super.toString() + " " + genericType;
	}

}
