package org.nasdanika.models.ecore.processors.doc;

import org.nasdanika.common.Context;

/**
 * Creates documentation generating processors
 * @author Pavel
 *
 */
public class EcoreDocProcessorFactory {
	
	protected Context context;
	
	public EcoreDocProcessorFactory(Context context) {
		this.context = context;
	}
	
//	@EPackageNodeProcessorFactory(nsURI = EcorePackage.eNS_URI)
//	public EPackageNodeProcessor createEPackageProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider) {		
//		return new EPackageNodeProcessor(config, context, prototypeProvider);
//	}		

}
