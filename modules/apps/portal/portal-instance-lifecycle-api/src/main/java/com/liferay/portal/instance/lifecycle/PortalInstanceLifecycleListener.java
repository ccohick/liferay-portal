/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.instance.lifecycle;

import aQute.bnd.annotation.ProviderType;

import com.liferay.portal.kernel.model.Company;

/**
 * @author Michael C. Han
 */
@ProviderType
public interface PortalInstanceLifecycleListener {

	/**
	 * @deprecated As of Judson (7.1.x), with no direct replacement
	 */
	@Deprecated
	public void portalInstancePreregistered(long companyId);

	public default void portalInstancePreunregistered(Company company)
		throws Exception {
	}

	public void portalInstanceRegistered(Company company) throws Exception;

	public void portalInstanceUnregistered(Company company) throws Exception;

}