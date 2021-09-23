import React from 'react';
import {HashRouter as Router, Route, Switch} from "react-router-dom";

import Index from './pages/index';
import Link from './pages/link'
import LoginCallback from "./pages/callback";
import Login from "./pages/callback/Login";

function App() {

  return (
      <>
        <div className="container mx-auto p-5">
          <div className="grid grid-cols-12">
            <div
                className="col-start-2 col-end-12 border-2 border-grey-100 p-2 text-center rounded-md shadow-sm">
              <Router>
                <Switch>
                  <Route path="/link/:uuid">
                    <Link missingCode={false}/>
                  </Route>
                  <Route path="/link" exact={true}>
                    <Link missingCode={true}/>
                  </Route>
                  <Route path="/callback">
                    <LoginCallback/>
                  </Route>
                  <Route path="/login">
                    <Login/>
                  </Route>
                  <Route path="/" exact={true}>
                    <Index/>
                  </Route>
                </Switch>
              </Router>
            </div>
          </div>
        </div>
      </>
  )
}

export default App
