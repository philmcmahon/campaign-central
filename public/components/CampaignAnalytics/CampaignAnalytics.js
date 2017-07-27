import React, {PropTypes} from "react";
import CampaignPerformanceSummary from "./Analytics/CampaignPerformanceSummary";
import CampaignContentContributionPie from "./Analytics/CampaignContentContributionPie";
import CampaignDailyUniquesChart from "./Analytics/CampaignDailyUniquesChart";
import CampaignDailyTrafficChart from "./Analytics/CampaignDailyTrafficChart";
import CampaignPagesCumulativeTrafficChart from "./Analytics/CampaignPagesCumulativeTrafficChart";
import ContentTrafficChart from "./Analytics/ContentTrafficChart";
import CampaignQualifiedChart from "./Analytics/CampaignQualifiedChart";
import CampaignTrafficDriverStatsChart from "./Analytics/CampaignTrafficDriverStatsChart";

class CampaignAnalytics extends React.Component {

  isAnalysisAvailable(campaign) {
    const analysableStatus = campaign.status === 'live' || campaign.status === 'dead';
    return (analysableStatus && campaign.startDate && campaign.pathPrefix );
  }

  getLatestPageViews() {
    if(this.props.campaignPageViews) {
      return this.props.campaignPageViews.pageCountStats[this.props.campaignPageViews.pageCountStats.length - 1];
    }

    return undefined;
  }

  getLatestUniqueUsers() {
    if(this.props.campaignDailyUniques) {
      return this.props.campaignDailyUniques.dailyUniqueUsers[this.props.campaignDailyUniques.dailyUniqueUsers.length - 1];
    }

    return undefined;
  }

  render () {
    return (
      <div>
        Place holder
      </div>
    );
  }
}


//REDUX CONNECTIONS
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

function mapStateToProps(state) {
  return {
    campaignPageViews: state.campaignPageViews,
    campaignDailyUniques: state.campaignDailyUniques,
    campaignTargetsReport: state.campaignTargetsReport,
    campaignQualifiedReport: state.campaignQualifiedReport
  };
}

export default connect(mapStateToProps)(CampaignAnalytics);