package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;

import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.html.model.app.graph.WidgetFactory.Selector;

/**
 * Something that references EClassifiers - generic type, typed element 
 */
public interface EClassifierNodeProcessorProvider {
	
	static Selector<Collection<EClassifierNodeProcessor<?>>> createEClassifierNodeProcessorSelector(int depth) {  
		return (widgetFactory, base, progressMonitor) -> {
			if (widgetFactory instanceof EClassifierNodeProcessorProvider) {
				return ((EClassifierNodeProcessorProvider) widgetFactory).getEClassifierNodeProcessors(depth, progressMonitor);
			}
			
			return Collections.emptyList();
		};
	}
	
	Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, ProgressMonitor progressMonitor);

}
