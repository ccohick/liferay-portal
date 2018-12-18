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

package com.liferay.asset.display.page.internal.upgrade.v2_1_0;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.layout.page.template.model.LayoutPageTemplateEntry;
import com.liferay.layout.page.template.service.LayoutPageTemplateEntryLocalService;
import com.liferay.layout.page.template.service.LayoutPageTemplateEntryService;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.UnicodeProperties;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author Pavel Savinov
 */
public class UpgradeAssetDisplayLayout extends UpgradeProcess {

	public UpgradeAssetDisplayLayout(
		AssetEntryLocalService assetEntryLocalService,
		LayoutLocalService layoutLocalService,
		LayoutPageTemplateEntryLocalService layoutPageTemplateEntryLocalService,
		LayoutPageTemplateEntryService layoutPageTemplateEntryService) {

		_assetEntryLocalService = assetEntryLocalService;
		_layoutLocalService = layoutLocalService;
		_layoutPageTemplateEntryLocalService =
			layoutPageTemplateEntryLocalService;
		_layoutPageTemplateEntryService = layoutPageTemplateEntryService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		StringBundler sb = new StringBundler(3);

		sb.append("select assetDisplayPageEntryId, userId, groupId, ");
		sb.append("classNameId, classPK, layoutPageTemplateEntryId from ");
		sb.append("AssetDisplayPageEntry where plid is null or plid = 0");

		ServiceContext serviceContext = new ServiceContext();

		try (LoggingTimer loggingTimer = new LoggingTimer();
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(sb.toString());
			PreparedStatement ps = AutoBatchPreparedStatementUtil.autoBatch(
				connection.prepareStatement(
					"update AssetDisplayPageEntry set plid = ? where " +
						"assetDisplayPageEntryId = ?"))) {

			while (rs.next()) {
				long classNameId = rs.getLong("classNameId");
				long classPK = rs.getLong("classPK");

				AssetEntry assetEntry = _assetEntryLocalService.fetchEntry(
					classNameId, classPK);

				if (assetEntry == null) {
					continue;
				}

				long assetDisplayPageEntryId = rs.getLong(
					"assetDisplayPageEntryId");

				long userId = rs.getLong("userId");
				long groupId = rs.getLong("groupId");
				long layoutPageTemplateEntryId = rs.getLong(
					"layoutPageTemplateEntryId");

				ps.setLong(
					1,
					_getPlid(
						assetEntry, userId, groupId, layoutPageTemplateEntryId,
						serviceContext));

				ps.setLong(2, assetDisplayPageEntryId);

				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	private long _getPlid(
			AssetEntry assetEntry, long userId, long groupId,
			long layoutPageTemplateEntryId, ServiceContext serviceContext)
		throws PortalException {

		UnicodeProperties typeSettingsProperties = new UnicodeProperties();

		typeSettingsProperties.put("visible", Boolean.FALSE.toString());

		serviceContext.setAttribute(
			"layout.instanceable.allowed", Boolean.TRUE);

		LayoutPageTemplateEntry layoutPageTemplateEntry = Optional.ofNullable(
			_layoutPageTemplateEntryService.fetchLayoutPageTemplateEntry(
				layoutPageTemplateEntryId)
		).orElse(
			_layoutPageTemplateEntryService.fetchDefaultLayoutPageTemplateEntry(
				groupId, assetEntry.getClassNameId(),
				assetEntry.getClassTypeId())
		);

		if (layoutPageTemplateEntry.getPlid() > 0) {
			return layoutPageTemplateEntry.getPlid();
		}

		Layout layout = _layoutLocalService.addLayout(
			userId, groupId, false, 0, assetEntry.getTitleMap(),
			assetEntry.getTitleMap(), assetEntry.getDescriptionMap(), null,
			null, "asset_display", typeSettingsProperties.toString(), true,
			new HashMap<>(), serviceContext);

		layoutPageTemplateEntry.setPlid(layout.getPlid());

		_layoutPageTemplateEntryLocalService.updateLayoutPageTemplateEntry(
			layoutPageTemplateEntry);

		return layout.getPlid();
	}

	private final AssetEntryLocalService _assetEntryLocalService;
	private final LayoutLocalService _layoutLocalService;
	private final LayoutPageTemplateEntryLocalService
		_layoutPageTemplateEntryLocalService;
	private final LayoutPageTemplateEntryService
		_layoutPageTemplateEntryService;

}