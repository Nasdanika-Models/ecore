package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;
import org.nasdanika.emf.EmfUtil;
import org.nasdanika.emf.EmfUtil.EModelElementDocumentation;
import org.nasdanika.emf.persistence.MarkerFactory;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Interpolator;
import org.nasdanika.exec.content.Markdown;
import org.nasdanika.exec.content.Text;
import org.nasdanika.graph.emf.EObjectConnection;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.TagName;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.EObjectNodeProcessor;
import org.nasdanika.ncore.util.NcoreUtil;

public class EModelElementNodeProcessor<T extends EModelElement> extends EObjectNodeProcessor<T> {
	
	public EModelElementNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config, 
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	/**
	 * Creating a link only if the action has content 
	 */
	@Override
	public Object createLink(URI base, ProgressMonitor progressMonitor) {
		Label label = createAction(progressMonitor);
		if (isCreateActionForUndocumented() || 
				(label instanceof Action && !(((Action) label).getContent().isEmpty() && ((Action) label).getSections().isEmpty()))) {
			return super.createLink(base, progressMonitor);
		}
		return createLabel(progressMonitor);
	}
	
	/**
	 * @return false if an action/page is created only for documented elements. False if actions/pages are created even for undocumented elements, e.g. in case of {@link EPackage}s and {@link EClassifier}s
	 */
	protected boolean isCreateActionForUndocumented() {
		return false;
	}
	
	@Override
	protected void configureLabel(EObject eObject, Label label, ProgressMonitor progressMonitor) {
		if (eObject instanceof EModelElement) {
			EModelElement modelElement = (EModelElement) eObject;
			if (Util.isBlank(label.getIcon())) {
				boolean isInterface = eObject instanceof EClass && ((EClass) eObject).isInterface();
				String defaultIcon = "https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/" + (isInterface ? "EInterface" : eObject.eClass().getName()) + ".gif";
				label.setIcon(NcoreUtil.getNasdanikaAnnotationDetail(modelElement, "icon", defaultIcon));
			}
			if (Util.isBlank(label.getTooltip())) {
				label.setTooltip(NcoreUtil.getNasdanikaAnnotationDetail(modelElement, "description", null));
			}
			if (Util.isBlank(label.getTooltip())) {
				String modelDoc = EcoreUtil.getDocumentation(modelElement);
				if (!Util.isBlank(modelDoc))
				label.setTooltip(modelDoc);
			}
			if (Util.isBlank(label.getText())) {
				label.setText(NcoreUtil.getNasdanikaAnnotationDetail(modelElement, "label", null));
			}		
			if (modelElement instanceof ENamedElement && Util.isBlank(label.getText())) {
				label.setText(((ENamedElement) modelElement).getName());
			}
		}
		super.configureLabel(eObject, label, progressMonitor);
	}
	
	@Override
	protected Action newAction(EObject eObject, ProgressMonitor progressMonitor) {
		Action newAction = super.newAction(eObject, progressMonitor);
		return newAction;
	}
	
	@Override
	protected Label createAction(ProgressMonitor progressMonitor) {
		Label action = super.createAction(progressMonitor);
		EModelElementDocumentation documentation = EmfUtil.getDocumentation((EModelElement) node.get());
		if (documentation != null) {
			action.getContent().add(interpolatedMarkdown(context.interpolateToString(documentation.documentation()), documentation.location(), progressMonitor));			
		}
		return action;
	}
	
	/**
	 * @param markdown Markdown text
	 * @return Spec for interpolating markdown and then converting to HTML. 
	 */
	protected Markdown interpolatedMarkdown(String markdown, URI location, ProgressMonitor progressMonitor) {
		if (Util.isBlank(markdown)) {
			return null;
		}
		Markdown ret = ContentFactory.eINSTANCE.createMarkdown();
		Interpolator interpolator = ContentFactory.eINSTANCE.createInterpolator();
		Text text = ContentFactory.eINSTANCE.createText();
		text.setContent(markdown);
		interpolator.setSource(text);
		ret.setSource(interpolator);
		ret.setStyle(true);
		
		// Creating a marker with EObject resource location for resource resolution in Markdown
		if (location != null) {
			org.nasdanika.ncore.Marker marker = context.get(MarkerFactory.class, MarkerFactory.INSTANCE).createMarker(location.toString(), progressMonitor);
			ret.getMarkers().add(marker); 
		}
		
		return ret;
	}

	/**
	 * Suppressing default behavior, explicit specification of how to build.
	 */	
	@Override
	protected void addReferenceChildren(
			EReference eReference, 
			Collection<Label> labels, 
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
	}
	
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		// TODO - EAnnotations...
		return false;
	}
	
	protected static String cardinality(ETypedElement typedElement) {
		int lowerBound = typedElement.getLowerBound();
		int upperBound = typedElement.getUpperBound();
		String cardinality;
		if (lowerBound == upperBound) {
			cardinality = String.valueOf(lowerBound);
		} else {
			cardinality = lowerBound + ".." + (upperBound == -1 ? "*" : String.valueOf(upperBound));
		}
		if (typedElement instanceof EReference && ((EReference) typedElement).isContainment()) {
			cardinality = "<B>"+cardinality+"</B>";
		}
		return cardinality;
	}
	
	// --- Reusable methods ---
	
	protected String typeLink(EObjectConnection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		return typeLink(connection, widgetFactory, null, progressMonitor);
	}

	/**
	 * Creates a link to a typed element type. Defaults to typed element name.
	 * @param connection
	 * @param widgetFactory
	 * @param withIcon
	 * @param progressMonitor
	 * @return
	 */
	protected String typeLink(EObjectConnection connection, WidgetFactory widgetFactory, URI base, ProgressMonitor progressMonitor) {		
		EGenericType eGenericType = ((ETypedElement) connection.getTarget().get()).getEGenericType();
		if (eGenericType == null) {
			return "void";
		}
		String typeName = eGenericType.getERawType().getName(); // TODO - as string
		String typeNameComment = "<!-- " + typeName + "--> ";
		String linkStr = widgetFactory.createWidgetString(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE, base, progressMonitor);
		if (linkStr == null) {
			linkStr = widgetFactory.createWidgetString(EcorePackage.Literals.ETYPED_ELEMENT__ETYPE, base, progressMonitor);			
		}
		return typeNameComment + (Util.isBlank(linkStr) ? typeName : linkStr);
	}
	
	protected String targetNameLink(EReferenceConnection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		boolean isDirect = false;
		EObject tt = connection.getTarget().get();
		if (tt instanceof EStructuralFeature) {
			isDirect = ((EStructuralFeature) tt).getEContainingClass() == getTarget();
		} else if (tt instanceof EOperation) {
			isDirect = ((EOperation) tt).getEContainingClass() == getTarget();
		} else if (tt instanceof EGenericType) {
			isDirect = tt.eContainer() == getTarget();			
		}
		String linkStr = widgetFactory.createLinkString(progressMonitor);
		String name = Util.isBlank(linkStr) ? ((ENamedElement) connection.getTarget().get()).getName() : linkStr;
		return isDirect ? TagName.b.create(name).toString() : name;
	}
		
	protected String description(EReferenceConnection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		Object label = widgetFactory.createLabel(progressMonitor);
		return label instanceof Label ? ((Label) label).getTooltip() : null;
	}	
	
	protected String declaringClassLink(EReferenceConnection connection, WidgetFactory widgetFactory, ProgressMonitor progressMonitor) {
		String declaringClassName;
		String linkStr;
		
		EObject tt = connection.getTarget().get();
		if (tt instanceof EStructuralFeature) {
			declaringClassName = ((EStructuralFeature) connection.getTarget().get()).getEContainingClass().getName();
			linkStr = widgetFactory.createWidgetString(EcorePackage.Literals.ESTRUCTURAL_FEATURE__ECONTAINING_CLASS, progressMonitor);
		} else if (tt instanceof EOperation) {
			declaringClassName = ((EOperation) connection.getTarget().get()).getEContainingClass().getName();
			linkStr = widgetFactory.createWidgetString(EcorePackage.Literals.EOPERATION__ECONTAINING_CLASS, progressMonitor);
		} else {
			throw new IllegalArgumentException("Should be EStructuralOperation or EOperation: " + tt);
		}
		
		String declaringClassNameComment = "<!-- " + declaringClassName + "--> ";
		return declaringClassNameComment + (Util.isBlank(linkStr) ? declaringClassName : linkStr);
	}

	/**
	 * Builds columns for {@link ENamedElement}
	 * @param tableBuilder
	 * @param progressMonitor
	 */
	protected void buildNamedElementColumns(DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> tableBuilder, ProgressMonitor progressMonitor) {
		tableBuilder
			.addStringColumnBuilder("name", true, false, "Name", endpoint -> targetNameLink(endpoint.getKey(), endpoint.getValue(), progressMonitor)) 
			.addStringColumnBuilder("description", true, false, "Description", endpoint -> description(endpoint.getKey(), endpoint.getValue(), progressMonitor));
	}
	
	/**
	 * Builds columns for {@link ETypedElement} including {@link ENamedElement} columns
	 * @param tableBuilder
	 * @param progressMonitor
	 */
	protected void buildTypedElementColumns(DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> tableBuilder, ProgressMonitor progressMonitor) {
		buildNamedElementColumns(tableBuilder, progressMonitor);
		tableBuilder
			.addStringColumnBuilder("type", true, true, "Type", endpoint -> typeLink(endpoint.getKey(), endpoint.getValue(), progressMonitor))  
			.addStringColumnBuilder("cardinality", true, true, "Cardinality", endpoint -> cardinality((ETypedElement) endpoint.getKey().getTarget().get()));

//		getLowerBound()
//		getUpperBound()
//		isMany()
//		isOrdered()
//		isRequired()
//		isUnique()		
		
	}
	
	/**
	 * Builds columns for {@link ETypedElement} including {@link ENamedElement} columns
	 * @param tableBuilder
	 * @param progressMonitor
	 */
	protected void buildStructuralFeatureColumns(DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> tableBuilder, ProgressMonitor progressMonitor) {
		buildTypedElementColumns(tableBuilder, progressMonitor);
		tableBuilder
			.addStringColumnBuilder("declaring-class", true, true, "Declaring Class", endpoint -> declaringClassLink(endpoint.getKey(), endpoint.getValue(), progressMonitor))
			.addBooleanColumnBuilder("changeable", true, false, "Changeable", endpoint -> ((EStructuralFeature) endpoint.getKey().getTarget().get()).isChangeable())
			.addBooleanColumnBuilder("derived", true, false, "Derived", endpoint -> ((EStructuralFeature) endpoint.getKey().getTarget().get()).isDerived());

// TODO
//		getDefaultValue()
//		getDefaultValueLiteral()
//		getFeatureID()
//		isTransient()
//		isUnsettable()
//		isVolatile()		
	}
	
	/**
	 * Builds columns for {@link ETypedElement} including {@link ENamedElement} columns
	 * @param tableBuilder
	 * @param progressMonitor
	 */
	protected void buildClassifierColumns(DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> tableBuilder, ProgressMonitor progressMonitor) {
		buildNamedElementColumns(tableBuilder, progressMonitor);

// TODO
//		getClassifierID()
//		getDefaultValue()
//		getEPackage()
//		getETypeParameters()
//		getInstanceClass()
//		getInstanceClassName()
//		getInstanceTypeName()
	}
	
	/**
	 * Loads documentation from Ecore model annotations. Override to return documentation from annotations.
	 * @return
	 */
	protected EModelElementDocumentation getLoadDocumentation() {
		return EmfUtil.getLoadDocumentation(getTarget());
	}
	
}
