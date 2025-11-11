/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Mark Hoffmann - initial API and implementation
 */
package org.gecko.emf.sensinact.model.functions;

import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.sensinact.mapping.FeatureMapping;

/**
 * Function service that extracts GenModel documentation annotations from EAttributes.
 * This function can be registered with a ValueMapper to extract documentation from model elements.
 *
 * <p>Usage example:</p>
 * <pre>
 * Map&lt;String, BiFunction&lt;EObject, FeatureMapping, Object&gt;&gt; functions = new HashMap&lt;&gt;();
 * functions.put("extractGenModelDoc", GenModelDocExtractor.INSTANCE);
 *
 * ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, mapping, functions);
 * </pre>
 *
 * <p>In your mapping configuration, set the functionId:</p>
 * <pre>
 * &lt;descriptionMapping functionId="extractGenModelDoc"&gt;
 *   &lt;featurePath href="...#//YourEClass/yourAttribute"/&gt;
 * &lt;/descriptionMapping&gt;
 * </pre>
 *
 * @author Mark Hoffmann
 * @since 31.07.2025
 */
public class GenModelDocExtractor implements BiFunction<EObject, FeatureMapping, Object> {

	/**
	 * Singleton instance that can be used directly in function registries.
	 */
	public static final GenModelDocExtractor INSTANCE = new GenModelDocExtractor();

	/**
	 * GenModel annotation source URI.
	 */
	private static final String GENMODEL_ANNOTATION = "http://www.eclipse.org/emf/2002/GenModel";

	/**
	 * Documentation detail key in GenModel annotations.
	 */
	private static final String DOCUMENTATION_KEY = "documentation";

	/**
	 * Private constructor to encourage use of INSTANCE.
	 */
	private GenModelDocExtractor() {
	}

	/**
	 * Extracts GenModel documentation from an EAttribute referenced in the FeatureMapping.
	 * The function navigates the feature path in the mapping to find the target EAttribute,
	 * then extracts its GenModel documentation annotation.
	 *
	 * @param source The source EObject (typically not used directly, but passed for context)
	 * @param featureMapping The FeatureMapping containing the feature path to the EAttribute
	 * @return The documentation string, or null if not found
	 */
	@Override
	public Object apply(EObject source, FeatureMapping featureMapping) {
		if (featureMapping == null) {
			return null;
		}

		List<EStructuralFeature> featurePath = featureMapping.getFeaturePath();
		if (featurePath == null || featurePath.isEmpty()) {
			return null;
		}

		// Get the last feature in the path - this should be the EAttribute we want documentation for
		EStructuralFeature targetFeature = featurePath.get(featurePath.size() - 1);

		// Only process EAttributes
		if (!(targetFeature instanceof EAttribute)) {
			return null;
		}

		return extractDocumentation(targetFeature);
	}

	/**
	 * Extracts documentation from the GenModel EAnnotation of an EStructuralFeature.
	 *
	 * @param feature The EStructuralFeature to extract documentation from
	 * @return The documentation string, or null if not found
	 */
	private String extractDocumentation(EStructuralFeature feature) {
		if (feature == null) {
			return null;
		}

		// Look for GenModel EAnnotation
		EAnnotation genModelAnnotation = feature.getEAnnotation(GENMODEL_ANNOTATION);
		if (genModelAnnotation == null) {
			return null;
		}

		// Extract the documentation detail
		EMap<String, String> details = genModelAnnotation.getDetails();
		if (details != null && !details.isEmpty()) {
			String documentation = details.get(DOCUMENTATION_KEY);
			// Clean up the documentation (remove extra whitespace, normalize line breaks)
			if (documentation != null) {
				return documentation.trim();
			}
		}

		return null;
	}
}
