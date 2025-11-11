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

import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Resource Mapping</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getUnit <em>Unit</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getUnitFeature <em>Unit Feature</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getValueFeature <em>Value Feature</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getMid <em>Mid</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getExtraMetadata <em>Extra Metadata</em>}</li>
 *   <li>{@link org.eclipse.sensinact.mapping.ResourceMapping#getDescriptionMapping <em>Description Mapping</em>}</li>
 * </ul>
 *
 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping()
 * @model
 * @generated
 */
@ProviderType
public interface ResourceMapping extends EObject, EAttribute {
	/**
	 * Returns the value of the '<em><b>Unit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Unit</em>' attribute.
	 * @see #setUnit(String)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_Unit()
	 * @model
	 * @generated
	 */
	String getUnit();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ResourceMapping#getUnit <em>Unit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Unit</em>' attribute.
	 * @see #getUnit()
	 * @generated
	 */
	void setUnit(String value);

	/**
	 * Returns the value of the '<em><b>Unit Feature</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.EStructuralFeature}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Unit Feature</em>' reference list.
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_UnitFeature()
	 * @model
	 * @generated
	 */
	EList<EStructuralFeature> getUnitFeature();

	/**
	 * Returns the value of the '<em><b>Value Feature</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.EStructuralFeature}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Value Feature</em>' reference list.
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_ValueFeature()
	 * @model
	 * @generated
	 */
	EList<EStructuralFeature> getValueFeature();

	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' reference.
	 * @see #setTimestamp(TimestampMapping)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_Timestamp()
	 * @model required="true"
	 * @generated
	 */
	TimestampMapping getTimestamp();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ResourceMapping#getTimestamp <em>Timestamp</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' reference.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(TimestampMapping value);

	/**
	 * Returns the value of the '<em><b>Mid</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mid</em>' attribute.
	 * @see #setMid(String)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_Mid()
	 * @model id="true" required="true"
	 * @generated
	 */
	String getMid();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ResourceMapping#getMid <em>Mid</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mid</em>' attribute.
	 * @see #getMid()
	 * @generated
	 */
	void setMid(String value);

	/**
	 * Returns the value of the '<em><b>Extra Metadata</b></em>' map.
	 * The key is of type {@link java.lang.String},
	 * and the value is of type {@link java.lang.String},
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Extra Metadata</em>' map.
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_ExtraMetadata()
	 * @model mapType="org.eclipse.sensinact.mapping.StringToStringMap&lt;org.eclipse.emf.ecore.EString, org.eclipse.emf.ecore.EString&gt;"
	 * @generated
	 */
	EMap<String, String> getExtraMetadata();

	/**
	 * Returns the value of the '<em><b>Description Mapping</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description Mapping</em>' reference.
	 * @see #setDescriptionMapping(NameMapping)
	 * @see org.eclipse.sensinact.mapping.MappingPackage#getResourceMapping_DescriptionMapping()
	 * @model derived="true"
	 * @generated
	 */
	NameMapping getDescriptionMapping();

	/**
	 * Sets the value of the '{@link org.eclipse.sensinact.mapping.ResourceMapping#getDescriptionMapping <em>Description Mapping</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description Mapping</em>' reference.
	 * @see #getDescriptionMapping()
	 * @generated
	 */
	void setDescriptionMapping(NameMapping value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model eobjectRequired="true" unitFunctionDataType="org.eclipse.sensinact.mapping.JavaStringFunction" unitFunctionRequired="true"
	 * @generated
	 */
	void mapUnit(EObject eobject, Function<EObject, String> unitFunction);

} // ResourceMapping
