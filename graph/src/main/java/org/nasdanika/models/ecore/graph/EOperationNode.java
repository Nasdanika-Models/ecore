package org.nasdanika.models.ecore.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.emf.EOperationConnection;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.emf.EReferenceConnection.Factory;

public class EOperationNode extends EObjectNode {

	public EOperationNode(
			EOperation target, 
			BiFunction<EObject, ProgressMonitor, ResultRecord> nodeFactory, 
			EReferenceConnection.Factory referenceConnectionFactory,
			EOperationConnection.Factory operationConnectionFactory,
			ReifiedTypeConnection.Factory reifiedTypeConnectionFactory,
			ProgressMonitor progressMonitor) {
		super(target, nodeFactory, referenceConnectionFactory, operationConnectionFactory, progressMonitor);
	}

	@Override
	public EOperation getTarget() {
		return (EOperation) super.getTarget();
	}
	
	@Override
	protected void resolve(Function<EObject, EObjectNode> registry, Factory referenceConnectionFactory,	ProgressMonitor progressMonitor) {
		super.resolve(registry, referenceConnectionFactory, progressMonitor);
				
		EOperation eOperation = getTarget();
		List<EOperation> inheritedOperations = new ArrayList<>();
		eOperation
			.getEContainingClass()
			.getEAllSuperTypes()
			.stream()
			.flatMap(st -> st.getEAllOperations().stream())
			.distinct()
			.sorted((a, b) -> {
				if (a == b) {
					return 0;
				}
				EClass acc = a.getEContainingClass();
				EClass bcc = b.getEContainingClass();
				if (acc != bcc) {
					if (acc.isSuperTypeOf(bcc)) {
						return 1;
					}
					if (bcc.isSuperTypeOf(acc)) {
						return -1;
					}
				}
				return a.getOperationID() - b.getOperationID();				
			}).forEach(op -> {
				for (EOperation io: inheritedOperations) {
					if (io.isOverrideOf(op)) {
						return;
					}
				}
				inheritedOperations.add(op);				
			});
			
		for (EOperation io: inheritedOperations) {
			if (eOperation.isOverrideOf(io)) {
				EObjectNode ioNode = registry.apply(io);
				if (ioNode != null) {
					new OverridesConnection(this, ioNode);
				}
			}
		}
	}
	
}
