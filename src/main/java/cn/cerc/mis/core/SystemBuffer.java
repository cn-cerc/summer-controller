package cn.cerc.mis.core;

public class SystemBuffer {

    /**
     * key = ClassName + UserCode + Version
     */
    public enum UserObject implements IBufferKey {
        ClassName,
        Permissions,
        Permission;

        @Override
        public int getStartingPoint() {
            return 0;
        }

        @Override
        public int getMinimumNumber() {
            return 1;
        }

        @Override
        public int getMaximumNumber() {
            return 3;
        }

    }

    public enum Global implements IBufferKey {
        /**
         * 在线用户
         */
        OnlineUsers,
        /**
         * 缓存重置
         */
        CacheReset,
        /**
         * 错误网址
         */
        ErrorUrl,
        /**
         * 超时服务
         */
        ServiceTimeout,
        /**
         * 超时页面
         */
        ViewPageTimeout,
        /**
         * 超时Frm
         */
        FrmTimeout,
        /**
         * token
         */
        Token,
        /**
         * 异步服务标记位
         */
        QuartzMonitor,
        /**
         * 导出服务标记位
         */
        ExportMonitor,
        /**
         * 项目错误与警告日志
         */
        ErrorWarnLog,
        /**
         * 菜单测试报告
         */
        QCMenuReport,
        /**
         * 超时
         */
        Timeout;

        @Override
        public int getStartingPoint() {
            return 10;
        }

        @Override
        public int getMinimumNumber() {
            return 0;
        }

        @Override
        public int getMaximumNumber() {
            return 0;
        }
    }

    public enum Token implements IBufferKey {
        DeviceInfo,
        SessionBase,
        UserMessage,
        Map,
        ReLive;

        @Override
        public int getStartingPoint() {
            return 20;
        }

        @Override
        public int getMinimumNumber() {
            return 1;
        }

        @Override
        public int getMaximumNumber() {
            return 2;
        }

    }

    /**
     * key = UserCode + DeviceId / ExportKey
     */
    public enum User implements IBufferKey {
        SessionInfo,
        ExportKey,
        BackUrl,
        /**
         * 调用频次
         */
        Frequency;

        @Override
        public int getStartingPoint() {
            return 30;
        }

        @Override
        public int getMinimumNumber() {
            return 1;
        }

        @Override
        public int getMaximumNumber() {
            return 2;
        }

    }

    public enum SyncServer implements IBufferKey {
        Diteng,
        Diaoyou,
        Tieke,
        Heritage;

        @Override
        public int getStartingPoint() {
            return 40;
        }

        @Override
        public int getMinimumNumber() {
            return 0;
        }

        @Override
        public int getMaximumNumber() {
            return 0;
        }
    }

    /**
     * key = sessionId + MD5(dataIn)
     */
    public enum Service implements IBufferKey {
        BigData;

        @Override
        public int getStartingPoint() {
            return 50;
        }

        @Override
        public int getMinimumNumber() {
            return 3;
        }

        @Override
        public int getMaximumNumber() {
            return 3;
        }

    }

    /**
     * key = corpNo + machineCode
     */
    public enum PrintService implements IBufferKey {
        MachineCode,
        Number;

        @Override
        public int getStartingPoint() {
            return 60;
        }

        @Override
        public int getMinimumNumber() {
            return 1;
        }

        @Override
        public int getMaximumNumber() {
            return 2;
        }

    }

    /**
     * key = entity.class + [corpNo] + entity.values
     */

    public enum Entity implements IBufferKey {
        Cache;

        @Override
        public int getStartingPoint() {
            return 70;
        }

        @Override
        public int getMinimumNumber() {
            return 2;
        }

        @Override
        public int getMaximumNumber() {
            return 10;
        }
    }

}
