package org.adempiere.ad.persistence;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.compiere.model.PO;

/**
 * Value getter handler.
 * 
 * e.g. org.compiere.model.I_C_Invoice.getGrandTotal()
 * 
 * @author tsa
 *
 */
/* package */class ValueGetterMethodInfo extends AbstractModelMethodInfo
{
	private static final Object DEFAULTVALUE_NotSupported = new Object();

	private final String propertyName;
	private final Class<?> returnType;
	private final Object defaultValue;

	public ValueGetterMethodInfo(final Method interfaceMethod, final String propertyName)
	{
		super(interfaceMethod);
		this.propertyName = propertyName;
		this.returnType = interfaceMethod.getReturnType();

		//
		// Default Value
		if (returnType == int.class)
		{
			this.defaultValue = Integer.valueOf(0);
		}
		else if (returnType == BigDecimal.class)
		{
			this.defaultValue = BigDecimal.ZERO;
		}
		else if (PO.class.isAssignableFrom(returnType))
		{
			// TODO: figure out which is this case
			this.defaultValue = DEFAULTVALUE_NotSupported;
		}
		else
		{
			this.defaultValue = null;
		}
	}

	@Override
	public Object invoke(final IModelInternalAccessor model, final Object[] methodArgs) throws Exception
	{
		Object value = null;
		final int idx = model.getColumnIndex(propertyName);
		if (idx >= 0)
		{
			value = model.getValue(propertyName, idx, returnType);
		}

		if (value != null)
		{
			return value;
		}

		if (defaultValue == DEFAULTVALUE_NotSupported)
		{
			throw new IllegalArgumentException("Method default value not supported - " + getInterfaceMethod());
		}
		return defaultValue;
	}

}
