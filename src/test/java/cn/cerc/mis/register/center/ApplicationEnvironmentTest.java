package cn.cerc.mis.register.center;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ApplicationEnvironmentTest {

    @Test
    public void testHostIP() {
        String hostIP = ApplicationEnvironment.hostIP();
        assertNotEquals("127.0.0.1", hostIP);
    }

}
