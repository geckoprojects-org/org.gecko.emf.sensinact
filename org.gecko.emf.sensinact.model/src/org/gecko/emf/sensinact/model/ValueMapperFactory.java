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
package org.gecko.emf.sensinact.model;

import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sensinact.core.twin.SensinactDigitalTwin;
import org.eclipse.sensinact.mapping.FeatureMapping;
import org.eclipse.sensinact.mapping.ProviderMapping;
import org.gecko.emf.sensinact.model.impl.ValueMapperImpl;

/**
 * Factory for creating {@link ValueMapper} instances.
 *
 * @author Mark Hoffmann
 * @since 31.07.2025
 */
public class ValueMapperFactory {

    /**
     * Creates a new ValueMapper instance configured for the specified provider mapping and digital twin.
     *
     * @param twin The SensiNact digital twin to update with mapped values
     * @param mapping The ProviderMapping configuration defining the transformation rules
     * @return A configured ValueMapper instance
     * @throws IllegalArgumentException if twin or mapping is null
     */
    public static ValueMapper createValueMapper(SensinactDigitalTwin twin, ProviderMapping mapping) {
        return new ValueMapperImpl(twin, mapping);
    }

    /**
     * Creates a new ValueMapper instance with a custom function registry.
     * Functions in the registry can be invoked via the functionId attribute in FeatureMappings.
     *
     * <p><b>Note on Auto-Generated Resource Descriptions:</b></p>
     * <p>When using {@link org.eclipse.sensinact.mapping.ReferenceMapping} for automatic resource generation,
     * the ValueMapper will automatically extract GenModel documentation annotations and set them as resource
     * descriptions IF the "extractGenModelDoc" function is registered in the function registry.</p>
     *
     * <p>Example to enable automatic descriptions:</p>
     * <pre>{@code
     * Map<String, BiFunction<EObject, FeatureMapping, Object>> functions = new HashMap<>();
     * functions.put("extractGenModelDoc", GenModelDocExtractor.INSTANCE);
     * ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, mapping, functions);
     * }</pre>
     *
     * @param twin The SensiNact digital twin to update with mapped values
     * @param mapping The ProviderMapping configuration defining the transformation rules
     * @param functionRegistry Map of function services keyed by functionId
     * @return A configured ValueMapper instance
     * @throws IllegalArgumentException if twin or mapping is null
     * @see org.gecko.emf.sensinact.model.functions.GenModelDocExtractor
     */
    public static ValueMapper createValueMapper(SensinactDigitalTwin twin, ProviderMapping mapping,
            Map<String, BiFunction<EObject, FeatureMapping, Object>> functionRegistry) {
        return new ValueMapperImpl(twin, mapping, functionRegistry);
    }
}