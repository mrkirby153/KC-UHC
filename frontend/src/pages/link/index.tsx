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

  const [minecraftUser, setMCUser] = useState<MinecraftUser | null>(null)
  const [existingUser, setExistingUser] = useState<OauthUser | null>(null)

  useEffect(() => {
    axios.get(`/api/uuid/${uuid}`).then(resp => {
      setMCUser(resp.data)
    })
    axios.get(`/api/user/${uuid}`).then(resp => {
      setExistingUser(resp.data)
    }).catch(result => {
      // Ignore
    })
    window.localStorage.setItem('UUID', uuid);
  }, [])

  const receivePostedMessage = (event: MessageEvent<any>) => {
    if (event.origin !== window.origin) {
      return;
    }
    window.removeEventListener('message', receivePostedMessage);
    switch (event.data) {
      case 'LOGIN_SUCCESS':
        axios.get(`/api/user/${uuid}`).then(resp => {
          setExistingUser(resp.data)
        }).catch(result => {
          // Ignore
        })
        break;
    }
  }

  const openPopUpWindow = () => {
    window.open(`/#/login`, 'test', 'menubar=no,toolbar=no,location=no')
    window.addEventListener("message", receivePostedMessage, false);
  }

  return <>
    <p>
      Click the button below to log in with Discord and link your Discord account
    </p>
    <p>
      Your account will be linked with the Minecraft account <span
        className={"text-green-500 font-bold"}>{minecraftUser?.username || "Loading..."}</span>
    </p>
    {!existingUser &&
    <Button className="bg-blurple mt-3" buttonType={ButtonType.CUSTOM} onClick={openPopUpWindow}>
      <img src={DiscordLogo}
           alt="Discord Logo"
           className="w-6 h-6 inline mr-2"/> Log in with Discord
    </Button>}
    {existingUser && <>
      <p className="mt-3">
        Already linked to <span
          className="text-blue-400">{existingUser.username}#{existingUser.discrim}</span>. <a
          className="text-blue-700 hover:underline cursor-pointer"
          onClick={() => setExistingUser(null)}>Not You?</a>
      </p>
    </>}
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