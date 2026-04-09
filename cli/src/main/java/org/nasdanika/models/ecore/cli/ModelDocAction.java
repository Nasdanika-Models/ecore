package org.nasdanika.models.ecore.cli;

/**
 * What to do with the model documentation when documentation resource is available and not empty.
 */
public enum ModelDocAction {
	
	/**
	 * Model documentation is added before the model documentation resource content. This is the default action.
	 */
	PREPEND,
	/**
	 * Model documentation is added after the model documentation resource content. 
	 */
	APPEND,
	/**
	 * Model documentation replaces the model documentation resource content. 
	 */	
	REPLACE,
	/**
	 * Model documentation is ignored - replaced with the model documentation resource content if available and not empty.  
	 */	
	IGNORE,
	/**
	 * Model documentation is put to <code>model-doc</code> token and the model documentation resource content is interpolated with this token. 
	 * <code>${model-doc}</code> token is replaced with the model documentation. 
	 */	
	INTERPOLATE;

}
