package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.WidgetFactory.Selector;

/**
 * Something that references EClassifiers - generic type, typed element 
 */
public interface EClassifierNodeProcessorProvider {
	
	static Selector<Collection<EClassifierNodeProcessor<?>>> createEClassifierNodeProcessorSelector(int depth, Predicate<WidgetFactory> predicate) {  
		return (widgetFactory, base, progressMonitor) -> {
			if (widgetFactory instanceof EClassifierNodeProcessorProvider && predicate.test(widgetFactory)) {
				return ((EClassifierNodeProcessorProvider) widgetFactory).getEClassifierNodeProcessors(depth, predicate, progressMonitor);
			}
			
			return Collections.emptyList();
		};
	}
	
	Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, Predicate<WidgetFactory> predicate, ProgressMonitor progressMonitor);

}
