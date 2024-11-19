package com.duynguyen.constants;

public class CMD {

    /* 
     * Message: 
     * + command: byte (main command)
     * + data: byte[]
     *  + command: byte (sub command)
     *  + data: byte[]
    */

    //main command
    //for feature
    public static final byte SUB_COMMAND = -30;
    //for login, register, client info,...
    public static final byte NOT_LOGIN = -29;
    //for map
    public static final byte NOT_MAP = -28;

    //sub command
    public static final byte LOGIN = -127;
    public static final byte REGISTER = -126;
    public static final byte CLIENT_INFO = -125;
    public static final byte FULL_SIZE = -32;
    public static final byte NEW_MESSAGE = -109;
    public static final byte GET_SESSION_ID = -27;
    public static final byte SERVER_DIALOG = -26;
    public static final byte SERVER_ALERT = -25;
    public static final byte SERVER_MESSAGE = -24;
    public static final byte SHOW_WAIT = -74;
    public static final byte ALERT_MESSAGE = 53;

}
