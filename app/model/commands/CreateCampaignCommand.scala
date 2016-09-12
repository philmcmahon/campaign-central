package model.commands

import java.util.UUID

import org.joda.time.DateTime
import model.{Campaign, User}
import repositories.CampaignRepository


case class CreateCampaignCommand(
                                name: String,
                                status: String,
                                clientId: String,
                                nominalValue: Option[Long] = None,
                                actualValue: Option[Long] = None,
                                startDate: Option[DateTime] = None,
                                endDate: Option[DateTime] = None,
                                category: Option[String] = None,
                                targets: Map[String, Long] = Map.empty,
                                collaborators: List[User] = Nil
                                ) extends Command {

  type T = Campaign

  def process()(implicit user: Option[User] = None): Option[Campaign] = {

    val now = new DateTime

    val campaign = Campaign(
      id = UUID.randomUUID().toString,
      name = name,
      status = status,
      clientId = clientId,
      created = now,
      createdBy = user.getOrElse(User("Unknown", "User", "unknown.user")),
      lastModified = now,
      lastModifiedBy = user.getOrElse(User("Unknown", "User", "unknown.user")),
      nominalValue = nominalValue,
      actualValue = actualValue,
      startDate = startDate,
      endDate = endDate,
      category = category,
      targets = targets,
      collaborators = (collaborators ++ user).distinct
    )

    CampaignRepository.putCampaign(campaign)
  }
}
