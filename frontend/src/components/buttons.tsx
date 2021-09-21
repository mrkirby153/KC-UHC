import React from 'react';

import './button.scss';

export enum ButtonType {
  SUCCESS,
  WARNING,
  ERROR,
  INFO,
  CUSTOM
}

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  buttonType: ButtonType
}

export const Button: React.FunctionComponent<ButtonProps> = (props) => {
  let {buttonType, className, ...rest} = props
  let btnClassName = "btn ";
  switch (props.buttonType) {
    case ButtonType.SUCCESS:
      btnClassName += "btn-success";
      break;
    case ButtonType.ERROR:
      btnClassName += "btn-error";
      break;
    case ButtonType.WARNING:
      btnClassName += "btn-warning"
      break;
    case ButtonType.INFO:
      btnClassName += "btn-info"
      break;
  }
  if(className) {
    btnClassName += ` ${className}`
  }
  return <button className={btnClassName} {...rest}/>
}