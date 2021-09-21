import React, {useEffect, useState} from 'react';
import {Button, ButtonType} from "../../components/buttons";
import './style.scss';
import DiscordLogo from './Discord-Logo-White.svg'
import {useHistory, useRouteMatch} from "react-router";
import axios from 'axios';

interface RouteParams {
  uuid: string
}

interface LinkProps {
  missingCode: boolean
}

const MissingCode: React.FC = () => {
  let history = useHistory();
  let [error, setError] = useState<string | null>(null)
  let [loading, setLoading] = useState<boolean>(false)
  let [code, setCode] = useState<string>("")

  const onFormSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!code) {
      setError("Code is required");
      return;
    }
    setError(null);
    setLoading(true);

    axios.get(`/api/code/${code}`).then(resp => {
      const uuid = resp.data;
      history.push(`/link/${uuid}`)
    }).catch(e => {
      setLoading(false);
      setError(e.response.data || "An unknown error occurred");
    });
  }

  return <>
    <p>
      Enter your link code in the box below
    </p>
    <div className="flex items-center justify-items-center justify-center mt-2">
      <form onSubmit={onFormSubmit}>
        <input type="text" className="border-gray-300 border-2 rounded-md p-2" disabled={loading}
               onChange={e => {
                 setCode(e.target.value);
                 setError(null)
               }} value={code} required={true}/>
        <Button buttonType={ButtonType.SUCCESS} className="ml-2" disabled={loading}
                type="submit">Submit</Button>
      </form>
    </div>
    {error && <div className="text-red-500">
      {error}
    </div>}
  </>
}

const HasCode: React.FC = () => {
  const match = useRouteMatch<RouteParams>()
  const uuid = match.params.uuid;

  const [authUrl, setAuthUrl] = useState("")

  useEffect(() => {
    axios.get(`/api/auth-url/${uuid}`).then(resp => {
      setAuthUrl(resp.data)
    })
  }, [])

  const receivePostedMessage = (event: MessageEvent<any>) => {
    if(event.origin !== window.origin) {
      return;
    }
  }

  const openPopUpWindow = () => {
    window.open(authUrl, 'test', 'menubar=no,toolbar=no,location=no')
    window.addEventListener("message", receivePostedMessage, false);
  }

  return <>
    <p>
      Click the button below to log in with Discord and link your accounts
    </p>
    <Button className="bg-blurple mt-3" buttonType={ButtonType.CUSTOM} disabled={!authUrl} onClick={openPopUpWindow}>
      <img src={DiscordLogo}
           alt="Discord Logo"
           className="w-6 h-6 inline mr-2"/> Log in with Discord
    </Button>
  </>
}

const Link: React.FC<LinkProps> = (props) => {
  let Component = props.missingCode ? MissingCode : HasCode
  return <>
    <h1>Link Your Accounts</h1>
    <Component/>
  </>
}

export default Link