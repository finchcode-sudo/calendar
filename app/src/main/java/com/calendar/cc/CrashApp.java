package com.calendar.cc;

import android.app.Application;
import android.content.Context;

public class CrashApp extends Application {

    // 运行批次号：IDE 编译时把当前 runId 烤进这一行（DebugHostSupport 优先读 code_slot/run_id）。
    private static String RUN_ID = "4a2514bcaf1f4fbc77201b78a6fb39c9";

    static String runId() { return RUN_ID; }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 调试容器代码槽：反射调用，正式包无 CodeSlotLoader 则忽略。
        try {
            Class<?> c = Class.forName(getClass().getPackage().getName() + ".CodeSlotLoader");
            c.getMethod("install", Context.class).invoke(null, this);
        } catch (Throwable ignored) {
            try {
                Class<?> c = Class.forName(getClass().getPackage().getName() + ".HotSwapLoader");
                c.getMethod("install", Context.class).invoke(null, this);
            } catch (Throwable ignored2) { }
        }
        try {
            Class<?> d = Class.forName(getClass().getPackage().getName() + ".DiagLogger");
            d.getMethod("init", Context.class).invoke(null, this);
        } catch (Throwable ignored) { }
        try {
            Class<?> s = Class.forName(getClass().getPackage().getName() + ".DebugHostSupport");
            s.getMethod("install", Context.class).invoke(null, this);
        } catch (Throwable ignored) { }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Class<?> d = Class.forName(getClass().getPackage().getName() + ".DiagLogger");
            d.getMethod("init", Context.class).invoke(null, this);
        } catch (Throwable ignored) { }
        try {
            Class<?> s = Class.forName(getClass().getPackage().getName() + ".DebugHostSupport");
            s.getMethod("install", Context.class).invoke(null, this);
        } catch (Throwable ignored) { }
    }
}
