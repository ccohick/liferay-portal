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

package com.liferay.bulk.rest.internal.resource.v1_0;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetCategoryModel;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.bulk.rest.dto.v1_0.DocumentBulkSelection;
import com.liferay.bulk.rest.dto.v1_0.TaxonomyCategory;
import com.liferay.bulk.rest.dto.v1_0.TaxonomyVocabulary;
import com.liferay.bulk.rest.internal.selection.v1_0.DocumentBulkSelectionFactory;
import com.liferay.bulk.rest.resource.v1_0.TaxonomyVocabularyResource;
import com.liferay.bulk.selection.BulkSelection;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.BaseModelPermissionCheckerUtil;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.vulcan.pagination.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Context;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Alejandro Tardín
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/taxonomy-vocabulary.properties",
	scope = ServiceScope.PROTOTYPE, service = TaxonomyVocabularyResource.class
)
public class TaxonomyVocabularyResourceImpl
	extends BaseTaxonomyVocabularyResourceImpl {

	@Override
	public Page<TaxonomyVocabulary>
			postContentSpaceTaxonomyVocabularyCommonPage(
				Long contentSpaceId,
				DocumentBulkSelection documentBulkSelection)
		throws Exception {

		Map<AssetVocabulary, List<AssetCategory>> assetCategoriesMap =
			_getAssetCategoriesMap(contentSpaceId, documentBulkSelection);

		return Page.of(
			transform(
				assetCategoriesMap.entrySet(),
				entry -> _toTaxonomyVocabulary(
					entry.getValue(), entry.getKey())));
	}

	private Function<AssetEntry, Set<AssetCategory>>
		_getAssetCategoriesFunction(PermissionChecker permissionChecker) {

		return assetEntry -> {
			if (!BaseModelPermissionCheckerUtil.containsBaseModelPermission(
					permissionChecker, assetEntry.getGroupId(),
					assetEntry.getClassName(), assetEntry.getClassPK(),
					ActionKeys.UPDATE)) {

				return Collections.emptySet();
			}

			return new HashSet<>(
				_assetCategoryLocalService.getCategories(
					assetEntry.getClassName(), assetEntry.getClassPK()));
		};
	}

	private Map<AssetVocabulary, List<AssetCategory>> _getAssetCategoriesMap(
			Long contentSpaceId, DocumentBulkSelection documentBulkSelection)
		throws Exception {

		Stream<AssetVocabulary> assetVocabulariesStream =
			_getAssetVocabulariesStream(contentSpaceId);

		Stream<AssetCategory> assetCategoriesStream = _getAssetCategoriesStream(
			documentBulkSelection);

		Map<Long, List<AssetCategory>> assetCategoriesMap =
			assetCategoriesStream.collect(
				Collectors.groupingBy(AssetCategoryModel::getVocabularyId));

		return assetVocabulariesStream.collect(
			Collectors.toMap(
				Function.identity(),
				assetVocabulary -> assetCategoriesMap.computeIfAbsent(
					assetVocabulary.getVocabularyId(),
					key -> new ArrayList<>())));
	}

	private Stream<AssetCategory> _getAssetCategoriesStream(
			DocumentBulkSelection documentBulkSelection)
		throws Exception {

		BulkSelection<?> bulkSelection = _documentBulkSelectionFactory.create(
			documentBulkSelection);

		BulkSelection<AssetEntry> assetEntryBulkSelection =
			bulkSelection.toAssetEntryBulkSelection();

		Stream<AssetEntry> stream = assetEntryBulkSelection.stream();

		Set<AssetCategory> assetCategories = stream.map(
			_getAssetCategoriesFunction(
				PermissionCheckerFactoryUtil.create(_user))
		).reduce(
			SetUtil::intersect
		).orElse(
			Collections.emptySet()
		);

		return assetCategories.stream();
	}

	private Stream<AssetVocabulary> _getAssetVocabulariesStream(
			Long contentSpaceId)
		throws Exception {

		List<AssetVocabulary> assetVocabularies =
			_assetVocabularyLocalService.getGroupVocabularies(
				_portal.getCurrentAndAncestorSiteGroupIds(contentSpaceId));

		Stream<AssetVocabulary> stream = assetVocabularies.stream();

		List<AssetVocabulary> filteredAssetVocabularies = stream.filter(
			assetVocabulary -> assetVocabulary.isAssociatedToClassNameId(
				_getClassNameId())
		).filter(
			assetVocabulary -> {
				int count =
					_assetCategoryLocalService.getVocabularyCategoriesCount(
						assetVocabulary.getVocabularyId());

				return count > 0;
			}
		).collect(
			Collectors.toList()
		);

		return filteredAssetVocabularies.stream();
	}

	private long _getClassNameId() {
		return _classNameLocalService.getClassNameId(
			DLFileEntry.class.getName());
	}

	private TaxonomyVocabulary _toTaxonomyVocabulary(
		List<AssetCategory> assetCategories, AssetVocabulary assetVocabulary) {

		return new TaxonomyVocabulary() {
			{
				multiValued = assetVocabulary.isMultiValued();
				name = assetVocabulary.getName();
				required = assetVocabulary.isRequired(
					_getClassNameId(),
					AssetCategoryConstants.ALL_CLASS_TYPE_PK);
				taxonomyCategories = transformToArray(
					assetCategories,
					assetCategory -> new TaxonomyCategory() {
						{
							taxonomyCategoryId = assetCategory.getCategoryId();
							taxonomyCategoryName = assetCategory.getName();
						}
					},
					TaxonomyCategory.class);
				taxonomyVocabularyId = assetVocabulary.getVocabularyId();
			}
		};
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

	@Reference
	private ClassNameLocalService _classNameLocalService;

	@Reference
	private DocumentBulkSelectionFactory _documentBulkSelectionFactory;

	@Reference
	private Portal _portal;

	@Context
	private User _user;

}