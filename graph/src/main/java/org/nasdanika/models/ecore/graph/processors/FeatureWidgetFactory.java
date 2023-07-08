package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.persistence.Feature;

/**
 * {@link WidgetFactory} for an object {@link Feature} - {@link EStructuralFeature} or {@link EOperation}
 */
public interface FeatureWidgetFactory extends WidgetFactory {
	
	String getLoadKey(EClass eClass);
	
	boolean isDefaultFeature(EClass eClass);
	
	boolean isLoadable();
	
	boolean hasLoadSpecificationAction();
	
	URI getLoadSpecRef(URI base);
	
	String getLoadDescription();

}
