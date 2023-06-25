package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.persistence.Feature;

/**
 * {@link WidgetFactory} for an object {@link Feature} - {@link EStructuralFeature} or {@link EOperation}
 */
public interface FeatureWidgetFactory extends WidgetFactory {
	
	String getLoadKey();
	
	boolean isLoadable();
	
	boolean hasLoadSpecificationAction();
	
	String getLoadDescription();

}
