import React, {useEffect} from 'react';
import axios from "axios";

const BypassPopupBlocker: React.FC = () => {
  useEffect(() => {
    axios.get(`/api/auth-url/${localStorage.getItem('UUID')}`).then(resp => {
      window.location.replace(resp.data);
    })
  }, [])
  return <>Please wait...</>
}

export default BypassPopupBlocker;