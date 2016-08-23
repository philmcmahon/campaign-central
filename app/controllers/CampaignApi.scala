package controllers

import java.util.UUID

import model._
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import com.gu.pandomainauth.model.{User => PandaUser}
import repositories.{CampaignRepository, GoogleAnalytics}

class CampaignApi(override val wsClient: WSClient) extends Controller with PandaAuthActions {

  def getAllCampaigns() = APIAuthAction { req =>
    Ok(Json.toJson(CampaignRepository.getAllCampaigns()))
  }

  def getCampaign(id: String) = APIAuthAction { req =>
    CampaignRepository.getCampaign(id) map { c => Ok(Json.toJson(c))} getOrElse NotFound
  }

  def getCampaignAnalytics(id: String) = APIAuthAction { req =>
    GoogleAnalytics.getAnalyticsForCampaign(id).flatten map { c => Ok(Json.toJson(c))} getOrElse NotFound
  }

  def bootstrapData() = APIAuthAction { req =>

    val user = loggedInUser(req.user)
    val now = new DateTime

    val campaigns = List(
      Campaign(
        id = UUID.randomUUID().toString,
        name = "Something about cars",
        status = "active",
        client = Client(UUID.randomUUID().toString, "Carmaker", "UK", Some(Agency(UUID.randomUUID().toString, "OMG"))),
        created = now,
        createdBy = user,
        lastModified = now,
        lastModifiedBy = user,
        nominalValue = Some(10000),
        actualValue = Some(0),
        targets = List(CampaignTarget("Unique users", 10000))
      ),Campaign(
        id = UUID.randomUUID().toString,
        name = "Pure hate",
        status = "prospect",
        client = Client(UUID.randomUUID().toString, "Nigel Trump", "UK", Some(Agency(UUID.randomUUID().toString, "Evil"))),
        created = now,
        createdBy = user,
        lastModified = now,
        lastModifiedBy = user,
        nominalValue = Some(10000),
        actualValue = Some(0),
        targets = List(CampaignTarget("Unique users", 10000))
      ),Campaign(
        id = UUID.randomUUID().toString,
        name = "TBC",
        status = "in production",
        client = Client(UUID.randomUUID().toString, "Babylon Zoo", "UK", Some(Agency(UUID.randomUUID().toString, "Local Host"))),
        created = now,
        createdBy = user,
        lastModified = now,
        lastModifiedBy = user,
        nominalValue = Some(10000),
        actualValue = Some(0),
        targets = List(CampaignTarget("Unique users", 10000))
      ),Campaign(
        id = UUID.randomUUID().toString,
        name = "I love it when a plan comes together",
        status = "dead",
        client = Client(UUID.randomUUID().toString, "A Team", "UK", Some(Agency(UUID.randomUUID().toString, "AWOL"))),
        created = now,
        createdBy = user,
        lastModified = now,
        lastModifiedBy = user,
        nominalValue = Some(10000),
        actualValue = Some(0),
        targets = List(CampaignTarget("Unique users", 10000))
      )
    )

    campaigns foreach( CampaignRepository.putCampaign )

    Ok("added 4 example campaigns")
  }

  def loggedInUser(pandaUser: PandaUser) = User(pandaUser.firstName, pandaUser.lastName, pandaUser.email)
}
