import React, {useEffect, useState} from 'react';
import {Button, ButtonType} from "../../components/buttons";
import axios from 'axios'


const LoginCallback: React.FC = () => {
  const urlParams = new URLSearchParams(window.location.search)

  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (urlParams.get('state') && urlParams.get('code')) {
      const state = urlParams.get('state')
      const code = urlParams.get('code')

      const uuid = window.localStorage.getItem('UUID')
      axios.post(`/api/map/${uuid}`, {
        state, code
      }).then(resp => {
        // Close the window
        window.opener.postMessage('LOGIN_SUCCESS');
        window.close();
      }).catch(e => {
        setError(e.response.data.error || 'An unknown error occurred')
        console.log(e)
      })
    }
  }, [])

  const loginFail = () => {
    window.opener.postMessage('FAILED')
    window.close();
  }

  if (urlParams.get('error')) {
    return <>
      <h1>An error occurred!</h1>
      <p>
        An error occurred when linking your accounts!
      </p>
      <p>
        <span
            className="font-bold">{urlParams.get('error')}</span>: {urlParams.get('error_description')}
      </p>
      <Button buttonType={ButtonType.INFO} className="mt-3" onClick={loginFail}>Close
        Window</Button>
    </>
  }

  if (error) {
    return <>
      <h1>An error occurred!</h1>
      <p>
        An error occurred when linking your accounts!
      </p>
      <p>
        {error}
      </p>
      <Button buttonType={ButtonType.INFO} className="mt-3" onClick={loginFail}>Close
        Window</Button>
    </>
  }

  return <>
    {success && <>
      <p>
        You have been logged in
      </p>
      <Button buttonType={ButtonType.INFO} className="mt-3" onClick={() => {
        window.opener.postMessage('LOGIN_SUCCESS');
        window.close();
      }
      }>Close Window</Button>
    </>}
  </>
}

export default LoginCallback