import { browserHistory } from 'react-router'
import {createCampaign as createCampaignApi} from '../../services/CampaignsApi';

function requestCampaignCreate(id, campaign) {
    return {
        type:       'CAMPAIGN_CREATE_REQUEST',
        id:         id,
        campaign:   campaign,
        receivedAt: Date.now()
    };
}

function recieveCampaignCreate(campaign) {
    browserHistory.push('/campaign/' + campaign.id);
    return {
        type:        'CAMPAIGN_CREATE_RECIEVE',
        campaign:    campaign,
        receivedAt:  Date.now()
    };
}

function errorCreatingCampaign(error) {
    return {
        type:       'SHOW_ERROR',
        message:    'Could not create campaign',
        error:      error,
        receivedAt: Date.now()
    };
}

export function createCampaign(id, campaign) {
    return dispatch => {
      dispatch(requestCampaignCreate(id, campaign));
      return createCampaignApi(id, campaign)
        .catch(error => dispatch(errorCreatingCampaign(error)))
        .then(res => {
          dispatch(recieveCampaignCreate(res));
        });
    };
}

export function populateEmptyCampaign() {
  return {
      type:        "CAMPAIGN_POPULATE_BLANK",
      campaign:    {
        name: "",
        status: "prospect",
        targets: []
      },
      receivedAt:  Date.now()
  };
}
