package services

import model.CampaignPageViewsItem
import repositories.CampaignPageViewsRepository

object CampaignService {
  def getPageViews(campaignId: String): Seq[CampaignPageViewsItem] = {
    CampaignPageViewsRepository.getCampaignPageViews(campaignId)
  }
}
