package com.duynguyen.socket;

import com.duynguyen.model.User;
import com.duynguyen.server.ServerManager;
import com.duynguyen.utils.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class ForceOutAction implements IAction {
    @Override
    public void call(JSONObject json) {
        try {
            int userId = json.getInt("user_id");
            User user = ServerManager.findUserByUserID(userId);
            if (user != null && user.character != null) {
                if (!user.isCleaned) {
                    user.session.getService().serverDialog("Có người đăng nhập vào tài khoản của bạn, force out.");
                    user.session.disconnect();
                }
            }
        } catch (JSONException ex) {
            Log.error("Error get socket", ex);
        }
    }
}
