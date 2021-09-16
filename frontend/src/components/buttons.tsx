import React from 'react';

import './button.scss';

export enum ButtonType {
  SUCCESS,
  WARNING,
  ERROR,
  INFO
}

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  buttonType: ButtonType
}

export const Button: React.FunctionComponent<ButtonProps> = (props) => {
  let {buttonType, ...rest} = props
  let className = "";
  switch (props.buttonType) {
    case ButtonType.SUCCESS:
      className = "btn-success";
      break;
    case ButtonType.ERROR:
      className = "btn-error";
      break;
    case ButtonType.WARNING:
      className = "btn-warning"
      break;
    case ButtonType.INFO:
      className = "btn-info"
      break;
  }
  return <button className={`btn ${className}`} {...rest}/>
}