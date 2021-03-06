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

package com.liferay.headless.delivery.internal.dto.v1_0.converter;

import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetLinkLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.headless.delivery.dto.v1_0.KnowledgeBaseArticle;
import com.liferay.headless.delivery.dto.v1_0.TaxonomyCategory;
import com.liferay.headless.delivery.dto.v1_0.converter.DTOConverter;
import com.liferay.headless.delivery.dto.v1_0.converter.DTOConverterContext;
import com.liferay.headless.delivery.internal.dto.v1_0.util.AggregateRatingUtil;
import com.liferay.headless.delivery.internal.dto.v1_0.util.CreatorUtil;
import com.liferay.headless.delivery.internal.dto.v1_0.util.ParentKnowledgeBaseFolderUtil;
import com.liferay.headless.delivery.internal.dto.v1_0.util.RelatedContentUtil;
import com.liferay.headless.delivery.internal.dto.v1_0.util.TaxonomyCategoryUtil;
import com.liferay.knowledge.base.model.KBArticle;
import com.liferay.knowledge.base.service.KBArticleService;
import com.liferay.knowledge.base.service.KBFolderService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.vulcan.util.TransformUtil;
import com.liferay.ratings.kernel.service.RatingsStatsLocalService;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Rubén Pulido
 */
@Component(
	property = "asset.entry.class.name=com.liferay.knowledge.base.model.KBArticle",
	service = {DTOConverter.class, KnowledgeBaseArticleDTOConverter.class}
)
public class KnowledgeBaseArticleDTOConverter implements DTOConverter {

	@Override
	public String getContentType() {
		return KnowledgeBaseArticle.class.getSimpleName();
	}

	@Override
	public KnowledgeBaseArticle toDTO(DTOConverterContext dtoConverterContext)
		throws Exception {

		KBArticle kbArticle = _kbArticleService.getLatestKBArticle(
			dtoConverterContext.getResourcePrimKey(),
			WorkflowConstants.STATUS_APPROVED);

		if (kbArticle == null) {
			return null;
		}

		return new KnowledgeBaseArticle() {
			{
				aggregateRating = AggregateRatingUtil.toAggregateRating(
					_ratingsStatsLocalService.fetchStats(
						KBArticle.class.getName(),
						kbArticle.getResourcePrimKey()));
				articleBody = kbArticle.getContent();
				creator = CreatorUtil.toCreator(
					_portal, _userLocalService.getUser(kbArticle.getUserId()));
				dateCreated = kbArticle.getCreateDate();
				dateModified = kbArticle.getModifiedDate();
				description = kbArticle.getDescription();
				encodingFormat = "text/html";
				friendlyUrlPath = kbArticle.getUrlTitle();
				id = kbArticle.getResourcePrimKey();
				keywords = ListUtil.toArray(
					_assetTagLocalService.getTags(
						KBArticle.class.getName(), kbArticle.getClassPK()),
					AssetTag.NAME_ACCESSOR);
				numberOfAttachments = Optional.ofNullable(
					kbArticle.getAttachmentsFileEntries()
				).map(
					List::size
				).orElse(
					0
				);
				numberOfKnowledgeBaseArticles =
					_kbArticleService.getKBArticlesCount(
						kbArticle.getGroupId(), kbArticle.getResourcePrimKey(),
						WorkflowConstants.STATUS_APPROVED);
				parentKnowledgeBaseFolderId = kbArticle.getKbFolderId();
				relatedContents = RelatedContentUtil.toRelatedContents(
					_assetEntryLocalService, _assetLinkLocalService,
					kbArticle.getModelClassName(),
					kbArticle.getResourcePrimKey(), _dtoConverterRegistry,
					dtoConverterContext.getLocale());
				siteId = kbArticle.getGroupId();
				taxonomyCategories = TransformUtil.transformToArray(
					_assetCategoryLocalService.getCategories(
						KBArticle.class.getName(), kbArticle.getClassPK()),
					TaxonomyCategoryUtil::toTaxonomyCategory,
					TaxonomyCategory.class);
				title = kbArticle.getTitle();

				setParentKnowledgeBaseFolder(
					() -> {
						if (kbArticle.getKbFolderId() <= 0) {
							return null;
						}

						return ParentKnowledgeBaseFolderUtil.
							toParentKnowledgeBaseFolder(
								_kbFolderService.getKBFolder(
									kbArticle.getKbFolderId()));
					});
			}
		};
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private AssetEntryLocalService _assetEntryLocalService;

	@Reference
	private AssetLinkLocalService _assetLinkLocalService;

	@Reference
	private AssetTagLocalService _assetTagLocalService;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private KBArticleService _kbArticleService;

	@Reference
	private KBFolderService _kbFolderService;

	@Reference
	private Portal _portal;

	@Reference
	private RatingsStatsLocalService _ratingsStatsLocalService;

	@Reference
	private UserLocalService _userLocalService;

}