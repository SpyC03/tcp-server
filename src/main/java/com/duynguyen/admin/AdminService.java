package com.duynguyen.admin;

import com.duynguyen.model.User;
import com.duynguyen.server.ServerManager;
import lombok.Getter;

public class AdminService {
    @Getter
    private static final AdminService INSTANCE = new AdminService();

    private AdminService() {
    }

    public void showServerInfo(User u) {
        long total, free, used;
        double mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        total = runtime.totalMemory();
        free = runtime.freeMemory();
        used = total - free;
        StringBuilder sb = new StringBuilder();
        sb.append("- Số người đang online: ").append(ServerManager.getNumberOnline()).append("\n");
        sb.append("- Memory usage (JVM): ")
                .append(String.format("%.1f/%.1f MB (%d%%)", used / mb, total / mb, (used * 100 / total))).append("\n");
        u.getService().showAlert("Thông tin", sb.toString());
    }
}
