import Reqwest from 'reqwest';
import {reEstablishSession} from 'babel?presets[]=es2015!panda-session';

export function AuthedReqwest(reqwestBody) {
  return new Promise(function(resolve, reject) {
    Reqwest(reqwestBody)
      .then(res => {
        resolve(res);
      })
      .fail(err => {
        if (err.status == 419) {
          reEstablishSession("/reauth", 5000).then(
            res => {
                Reqwest(reqwestBody).then(res => resolve(res)).fail(err => reject(err));
            },
            error => {
              console.error(error);
            });
        } else {
          reject(err);
        }
      });
  });
}
