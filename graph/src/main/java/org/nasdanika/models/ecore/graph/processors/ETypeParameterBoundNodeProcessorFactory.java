package org.nasdanika.models.ecore.graph.processors;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.ETypeParameter;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

/**
 * Annotation for customizing {@link EGenericTypeNodeProcessor} for {@link ETypeParameter} bound.
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
public @interface ETypeParameterBoundNodeProcessorFactory {
	
	// Selector
	
	/**
	 * Containing {@link EPackage} namespace URI.
	 * @return
	 */
	String nsURI() default "";
	
	/**
	 * {@link EClassifier} ID as specified in the generated {@link EPackage} constants
	 * @return
	 */
	int classifierID() default -1;
	
	/**
	 * For {@link EOperation} type parameters - operation ID as specified in the generated {@link EPackage} constants.
	 * @return
	 */
	int operationID() default -1;
	
	/**
	 * Parameter name.
	 * @return
	 */
	String typeParameterName();
	
	/**
	 * Bound index. Defaults to 0.
	 * @return
	 */
	int index() default 0;
	
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
