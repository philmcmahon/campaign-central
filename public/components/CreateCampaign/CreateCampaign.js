import React, { PropTypes } from 'react'
import CampaignEdit from '../CampaignInformationEdit/CampaignEdit';

class CreateCampaign extends React.Component {

  componentDidMount() {
    this.props.campaignActions.populateEmptyCampaign();
  }

  createCampaign = () => {
    this.props.campaignActions.createCampaign(this.props.campaign);
  }

  render () {

    if (!this.props.campaign) {
      return false;
    }

    return (
      <div className="campaign">
        <h2>Create A Campaign</h2>
        <div className="campaign__row">
          <CampaignEdit campaign={this.props.campaign} updateCampaign={this.props.campaignActions.updateCampaign} hideTargets={true} saveCampaign={this.createCampaign}/>
        </div>
      </div>
    );
  }
}

//REDUX CONNECTIONS
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as updateCampaign from '../../actions/CampaignActions/updateCampaign';
import * as saveCampaign from '../../actions/CampaignActions/saveCampaign';
import * as createCampaign from '../../actions/CampaignActions/createCampaign';

function mapStateToProps(state) {
  return {
    campaign: state.campaign
  };
}

function mapDispatchToProps(dispatch) {
  return {
    campaignActions: bindActionCreators(Object.assign({}, updateCampaign, saveCampaign, createCampaign), dispatch)
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(CreateCampaign);
