import React from 'react';
import {Router, Route, IndexRoute, browserHistory} from 'react-router';

import {getStore} from './util/storeAccessor';
import {hasPermission} from './util/verifyPermission';

import Main from './components/Main';
import Campaigns from './components/Campaigns/Campaigns';
import Campaign from './components/Campaign/Campaign';


function requirePermission(permissionName, nextState, replaceState) {
  if (!hasPermission(permissionName)) {
    replaceState(null, '/unauthorised');
  }
}

export const router = (
  <Router history={browserHistory}>
    <Route path="/" component={Main}>
      <Route path="/campaigns" component={Campaigns} />
      <Route path="/campaigns/:filterName" component={Campaigns} />
      <Route path="/campaign/:id" component={Campaign} />
    </Route>
  </Router>
);
