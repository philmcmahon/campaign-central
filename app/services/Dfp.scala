package services

import com.google.api.ads.common.lib.auth.OfflineCredentials
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api._
import com.google.api.ads.dfp.axis.factory.DfpServices
import com.google.api.ads.dfp.axis.utils.v201608.{ReportDownloader, StatementBuilder}
import com.google.api.ads.dfp.axis.v201608.Column._
import com.google.api.ads.dfp.axis.v201608.DateRangeType.REACH_LIFETIME
import com.google.api.ads.dfp.axis.v201608.Dimension._
import com.google.api.ads.dfp.axis.v201608.ExportFormat._
import com.google.api.ads.dfp.axis.v201608.{ReportDownloadOptions, ReportJob, ReportQuery, ReportServiceInterface, _}
import com.google.api.ads.dfp.lib.client.DfpSession
import play.api.Logger
import services.Config.conf._

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

object Dfp {

  System.setProperty("api.dfp.soapRequestTimeout", "300000")

  def mkSession(): DfpSession = {
    new DfpSession.Builder()
    .withOAuth2Credential(
      new OfflineCredentials.Builder()
      .forApi(DFP)
      .withClientSecrets(dfpClientId, dfpClientSecret)
      .withRefreshToken(dfpRefreshToken)
      .build()
      .generateCredential()
    )
    .withApplicationName(dfpAppName)
    .withNetworkCode(dfpNetworkCode)
    .build()
  }

  def fetchLineItemsByOrder(session: DfpSession, orderIds: Seq[Long]): Seq[LineItem] = {
    val lineItems = fetchLineItems(
      session,
      new StatementBuilder().where(s"orderId in (${orderIds.mkString(",")})").toStatement)
    lineItems getOrElse Nil
  }

  def fetchSuggestedLineItems(
    campaignName: String,
    clientName: String,
    session: DfpSession,
    orderIds: Seq[Long]
  ): Seq[LineItem] = {

    def fetch(nameCondition: String, orderId: Long): Seq[LineItem] = {
      Logger.info(s"Fetching line items to suggest in order $orderId with condition [$nameCondition]")
      val lineItems = fetchLineItems(
        session,
        new StatementBuilder()
        .where(s"orderId = :orderId AND $nameCondition")
        .withBindVariableValue("orderId", orderId)
        .toStatement
      )
      lineItems getOrElse Nil
    }

    def nameCondition(name: String) = s"name like '%$name%'"

    def pipedNameCondition(name: String) = nameCondition(s"| $name |")

    def splitSignificantWords(s: String): Seq[String] = {
      val words = s.split("\\s")
      words.map(_.trim.stripSuffix(":").toLowerCase)
      .filterNot(StopWords().contains)
    }

    lazy val first2SignificantWordsNameCondition =
      splitSignificantWords(campaignName).take(2).mkString("name like '%", " ", "%'")

    lazy val first3SignificantWordsNameCondition =
      splitSignificantWords(campaignName).distinct.take(3).mkString("name like '%", "%' AND name like '%", "%'")

    val fetches =
      orderIds.toStream.map(o => fetch(pipedNameCondition(campaignName), o)) #:::
      orderIds.toStream.map(o => fetch(pipedNameCondition(clientName), o)) #:::
      orderIds.toStream.map(o => fetch(nameCondition(campaignName), o)) #:::
      orderIds.toStream.map(o => fetch(nameCondition(clientName), o)) #:::
      orderIds.toStream.map(o => fetch(first2SignificantWordsNameCondition, o)) #:::
      orderIds.toStream.map(o => fetch(first3SignificantWordsNameCondition, o))

    fetches.find(_.nonEmpty) getOrElse Nil
  }

  private def fetchLineItems(session: DfpSession, statement: Statement): Try[Seq[LineItem]] = {
    val start = System.currentTimeMillis
    val lineItemService = new DfpServices().get(session, classOf[LineItemServiceInterface])
    val result = Try(lineItemService.getLineItemsByStatement(statement)) map { page =>

      // assuming only one page of results
      safeSeq(page.getResults)
    }
    result match {
      case Failure(e) =>
        Logger.error("Fetching line items failed", e)
      case Success(items) =>
        Logger.info(s"Fetched ${items.size} line items in ${System.currentTimeMillis - start} ms")
    }
    result
  }

  def fetchStatsReport(session: DfpSession, lineItemIds: Seq[Long]): Option[BufferedSource] = {

    if (lineItemIds.isEmpty) None

    else {

      val qry = new ReportQuery()
      qry.setDateRangeType(REACH_LIFETIME)
      qry.setStatement(
        new StatementBuilder()
        .where(s"LINE_ITEM_ID IN (${lineItemIds.mkString(",")})")
        .toStatement
      )
      qry.setDimensions(Array(DATE))
      qry.setColumns(
        Array(
          TOTAL_INVENTORY_LEVEL_IMPRESSIONS,
          TOTAL_INVENTORY_LEVEL_CLICKS
        )
      )

      val start = System.currentTimeMillis
      val report = fetchReport(session, qry)
      report match {
        case Failure(e) =>
          Logger.error(s"Stats report on line items ${lineItemIds.mkString(", ")} failed: ${e.getMessage}")
        case Success(_) =>
          Logger.info(
            s"Stats report on line items ${lineItemIds.mkString(", ")} took ${System.currentTimeMillis - start} ms"
          )
      }
      report.toOption
    }
  }

  private def fetchReport(session: DfpSession, qry: ReportQuery): Try[BufferedSource] = {

    val reportService = new DfpServices().get(session, classOf[ReportServiceInterface])

    val reportJob = {
      val job = new ReportJob()
      job.setReportQuery(qry)
      reportService.runReportJob(job)
    }

    val reportDownloader = new ReportDownloader(reportService, reportJob.getId)
    Try(reportDownloader.waitForReportReady()) map { completed =>

      val source = {
        val options = new ReportDownloadOptions()
        options.setExportFormat(CSV_DUMP)
        options.setUseGzipCompression(false)
        reportDownloader.getDownloadUrl(options)
      }

      Source.fromURL(source)
    }
  }

  def hasCampaignIdCustomFieldValue(campaignId: String)(lineItem: LineItem): Boolean = {
    safeSeq(lineItem.getCustomFieldValues) exists { value =>
      value.getCustomFieldId == dfpCampaignFieldId &&
      value.asInstanceOf[CustomFieldValue].getValue.asInstanceOf[TextValue].getValue.toLowerCase == campaignId
    }
  }

  private def safeSeq[T](ts: Array[T]): Seq[T] = Option(ts).map(_.toSeq).getOrElse(Nil)
}

object StopWords {

  val specificExtras = Seq("new", "test")

  // Taken from http://www.ranks.nl/stopwords
  def apply() = Seq(
    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at",
    "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
    "can't", "cannot", "could", "couldn't",
    "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
    "each",
    "few", "for", "from", "further",
    "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
    "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
    "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
    "let's",
    "me", "more", "most", "mustn't", "my", "myself",
    "no", "nor", "not",
    "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
    "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
    "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these",
    "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
    "under", "until", "up",
    "very",
    "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
    "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't",
    "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
  ) ++ specificExtras
}
