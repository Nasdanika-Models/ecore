package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.emf.EmfUtil.EModelElementDocumentation;
import org.nasdanika.emf.persistence.EObjectLoader;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Text;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.Tag;
import org.nasdanika.html.TagName;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.WidgetFactory.Selector;
import org.nasdanika.html.model.bootstrap.BootstrapFactory;
import org.nasdanika.html.model.bootstrap.Table;
import org.nasdanika.html.model.bootstrap.TableCell;
import org.nasdanika.html.model.bootstrap.TableRow;
import org.nasdanika.html.model.bootstrap.TableSection;
import org.nasdanika.ncore.util.NcoreUtil;

public abstract class ETypedElementNodeProcessor<T extends ETypedElement> extends ENamedElementNodeProcessor<T> implements FeatureWidgetFactory, EClassifierNodeProcessorProvider {

	public ETypedElementNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	private WidgetFactory typeWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eType'")
	public final void setTypeEndpoint(WidgetFactory typeWidgetFactory) {
		this.typeWidgetFactory = typeWidgetFactory;
	}
	
	protected WidgetFactory genericTypeWidgetFactory;
	
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
				if (rwf != null) {
					return rwf.createLink(base, progressMonitor);
				}
				if (genericTypeWidgetFactory != null) {
					return genericTypeWidgetFactory.createLink(base, progressMonitor);
				} 
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
		return null;
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
		
		// TODO - properties table, subclasses may add more rows 
		
		return action;
	}
	
	protected TableRow buildPropertyRow(String name, Collection<EObject> cellValues) {
		TableRow row = BootstrapFactory.eINSTANCE.createTableRow();
		TableCell nameCell = BootstrapFactory.eINSTANCE.createTableCell();
		row.getCells().add(nameCell);
		nameCell.setHeader(true);
		Text nameText = ContentFactory.eINSTANCE.createText();
		nameText.setContent(name);
		nameCell.getContent().add(nameText);		
		
		TableCell valueCell = BootstrapFactory.eINSTANCE.createTableCell();
		row.getCells().add(valueCell);
		valueCell.getContent().addAll(cellValues);
		
		return row;		
	}
	
	protected TableRow buildPropertyRow(String name, boolean cellValue) {
		return buildPropertyRow(name, cellValue ? "<i class=\"fas fa-check\"></i>" : "");
	}
	
	protected TableRow buildPropertyRow(String name, String cellValue) {
		Text valueText = ContentFactory.eINSTANCE.createText();
		valueText.setContent(cellValue);
		return buildPropertyRow(name, Collections.singleton(valueText));
	}

	protected Action createLoadSpecificationAction(Label action, ProgressMonitor progressMonitor) {
		EModelElementDocumentation loadDoc = getLoadDocumentation();
		if (loadDoc == null) {
			return null;
		}
		Action loadSpecificationAction = AppFactory.eINSTANCE.createAction();
		loadSpecificationAction.setText("Load specification");
		loadSpecificationAction.setLocation("load-specification.html");
		
		// Properties table
		Table propertiesTable = BootstrapFactory.eINSTANCE.createTable();
		Text autoText = ContentFactory.eINSTANCE.createText();
		autoText.setContent("width:auto");
		propertiesTable.getAttributes().put("style", autoText);
		TableSection body = BootstrapFactory.eINSTANCE.createTableSection();
		propertiesTable.setBody(body);
		EList<TableRow> bodyRows = body.getRows();
		bodyRows.add(buildPropertyRow("Load Key", getLoadKey(null)));
		
		if (getTarget() instanceof EOperation) {
			// TODO
//			bodyRows.add(buildPropertyRow("Type", Collections.emptyList()));
//			return featureWidgetFactoryEntry.getValue().createWidgetString((Selector<Object>) this::parameterTypes, progressMonitor);
		} else {
//			return typeLink(featureWidgetFactoryEntry.getKey(), featureWidgetFactoryEntry.getValue(), progressMonitor)
		}
		
		bodyRows.add(buildPropertyRow("Default", Collections.emptyList()));
		bodyRows.add(buildPropertyRow("Multiplicity", Collections.emptyList()));
		bodyRows.add(buildPropertyRow("Homogeneous", Collections.emptyList()));
		bodyRows.add(buildPropertyRow("Strict Containment", Collections.emptyList()));
		bodyRows.add(buildPropertyRow("Exclusive With", Collections.emptyList()));
		bodyRows.add(buildPropertyRow("Description", Collections.emptyList()));
		
//		loadSpecificationTableBuilder.addStringColumnBuilder("key", true, true, "Key", featureWidgetFactoryEntry -> {
//			FeatureWidgetFactory featureWidgetFactory = featureWidgetFactoryEntry.getValue();
//			String key = featureWidgetFactory.getLoadKey(getTarget());
//			// TODO - link if there is a feature spec detail
//			if (featureWidgetFactory.isDefaultFeature(getTarget())) {
//				key = "<b>" + key + "</b>";
//			}
//			if (featureWidgetFactory.hasLoadSpecificationAction()) {
//				URI loadSpecURI = featureWidgetFactory.getLoadSpecRef(uri);
//				key = "<a href=\"" + loadSpecURI + "\">" + key + "</a>";
//			}
//			return key;
//		});
//	
//	loadSpecificationTableBuilder.addStringColumnBuilder("type", true, true, "Type", featureWidgetFactoryEntry -> {
//		EObject target = featureWidgetFactoryEntry.getKey().getTarget().getTarget();
//		if (target instanceof EOperation) {
//			return featureWidgetFactoryEntry.getValue().createWidgetString((Selector<Object>) this::parameterTypes, progressMonitor);
//		}
//		return typeLink(featureWidgetFactoryEntry.getKey(), featureWidgetFactoryEntry.getValue(), progressMonitor);
//	});  
		
		
		
		
//			EList<TableCell> elementRowCells = elementRow.getCells();
//			for (ColumnBuilder<? super T> cb: columnBuilders) {
//				TableCell elementCell = BootstrapFactory.eINSTANCE.createTableCell();
//				elementRowCells.add(elementCell);
//				cb.buildCell(element, elementCell, progressMonitor);
//			}
		
		loadSpecificationAction.getContent().add(propertiesTable);
		
		
//		Table table = context.get(BootstrapFactory.class).table();
//		table.toHTMLElement().style().width("auto");
		
//		loadSpecificationTableBuilder.addStringColumnBuilder("cardinality", true, false, "Cardinality", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getCardinality());
//		loadSpecificationTableBuilder.addBooleanColumnBuilder("homogeneous", true, false, "Homogeneous", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).isHomogeneous());
//		loadSpecificationTableBuilder.addBooleanColumnBuilder("strict-containment", true, false, "Strict Containment", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).isStrictContainment());
//		loadSpecificationTableBuilder.addStringColumnBuilder("exclusive", true, true, "Exclusive With", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getExclusiveWith(getTarget()));
//		loadSpecificationTableBuilder.addStringColumnBuilder("description", true, false, "Description", featureWidgetFactoryEntry -> ((ETypedElementNodeProcessor<?>) featureWidgetFactoryEntry.getValue()).getLoadDescription());
//		
//		
//		genericType(sf.getEGenericType(), eObject, ETypedElementActionSupplier.addRow(table, "Type")::add, progressMonitor);
//		
//		boolean isDefaultFeature = EObjectLoader.isDefaultFeature(eObject, sf);
//		if (isDefaultFeature) {
//			ETypedElementActionSupplier.addRow(table, "Default").add("true");				
//		}
//		
//		boolean isHomogeneous = HomogeneousPredicate.test(sf);
//		if (isHomogeneous) {
//			ETypedElementActionSupplier.addRow(table, "Homogeneous").add("true");									
//		}
//		
//		boolean isStrictContainment = strictContainmentPredicate.test(sf);			
//		if (isStrictContainment) {
//			ETypedElementActionSupplier.addRow(table, "Strict containment").add("true");									
//		}
//		
//		Object[] exclusiveWith = exclusiveWithExtractor.apply(sf);
//		if (exclusiveWith.length != 0) {
//			Tag ul = TagName.ul.create();
//			for (Object exw: exclusiveWith) {
//				ul.content(TagName.li.create(exw));
//			}
//			ETypedElementActionSupplier.addRow(table, "Exclusive with").add(ul);				
//		}

//		addContent(loadSpecificationAction., table.toString());
						
		loadSpecificationAction.getContent().add(interpolatedMarkdown(loadDoc.documentation(), loadDoc.location(), progressMonitor));
		return loadSpecificationAction;
	}
	
	/**
	 * Type member cardinality to show on diagrams. 0..1 and 1..1 are the default - not shown
	 * @return
	 */
	public String getMemberMultiplicity() {
		String multiplicity = getRelationMultiplicity();
		if (multiplicity == null) {
			return multiplicity;
		}
		
		if ("*".equals(multiplicity)) {
			return "[]";
		}
		
		return "[" + multiplicity + "]";
	}
	
	/**
	 * Cardinality to show on diagrams. 0..1 and 1..1 are the default - not shown
	 * @return
	 */
	public String getRelationMultiplicity() {
		T typedElement = getTarget();
		int lowerBound = typedElement.getLowerBound();
		int upperBound = typedElement.getUpperBound();
		
		if ((lowerBound == 0 || lowerBound ==1) && upperBound == 1) {
			return null; // Default cardinalities - should not be shown 
		}
		
		if (lowerBound == upperBound) {
			return String.valueOf(lowerBound);
		} 
		
		if (lowerBound == 0 && upperBound == -1) {
			return "*";
		}
				
		return lowerBound + ".." + (upperBound == -1 ? "*" : String.valueOf(upperBound));
	}	
	
	public String getMultiplicity() {
		T typedElement = getTarget();
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
	
	public boolean isHomogeneous() {
		return "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.IS_HOMOGENEOUS)) || NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.REFERENCE_TYPE) != null;
	}
	
	public boolean isStrictContainment() {
		return isHomogeneous() && "true".equals(NcoreUtil.getNasdanikaAnnotationDetail(getTarget(), EObjectLoader.IS_STRICT_CONTAINMENT)); 
	}

	public String getExclusiveWith(EClass eClass) {
		Object[] exclusiveWith = EObjectLoader.getExclusiveWith(eClass, getTarget(), EObjectLoader.LOAD_KEY_PROVIDER);
		if (exclusiveWith.length == 0) {
			return null;
		}
		Tag ul = TagName.ul.create();
		for (Object exw : exclusiveWith) {
			ul.content(TagName.li.create(exw));
		}
		return ul.toString();
	}
	
	public DiagramElement generateTypeDiagramElement(
			URI base, 
			Function<EClassifier, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {		
		
		Selector<DiagramElement> diagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDiagramElement(sBase, diagramElementProvider, pm);
		};
		
		return typeWidgetFactory.createWidget(diagramElementSelector, base, progressMonitor);
	}
	
	@Override
	public Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, ProgressMonitor progressMonitor) {
		Collection<EClassifierNodeProcessor<?>> ret = new HashSet<>();
		Selector<Collection<EClassifierNodeProcessor<?>>> selector = EClassifierNodeProcessorProvider.createEClassifierNodeProcessorSelector(depth);
		// generic type
		if (genericTypeWidgetFactory != null) {
			ret.addAll(genericTypeWidgetFactory.createWidget(selector, progressMonitor));
		}

		return ret;
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
//		List<EStructuralFeature> sortedFeatures = eObject.getEAllStructuralFeatures().stream().filter(predicate.and(elementPredicate)).sorted(namedElementComparator).toList();
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
