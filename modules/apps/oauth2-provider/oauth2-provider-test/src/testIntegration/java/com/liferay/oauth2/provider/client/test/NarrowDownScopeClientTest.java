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

package com.liferay.oauth2.provider.client.test;

import com.liferay.oauth2.provider.constants.GrantType;
import com.liferay.oauth2.provider.test.internal.TestApplication;
import com.liferay.oauth2.provider.test.internal.activator.BaseTestPreparatorBundleActivator;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.PortalUtil;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Carlos Sierra Andrés
 */
@RunAsClient
@RunWith(Arquillian.class)
public class NarrowDownScopeClientTest extends BaseClientTestCase {

	@Deployment
	public static Archive<?> getArchive() throws Exception {
		return BaseClientTestCase.getArchive(
			NarrowDownScopeTestPreparatorBundleActivator.class);
	}

	@Test
	public void test() throws Exception {
		Assert.assertEquals(
			"GET",
			getToken(
				"oauthTestApplication", null,
				getAuthorizationCodeBiFunction(
					"test@liferay.com", "test", null, "GET"),
				this::parseScopeString));

		Assert.assertEquals(
			"GET",
			getToken(
				"oauthTestApplication", null,
				getClientCredentialsResponseBiFunction("GET"),
				this::parseScopeString));

		Assert.assertEquals(
			"GET",
			getToken(
				"oauthTestApplication", null,
				getResourceOwnerPasswordBiFunction(
					"test@liferay.com", "test", "GET"),
				this::parseScopeString));

		String scopeString = getToken(
			"oauthTestApplication", null,
			getResourceOwnerPasswordBiFunction("test@liferay.com", "test"),
			this::parseScopeString);

		Assert.assertEquals(
			new HashSet<>(Arrays.asList("GET", "POST")),
			new HashSet<>(Arrays.asList(scopeString.split(" "))));

		Assert.assertEquals(
			"invalid_grant",
			getToken(
				"oauthTestApplication", null,
				getResourceOwnerPasswordBiFunction(
					"test@liferay.com", "test", "GET POST PUT"),
				this::parseError));
	}

	public static class NarrowDownScopeTestPreparatorBundleActivator
		extends BaseTestPreparatorBundleActivator {

		@Override
		protected void prepareTest() throws Exception {
			long defaultCompanyId = PortalUtil.getDefaultCompanyId();

			User user = UserTestUtil.getAdminUser(defaultCompanyId);

			Dictionary<String, Object> properties = new HashMapDictionary<>();

			properties.put("oauth2.test.application", true);

			registerJaxRsApplication(
				new TestApplication(), "methods", properties);

			createOAuth2Application(
				defaultCompanyId, user, "oauthTestApplication",
				Arrays.asList(
					GrantType.AUTHORIZATION_CODE, GrantType.CLIENT_CREDENTIALS,
					GrantType.RESOURCE_OWNER_PASSWORD),
				Arrays.asList("GET", "POST"));
		}

	}

}