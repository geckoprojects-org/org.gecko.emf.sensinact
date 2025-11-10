/*
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
package org.eclipse.sensinact.mapping;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EStructuralFeature;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Service Mapping</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.sensinact.mapping.ServiceMapping#getResources <em>Resources</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionFeature <em>Collection Feature</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionIndex <em>Collection Index</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionFilter <em>Collection Filter</em>}</li>
 * </ul>
 *
 * @see org.eclipse.sensinact.mapping.MappingPackage#getServiceMapping()
 * @model
 * @generated
 */
@ProviderType
public interface ServiceMapping extends Mapping {
	/**
	 * Returns the value of the '<em><b>Resources</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.sensinact.mapping.ResourceMapping}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Resources</em>' containment reference list.
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getServiceMapping_Resources()
	 * @model containment="true"
	 * @generated
	 */
	EList<ResourceMapping> getResources();

	/**
	 * Returns the value of the '<em><b>Collection Feature</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional reference to a collection feature in the source object. When set, this service will map from a specific element within that collection, selected by collectionIndex or collectionFilter.
	 * This enables flexible mapping scenarios where different services map from different collections:
	 * - Service A maps from sourceObject.hourlyReports[0]
	 * - Service B maps from sourceObject.dailyReports[0]
	 * - Service C maps directly from sourceObject (no collection)
	 * 
	 * Example: In a weather scenario where WeatherReports contains both 'reports' and 'forecasts' collections, currentWeather service can map from reports[0] while weeklyForecast service maps from forecasts[0].
	 * 
	 * If not set, the service maps directly from the source instance passed to the mapper.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Collection Feature</em>' reference.
	 * @see #setCollectionFeature(EStructuralFeature)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getServiceMapping_CollectionFeature()
	 * @model
	 * @generated
	 */
	EStructuralFeature getCollectionFeature();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionFeature <em>Collection Feature</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Collection Feature</em>' reference.
	 * @see #getCollectionFeature()
	 * @generated
	 */
	void setCollectionFeature(EStructuralFeature value);

	/**
	 * Returns the value of the '<em><b>Collection Index</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * When collectionFeature is set, specifies which element index from that collection to use as the source for this service. Default is 0 (first element).
	 * For example, if collectionFeature points to 'reports', setting collectionIndex=0 uses reports[0], while collectionIndex=1 uses reports[1].
	 * This attribute is ignored if collectionFeature is not set.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Collection Index</em>' attribute.
	 * @see #setCollectionIndex(int)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getServiceMapping_CollectionIndex()
	 * @model default="0"
	 * @generated
	 */
	int getCollectionIndex();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionIndex <em>Collection Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Collection Index</em>' attribute.
	 * @see #getCollectionIndex()
	 * @generated
	 */
	void setCollectionIndex(int value);

	/**
	 * Returns the value of the '<em><b>Collection Filter</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Optional filter expression to select a specific element from the collection instead of using a fixed index. The expression syntax and evaluation mechanism are implementation-specific.
	 * If both collectionIndex and collectionFilter are set, collectionFilter takes precedence.
	 * This allows for more dynamic selection criteria, such as 'select the element with the earliest timestamp' or 'select the element where type=CURRENT'.
	 * This attribute is ignored if collectionFeature is not set.
	 * <!-- end-model-doc -->
	 * @return the value of the '<em>Collection Filter</em>' attribute.
	 * @see #setCollectionFilter(String)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getServiceMapping_CollectionFilter()
	 * @model
	 * @generated
	 */
	String getCollectionFilter();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ServiceMapping#getCollectionFilter <em>Collection Filter</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Collection Filter</em>' attribute.
	 * @see #getCollectionFilter()
	 * @generated
	 */
	void setCollectionFilter(String value);

} // ServiceMapping
