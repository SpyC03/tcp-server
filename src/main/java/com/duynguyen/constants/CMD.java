package com.duynguyen.constants;

public class CMD {

    /* 
     * Message: 
     * + command: byte (main command)
     * + data: byte[]
     *  + command: byte (sub command)
     *  + data: byte[]
    */

    //main command for in game feat
    public static final byte IN_GAME = 1;
    //sub command
    public static final byte ITEM_BAG_CLEAR = -1;
    public static final byte ITEM_BAG_ADD = 1;
    public static final byte ME_UP_COIN_BAG = -2;
    public static final byte ME_LOAD_INFO = 2;
    public static final byte ME_LOAD_LEVEL = -3;
    public static final byte ME_LOAD_ALL = -3;
    public static final byte PLAYER_UP_EXP = -4;
    public static final byte PLAYER_LOAD_ALL = 4;
    public static final byte READY_GET_IN = -5;
    public static final byte CREATE_PLAYER = 5;
    public static final byte UPDATE_ENERGY = -6;


    //main command for not in game feat
    public static final byte NOT_IN_GAME = -1;
    //sub command for not in game
    public static final byte FULL_SIZE = 0;
    public static final byte LOGIN = -1;
    public static final byte REGISTER = 1;
    public static final byte CLIENT_INFO = -2;
    public static final byte GET_SESSION_ID = 2;
    public static final byte SERVER_DIALOG = -3;
    public static final byte SERVER_ALERT = 3;
    public static final byte SERVER_MESSAGE = -4;
    public static final byte SHOW_WAIT = 4;
    public static final byte ALERT_MESSAGE = 5;



}
