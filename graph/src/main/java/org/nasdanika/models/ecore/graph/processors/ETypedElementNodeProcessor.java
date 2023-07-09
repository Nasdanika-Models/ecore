package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.emf.EmfUtil.EModelElementDocumentation;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.graph.Registry;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.ncore.util.NcoreUtil;

public abstract class ETypedElementNodeProcessor<T extends ETypedElement> extends ENamedElementNodeProcessor<T> implements FeatureWidgetFactory {

	public ETypedElementNodeProcessor(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	private WidgetFactory typeWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eType'")
	public final void setTypeEndpoint(WidgetFactory typeWidgetFactory) {
		this.typeWidgetFactory = typeWidgetFactory;
	}
	
	private WidgetFactory genericTypeWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eGenericType'")
	public final void setGenericTypeEndpoint(WidgetFactory genericTypeWidgetFactory) {
		this.genericTypeWidgetFactory = genericTypeWidgetFactory;
	}
	
	@Override
	public Object createWidget(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == EcorePackage.Literals.ETYPED_ELEMENT__ETYPE && typeWidgetFactory != null) {
			return typeWidgetFactory.createLink(base, progressMonitor);
		}
		if (selector == EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE && genericTypeWidgetFactory != null) {
			return genericTypeWidgetFactory.createLink(base, progressMonitor);
		}
		if (selector instanceof EClassNodeProcessor.ReifiedTypeSelector) {
			EClassNodeProcessor.ReifiedTypeSelector reifiedTypeSelector = (EClassNodeProcessor.ReifiedTypeSelector) selector;
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE) {
				EGenericType genericType = getTarget().getEGenericType();
				WidgetFactory rwf = reifiedTypeSelector.getReifiedTypeWidgetFactory(genericType);
				if (rwf == null) {
					if (genericTypeWidgetFactory != null) {
						return genericTypeWidgetFactory.createLink(base, progressMonitor);
					}
				}
				return rwf.createLink(base, progressMonitor);
			}
		}		
		
		return super.createWidget(selector, base, progressMonitor);
	}

	// 

	@Override
	public String getLoadKey(EClass eClass) {
		return NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.LOAD_KEY, NcoreUtil.getFeatureKey(eClass, getTarget()));
	}
	
	@Override
	public boolean isDefaultFeature(EClass eClass) {
		return EObjectLoader.isDefaultFeature(eClass, getTarget());
	}

	@Override
	public boolean hasLoadSpecificationAction() {
		return getLoadDocumentation() != null;
	}

	@Override
	public String getLoadDescription() {
		return "TODO!";
	}	
	
	@Override
	public URI getLoadSpecRef(URI base) {
		if (uri == null) {
			return null;
		}
		URI loadSpecRef = URI.createURI("load-specification.html").resolve(uri);
		return base == null ? loadSpecRef : loadSpecRef.deresolve(base, true, true, true);
	}
	
	@Override
	public boolean isLoadable() {
		return "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.IS_LOADABLE, "true"));
	}	
	
	@Override
	protected Label createAction(ProgressMonitor progressMonitor) {
		Label action = super.createAction(progressMonitor);
		if (isLoadable()) {
			Action loadSpecificationAction = createLoadSpecificationAction(action, progressMonitor);
			if (loadSpecificationAction != null && action instanceof Action) {
				((Action) action).getNavigation().add(loadSpecificationAction);
			}
		}
		
		return action;
	}

	protected Action createLoadSpecificationAction(Label action, ProgressMonitor progressMonitor) {
		EModelElementDocumentation loadDoc = getLoadDocumentation();
		if (loadDoc == null) {
			return null;
		}
		Action loadSpecificationAction = AppFactory.eINSTANCE.createAction();
		loadSpecificationAction.setText("Load specification");
		loadSpecificationAction.setLocation("load-specification.html");
		
		// TODO - properties table: key, type, cardinality, homogeneous, strict containment, exclusive with
				
		loadSpecificationAction.getContent().add(interpolatedMarkdown(loadDoc.documentation(), loadDoc.location(), progressMonitor));
		return loadSpecificationAction;
	}
	
}


//private void generateLoadSpecification(
//		Action action, 
//		Comparator<ENamedElement> namedElementComparator,
//		ProgressMonitor progressMonitor) {
//	
//	// Load specification
//	if (!eObject.isAbstract() && "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(eObject, EObjectLoader.IS_LOADABLE, "true"))) {
//		Action loadSpecificationAction = AppFactory.eINSTANCE.createAction();
//		loadSpecificationAction.setText("Load specification");
//		loadSpecificationAction.setLocation(eObject.getName() + "-load-specification.html");			
//		action.getNavigation().add(loadSpecificationAction);
//		
//		EModelElementDocumentation loadDoc = EmfUtil.getLoadDocumentation(eObject);
//		if (loadDoc != null) {
//			loadSpecificationAction.getContent().add(interpolatedMarkdown(loadDoc.documentation(), loadDoc.location(), progressMonitor));
//		}
//		
//		Predicate<EStructuralFeature> predicate = sf -> sf.isChangeable() && "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.IS_LOADABLE, "true"));
//		List<EStructuralFeature> sortedFeatures = eObject.getEAllStructuralFeatures().stream().filter(predicate.and(elementPredicate)).sorted(namedElementComparator).collect(Collectors.toList());
//		
//		Function<EStructuralFeature, String> keyExtractor = sf -> NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.LOAD_KEY, NcoreUtil.getFeatureKey(eObject, sf));
//		Predicate<EStructuralFeature> HomogeneousPredicate = sf -> "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.IS_Homogeneous)) || NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.REFERENCE_TYPE) != null;
//		Predicate<EStructuralFeature> strictContainmentPredicate = HomogeneousPredicate.and(sf -> "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(sf, EObjectLoader.IS_STRICT_CONTAINMENT)));
//		Function<EStructuralFeature, Object[]> exclusiveWithExtractor = sf -> EObjectLoader.getExclusiveWith(eObject, sf, EObjectLoader.LOAD_KEY_PROVIDER);
//		
//		DynamicTableBuilder<EStructuralFeature> loadSpecificationTableBuilder = new DynamicTableBuilder<>();
//		loadSpecificationTableBuilder
//			.addStringColumnBuilder("key", true, true, "Key", sf -> {
//				String key = keyExtractor.apply(sf);
//				return TagName.a.create(key).attribute("href", "#key-section-" + key).attribute("style", "font-weight:bold", EObjectLoader.isDefaultFeature(eObject, sf)).toString();
//			})
//			.addStringColumnBuilder("type", true, true, "Type", attr -> {
//				EGenericType genericType = attr.getEGenericType(); 
//				if (genericType == null) {
//					return null;
//				}
//				StringBuilder sb = new StringBuilder();
//				genericType(genericType, eObject, sb::append, progressMonitor);
//				return sb.toString();
//			})
//			.addStringColumnBuilder("cardinality", true, false, "Cardinality", EModelElementActionSupplier::cardinality)
//			.addBooleanColumnBuilder("Homogeneous", true, false, "Homogeneous", HomogeneousPredicate)
//			.addBooleanColumnBuilder("strict-containment", true, false, "Strict Containment", strictContainmentPredicate)
//			.addStringColumnBuilder("exclusive-with", true, false, "Exclusive With", sf -> {
//				Object[] exclusiveWith = exclusiveWithExtractor.apply(sf);
//				if (exclusiveWith.length == 0) {
//					return null;
//				}
//				Tag ul = TagName.ul.create();
//				for (Object exw: exclusiveWith) {
//					ul.content(TagName.li.create(exw));
//				}
//				return ul.toString();				
//			})
//			.addStringColumnBuilder("description", true, false, "Description", this::getEStructuralFeatureFirstLoadDocSentence);
//			// Other things not visible?
//		
//		org.nasdanika.html.model.html.Tag loadSpecificationTable = loadSpecificationTableBuilder.build(sortedFeatures, eObject.getEPackage().getNsURI().hashCode() + "-" + eObject.getName() + "-load-specification", "load-specification-table", progressMonitor);						
//		
//		for (EStructuralFeature sf: sortedFeatures) {
//			Action featureAction = AppFactory.eINSTANCE.createAction();
//			String key = keyExtractor.apply(sf);
//			featureAction.setText(key);
//			String sectionAnchor = "key-section-" + key;
//			
//			featureAction.setName(sectionAnchor);			
//			loadSpecificationAction.getSections().add(featureAction);
//
//			// Properties table
//			Table table = context.get(BootstrapFactory.class).table();
//			table.toHTMLElement().style().width("auto");
//			
//			genericType(sf.getEGenericType(), eObject, ETypedElementActionSupplier.addRow(table, "Type")::add, progressMonitor);
//			
//			boolean isDefaultFeature = EObjectLoader.isDefaultFeature(eObject, sf);
//			if (isDefaultFeature) {
//				ETypedElementActionSupplier.addRow(table, "Default").add("true");				
//			}
//			
//			boolean isHomogeneous = HomogeneousPredicate.test(sf);
//			if (isHomogeneous) {
//				ETypedElementActionSupplier.addRow(table, "Homogeneous").add("true");									
//			}
//			
//			boolean isStrictContainment = strictContainmentPredicate.test(sf);			
//			if (isStrictContainment) {
//				ETypedElementActionSupplier.addRow(table, "Strict containment").add("true");									
//			}
//			
//			Object[] exclusiveWith = exclusiveWithExtractor.apply(sf);
//			if (exclusiveWith.length != 0) {
//				Tag ul = TagName.ul.create();
//				for (Object exw: exclusiveWith) {
//					ul.content(TagName.li.create(exw));
//				}
//				ETypedElementActionSupplier.addRow(table, "Exclusive with").add(ul);				
//			}
//
//			addContent(featureAction, table.toString());
//			
//			EModelElementDocumentation featureLoadDoc = getFeatureLoadDoc(sf);
//			if (featureLoadDoc != null) {
//				featureAction.getContent().add(interpolatedMarkdown(context.interpolateToString(featureLoadDoc.documentation()), featureLoadDoc.location(), progressMonitor));
//			}
//		}	
//		
//		loadSpecificationAction.getContent().add(loadSpecificationTable);
//	}
//}
