package repositories

import java.util.UUID

import model.{Campaign, CampaignWithSubItems, Note}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsValue
import services.Dynamo
import com.amazonaws.services.dynamodbv2.document.Item

import scala.collection.JavaConversions._

object CampaignRepository {

  def getCampaign(campaignId: String) = {
    Option(Dynamo.campaignTable.getItem("id", campaignId)).map{ Campaign.fromItem }
  }

  def getAllCampaigns() = {
    Dynamo.campaignTable.scan().map{ Campaign.fromItem }.toList
  }

  def getCampaignWithSubItems(campaignId: String) = {
    val campaign = Option(Dynamo.campaignTable.getItem("id", campaignId)).map{ Campaign.fromItem }

    campaign map { c =>
      CampaignWithSubItems(
        campaign = c,
        content = CampaignContentRepository.getContentForCampaign(c.id),
        notes = CampaignNotesRepository.getNotesForCampaign(c.id)
      )
    }
  }

  def putCampaign(campaign: Campaign) = {
    try {
      Dynamo.campaignTable.putItem(campaign.toItem)
      Some(campaign)
    } catch {
      case e: Error => {
        Logger.error(s"failed to persist campaign $campaign", e)
        None
      }
    }
  }

}
