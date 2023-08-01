package org.nasdanika.models.ecore.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.emf.EObjectGraphFactory;
import org.nasdanika.graph.emf.EObjectNode;

public class EOperationNode extends EObjectNode {
	
	public EOperationNode(
			EOperation target,
			boolean parallel,
			Function<EObject, CompletionStage<Element>> elementProvider, 
			Consumer<CompletionStage<?>> stageConsumer,
			CompletionStage<Map<EObject, Element>> registry,
			EObjectGraphFactory factory,
			ProgressMonitor progressMonitor) {
		super(target, parallel, elementProvider, stageConsumer, registry, factory, progressMonitor);
		
		List<EOperation> inheritedOperations = new ArrayList<>();
		target
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
				boolean isOverridden = false;
				Iterator<EOperation> iit = inheritedOperations.iterator();
				while (iit.hasNext()) {
					EOperation iop = iit.next();
					if (op.isOverrideOf(iop)) {
						iit.remove();
					} else if (iop.isOverrideOf(op)) {
						isOverridden = true;
					}							
				}
				if (!isOverridden) {
					inheritedOperations.add(op);
				}
			});
			
		for (EOperation io: inheritedOperations) {
			if (target.isOverrideOf(io)) {
				stageConsumer.accept(elementProvider.apply(io).thenAccept(ioNode -> new OverridesConnection(this, (EObjectNode) ioNode)));
			}
		}		
	}

	@Override
	public EOperation get() {
		return (EOperation) super.get();
	}
	
}
