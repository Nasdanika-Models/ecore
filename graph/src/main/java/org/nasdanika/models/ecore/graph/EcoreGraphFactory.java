package org.nasdanika.models.ecore.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.emf.EObjectGraphFactory;
import org.nasdanika.graph.emf.EObjectNode;

/**
 * A factory for Ecore model object. Reflective target for {@link EObjectGraphFactory}.
 * Graph factory for ecore models
 * @author Pavel
 *
 */
public class EcoreGraphFactory extends EObjectGraphFactory {
	
	public void createReifiedTypeConnection(
			EClassNode source, 
			EGenericType genericType,
			Function<EObject, CompletionStage<Element>> elementProvider, 
			Consumer<CompletionStage<?>> stageConsumer,
			ProgressMonitor progressMonitor) {
		
		EGenericType reifiedType = EcoreUtil.getReifiedType(source.get(), genericType);
		if (reifiedType != null && !Objects.equals(reifiedType, genericType)) {
			stageConsumer.accept(elementProvider.apply(reifiedType).thenAccept(reifiedTypeNode -> new ReifiedTypeConnection(source, (EObjectNode) reifiedTypeNode, genericType)));
		}
	}	

	@org.nasdanika.common.Transformer.Factory(type=EClass.class)
	public EClassNode createEClassNode(
			EClass element,
			boolean parallel,
			Function<EObject, CompletionStage<Element>> elementProvider, 
			CompletionStage<Map<EObject, Element>> registry,
			Consumer<CompletionStage<?>> stageConsumer,
			ProgressMonitor progressMonitor) {
		
		return new EClassNode(element, parallel, elementProvider, stageConsumer, registry, this, progressMonitor);
	}
	
	@org.nasdanika.common.Transformer.Factory(type=EOperation.class)
	public EOperationNode createEOperationNode(
			EOperation element,
			boolean parallel,
			Function<EObject, CompletionStage<Element>> elementProvider, 
			CompletionStage<Map<EObject, Element>> registry,
			Consumer<CompletionStage<?>> stageConsumer,
			ProgressMonitor progressMonitor) {

		return new EOperationNode(element, parallel, elementProvider, stageConsumer, registry, this, progressMonitor);
	}

	@Override
	protected String referencePath(EObjectNode source, EObjectNode target, EReference reference, int index) {
		if (reference.getEKeys().isEmpty() && target.get() instanceof ENamedElement && reference.isUnique()) {
			String name = ((ENamedElement) target.get()).getName();
			if (target.get() instanceof EOperation /** && !((EOperation) target.getTarget()).getEParameters().isEmpty() */) {
				EOperation eOperation = (EOperation) target.get();
				return name + "-" + eOperation.getOperationID();
			}
			return name;
		}
		return super.referencePath(source, target, reference, index);
	}
	
	@Override
	protected Collection<EList<Object>> createBindings(EObjectNode node, EOperation eOperation) {
		if (eOperation == EcorePackage.Literals.ECLASS___GET_FEATURE_TYPE__ESTRUCTURALFEATURE) {
			Collection<EList<Object>> ret = new ArrayList<>();
			for (EStructuralFeature sf: ((EClass) node.get()).getEAllStructuralFeatures()) {
				ret.add(ECollections.singletonEList(sf));
			}
			return ret;
		}
		return super.createBindings(node, eOperation);
	};
	
	@Override
	protected Object argumentToPathSegment(EParameter parameter, Object argument) {
		if (parameter.eContainer() == EcorePackage.Literals.ECLASS___GET_FEATURE_TYPE__ESTRUCTURALFEATURE) {
			return ((ENamedElement) argument).getName();
		}
		
		return super.argumentToPathSegment(parameter, argument);
	};
	
}
