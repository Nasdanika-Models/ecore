package org.nasdanika.models.ecore.graph.processors;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

/**
 * Annotated method shall have the following signature: 
 * <PRE>
 * {@link NodeProcessorConfig}&lt;Object, {@link WidgetFactory}, {@link WidgetFactory}, {@link Registry}&lt;{@link URI}&gt;&gt; config, 
 * {@link Function}&lt;{@link ProgressMonitor}, {@link Action}&gt; prototypeProvider,
 * {@link BiConsumer}&lt;{@link Label}, {@link ProgressMonitor}&gt; labelConfigurator,
 * {@link ProgressMonitor} progressMonitor
 * </PRE>
 * @author Pavel
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface EStructuralFeatureNodeProcessorFactory {
	
	// Selector
	
	/**
	 * Containing {@link EPackage} namespace URI.
	 * @return
	 */
	String nsURI(); // default "";
	
	/**
	 * {@link EClass} ID as specified in the generated {@link EPackage} constants
	 * @return
	 */
	int classID(); // default -1;
		
	/**
	 * {@link EStructuralFeature} ID as specified in the generated {@link EPackage} constants for a given class.
	 * @return
	 */
	int featureID();	
	
	// Action prototype

	/**
	 * YAML specification of the action prototype. 
	 * @return
	 */
	String actionPrototype() default "";
	
	/**
	 * URI of an action prototype resource resolved relative to the annotated method's class.
	 * @return
	 */
	String actionPrototypeRef() default "";
	
	/**
	 * Element label is used for action text, overrides action prototype setting. 
	 * @return
	 */
	String label() default "";
	
	/**
	 * Action icon, overrides action prototype setting. 
	 * @return
	 */
	String icon() default "";
	
	/**
	 * Description is used for action tooltips, overrides action prototype setting. 
	 * @return
	 */
	String description() default "";
	
	/**
	 * Documentation in markdown, added to action content. 
	 * @return
	 */
	String documentation() default "";

}
