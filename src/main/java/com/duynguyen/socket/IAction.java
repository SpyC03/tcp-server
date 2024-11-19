package com.duynguyen.socket;

import org.json.JSONObject;

interface IAction {

    abstract void call(JSONObject json);

}