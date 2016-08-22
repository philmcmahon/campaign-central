package repositories

import model.Note
import org.joda.time.DateTime
import play.api.Logger
import services.Dynamo

import scala.collection.JavaConversions._

object CampaignNotesRepository {

  def getNotesForCampaign(campaignId: String) = {
    Dynamo.campaignNotesTable.query("campaignId", campaignId).map{ Note.fromItem }.toList
  }

  def getNote(campaignId: String, created: DateTime) = {
    Option(Dynamo.campaignNotesTable.getItem("campaignId", campaignId, "created", created.getMillis)).map{ Note.fromItem }
  }

  def putNote(note: Note) = {
    try {
      Dynamo.campaignNotesTable.putItem(note.toItem)
      Some(note)
    } catch {
      case e: Error => {
        Logger.error(s"failed to persist note $note", e)
        None
      }
    }
  }

}
