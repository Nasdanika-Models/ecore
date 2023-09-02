package org.nasdanika.models.ecore.graph;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.emf.EObjectNode;

public class EClassNode extends EObjectNode {

	public EClassNode(
			EClass target,
			boolean parallel,
			BiConsumer<EObject, BiConsumer<Element,ProgressMonitor>> elementProvider, 
			Consumer<BiConsumer<Map<EObject, Element>,ProgressMonitor>> registry,
			EcoreGraphFactory factory,
			ProgressMonitor progressMonitor) {
		super(target, parallel, elementProvider, registry, factory, progressMonitor);

		for (EOperation eOperation: target.getEAllOperations()) {
			factory.createReifiedTypeConnection(this, eOperation.getEGenericType(), elementProvider, progressMonitor);
			for (EParameter eParameter: eOperation.getEParameters()) {
				factory.createReifiedTypeConnection(this, eParameter.getEGenericType(), elementProvider, progressMonitor);
			}
			for (EGenericType ge: eOperation.getEGenericExceptions()) {
				factory.createReifiedTypeConnection(this, ge, elementProvider, progressMonitor);
			}
		}	
		for (EGenericType gst: target.getEAllGenericSuperTypes()) {
			factory.createReifiedTypeConnection(this, gst, elementProvider, progressMonitor);
		}
		for (EStructuralFeature sf: target.getEAllStructuralFeatures()) {
			factory.createReifiedTypeConnection(this, sf.getEGenericType(), elementProvider, progressMonitor);
		}
	}

	@Override
	public EClass get() {
		return (EClass) super.get();
	}	
	
}
