package org.nasdanika.models.ecore.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

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
import org.nasdanika.graph.emf.EObjectGraphFactory;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EObjectNode.ResultRecord;
import org.nasdanika.graph.emf.NodeFactory;

/**
 * Graph factory for ecore models
 * @author Pavel
 *
 */
public class EcoreGraphFactory extends EObjectGraphFactory {
	
	@NodeFactory(type = EClass.class)
	public EClassNode createEClassNode(EObject eObject, BiFunction<EObject, ProgressMonitor, ResultRecord> nodeFactory, ProgressMonitor progressMonitor) {
		return new EClassNode(
				(EClass) eObject, 
				nodeFactory,
				this::createReferenceConnection, 
				this::createOperationConnections,
				(source, genericType, nFactory) -> createReifiedTypeConnection(source, genericType, nFactory, progressMonitor),
				progressMonitor);
	}
	
	@NodeFactory(type = EOperation.class)
	public EOperationNode createEOperationNode(EObject eObject, BiFunction<EObject, ProgressMonitor, ResultRecord> nodeFactory, ProgressMonitor progressMonitor) {
		return new EOperationNode(
				(EOperation) eObject, 
				nodeFactory,
				this::createReferenceConnection, 
				this::createOperationConnections,
				(source, genericType, nFactory) -> createReifiedTypeConnection(source, genericType, nFactory, progressMonitor),
				progressMonitor);
	}

	@Override
	protected String referencePath(EObjectNode source, EObjectNode target, EReference reference, int index) {
		if (reference.getEKeys().isEmpty() && target.getTarget() instanceof ENamedElement && reference.isUnique()) {
			String name = ((ENamedElement) target.getTarget()).getName();
			if (target.getTarget() instanceof EOperation /** && !((EOperation) target.getTarget()).getEParameters().isEmpty() */) {
				EOperation eOperation = (EOperation) target.getTarget();
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
			for (EStructuralFeature sf: ((EClass) node.getTarget()).getEAllStructuralFeatures()) {
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
		
	/**
	 * Creates a connection from the source {@link EClassNode} to reified {@link EGenericType} node. The connection is qualified by the original EGenericType
	 * @param source Source (context) EClass
	 * @param genericType Generic type to be reified. Available via {@link ReifiedTypeConnection}.getGenericType() method.
	 * @param nodeFactory Node factory for connections.
	 * @param progressMonitor Progress monitor.
	 */
	protected void createReifiedTypeConnection(
			EClassNode source, 
			EGenericType genericType, 
			BiFunction<EObject, ProgressMonitor, EObjectNode.ResultRecord> nodeFactory, 
			ProgressMonitor progressMonitor) {
		EGenericType reifiedType = EcoreUtil.getReifiedType(source.getTarget(), genericType);
		if (reifiedType != null && !Objects.equals(reifiedType, genericType)) {
			EObjectNode.ResultRecord reifiedTypeRecord = nodeFactory.apply(reifiedType, progressMonitor);
			new ReifiedTypeConnection(source, reifiedTypeRecord.node(), -1, null, genericType, reifiedTypeRecord.isNew());
		}
	}	
	
}
