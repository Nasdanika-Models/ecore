package org.nasdanika.models.ecore.graph;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

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
			Function<EObject, CompletionStage<Element>> elementProvider, 
			Consumer<CompletionStage<?>> stageConsumer,
			CompletionStage<Map<EObject, Element>> registry,
			EcoreGraphFactory factory,
			ProgressMonitor progressMonitor) {
		super(target, parallel, elementProvider, stageConsumer, registry, factory, progressMonitor);

		for (EOperation eOperation: target.getEAllOperations()) {
			factory.createReifiedTypeConnection(this, eOperation.getEGenericType(), elementProvider, stageConsumer, progressMonitor);
			for (EParameter eParameter: eOperation.getEParameters()) {
				factory.createReifiedTypeConnection(this, eParameter.getEGenericType(), elementProvider, stageConsumer, progressMonitor);
			}
			for (EGenericType ge: eOperation.getEGenericExceptions()) {
				factory.createReifiedTypeConnection(this, ge, elementProvider, stageConsumer, progressMonitor);
			}
		}	
		for (EGenericType gst: target.getEAllGenericSuperTypes()) {
			factory.createReifiedTypeConnection(this, gst, elementProvider, stageConsumer, progressMonitor);
		}
		for (EStructuralFeature sf: target.getEAllStructuralFeatures()) {
			factory.createReifiedTypeConnection(this, sf.getEGenericType(), elementProvider, stageConsumer, progressMonitor);
		}
	}

	@Override
	public EClass getTarget() {
		return (EClass) super.getTarget();
	}	
	
}
