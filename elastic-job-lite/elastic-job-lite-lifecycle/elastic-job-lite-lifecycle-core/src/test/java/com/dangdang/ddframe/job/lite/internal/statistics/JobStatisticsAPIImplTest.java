/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.statistics;

import com.dangdang.ddframe.job.lite.api.JobStatisticsAPI;
import com.dangdang.ddframe.job.lite.domain.ExecutionInfo;
import com.dangdang.ddframe.job.lite.domain.JobBriefInfo;
import com.dangdang.ddframe.job.lite.domain.ServerInfo;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public final class JobStatisticsAPIImplTest {
    
    private JobStatisticsAPI jobStatisticsAPI;
    
    @Mock
    private CoordinatorRegistryCenter registryCenter;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        jobStatisticsAPI = new JobStatisticsAPIImpl(registryCenter);
    }
    
    @Test
    public void assertGetAllJobsBriefInfo() {
        when(registryCenter.getChildrenKeys("/")).thenReturn(Arrays.asList("test_job_1", "test_job_2"));
        String simpleJobJson1 =  "{\"jobName\":\"test_job_1\",\"jobClass\":\"com.dangdang.ddframe.job.lite.fixture.TestSimpleJob\",\"jobType\":\"SIMPLE\",\"cron\":\"0/1 * * * * ?\","
                + "\"shardingTotalCount\":3,\"shardingItemParameters\":\"0\\u003da,1\\u003db\",\"jobParameter\":\"param\",\"failover\":true,\"misfire\":false,\"description\":\"desc1\","
                + "\"jobProperties\":{\"executor_service_handler\":\"com.dangdang.ddframe.job.api.internal.executor.DefaultExecutorServiceHandler\","
                + "\"job_exception_handler\":\"com.dangdang.ddframe.job.api.internal.executor.DefaultJobExceptionHandler\"},"
                + "\"monitorExecution\":false,\"maxTimeDiffSeconds\":1000,\"monitorPort\":8888,\"jobShardingStrategyClass\":\"testClass\",\"disabled\":true,\"overwrite\":true}";
        String simpleJobJson2 =  "{\"jobName\":\"test_job_2\",\"jobClass\":\"com.dangdang.ddframe.job.lite.fixture.TestSimpleJob\",\"jobType\":\"SIMPLE\",\"cron\":\"0/1 * * * * ?\","
                + "\"shardingTotalCount\":3,\"shardingItemParameters\":\"0\\u003da,1\\u003db\",\"jobParameter\":\"param\",\"failover\":true,\"misfire\":false,\"description\":\"desc2\","
                + "\"jobProperties\":{\"executor_service_handler\":\"com.dangdang.ddframe.job.api.internal.executor.DefaultExecutorServiceHandler\","
                + "\"job_exception_handler\":\"com.dangdang.ddframe.job.api.internal.executor.DefaultJobExceptionHandler\"},"
                + "\"monitorExecution\":false,\"maxTimeDiffSeconds\":1000,\"monitorPort\":8888,\"jobShardingStrategyClass\":\"testClass\",\"disabled\":true,\"overwrite\":true}";
        when(registryCenter.get("/test_job_1/config")).thenReturn(simpleJobJson1);
        when(registryCenter.get("/test_job_2/config")).thenReturn(simpleJobJson2);
        when(registryCenter.getChildrenKeys("/test_job_1/servers")).thenReturn(Arrays.asList("ip1", "ip2"));
        when(registryCenter.getChildrenKeys("/test_job_2/servers")).thenReturn(Arrays.asList("ip3", "ip4"));
        when(registryCenter.get("/test_job_1/servers/ip1/status")).thenReturn("RUNNING");
        when(registryCenter.get("/test_job_1/servers/ip2/status")).thenReturn("READY");
        when(registryCenter.isExisted("/test_job_1/servers/ip2/disabled")).thenReturn(true);
        when(registryCenter.isExisted("/test_job_2/servers/ip3/paused")).thenReturn(true);
        when(registryCenter.isExisted("/test_job_2/servers/ip4/shutdown")).thenReturn(true);
        int i = 0;
        for (JobBriefInfo each : jobStatisticsAPI.getAllJobsBriefInfo()) {
            i++;
            assertThat(each.getJobName(), is("test_job_" + i));
            assertThat(each.getDescription(), is("desc" + i));
            assertThat(each.getCron(), is("0/1 * * * * ?"));
            switch (i) {
                case 1:
                    assertThat(each.getStatus(), is(JobBriefInfo.JobStatus.DISABLED));
                    break;
                case 2:
                    assertThat(each.getStatus(), is(JobBriefInfo.JobStatus.ALL_CRASHED));
                    break;
                default:
                    fail();
            }
        }
    }
    
    @Test
    public void assertGetServers() {
        when(registryCenter.getChildrenKeys("/test_job/servers")).thenReturn(Arrays.asList("ip1", "ip2"));
        when(registryCenter.get("/test_job/servers/ip1/hostName")).thenReturn("host1");
        when(registryCenter.get("/test_job/servers/ip2/hostName")).thenReturn("host2");
        when(registryCenter.get("/test_job/servers/ip1/processSuccessCount")).thenReturn("101");
        when(registryCenter.get("/test_job/servers/ip2/processSuccessCount")).thenReturn("102");
        when(registryCenter.get("/test_job/servers/ip1/processFailureCount")).thenReturn("11");
        when(registryCenter.get("/test_job/servers/ip2/processFailureCount")).thenReturn("12");
        when(registryCenter.get("/test_job/servers/ip1/sharding")).thenReturn("0,1");
        when(registryCenter.get("/test_job/servers/ip2/sharding")).thenReturn("2,3");
        when(registryCenter.get("/test_job/servers/ip1/status")).thenReturn("RUNNING");
        when(registryCenter.get("/test_job/servers/ip2/status")).thenReturn("READY");
        int i = 0;
        for (ServerInfo each : jobStatisticsAPI.getServers("test_job")) {
            i++;
            assertThat(each.getJobName(), is("test_job"));
            assertThat(each.getIp(), is("ip" + i));
            assertThat(each.getHostName(), is("host" + i));
            switch (i) {
                case 1:
                    assertThat(each.getProcessSuccessCount(), is(101));
                    assertThat(each.getProcessFailureCount(), is(11));
                    assertThat(each.getStatus(), is(ServerInfo.ServerStatus.RUNNING));
                    break;
                case 2:
                    assertThat(each.getProcessSuccessCount(), is(102));
                    assertThat(each.getProcessFailureCount(), is(12));
                    assertThat(each.getStatus(), is(ServerInfo.ServerStatus.READY));
                    break;
                default:
                    fail();
            }
        }
    }
    
    @Test
    public void assertGetExecutionInfoWithoutMonitorExecution() {
        when(registryCenter.isExisted("/test_job/execution")).thenReturn(false);
        assertTrue(jobStatisticsAPI.getExecutionInfo("test_job").isEmpty());
    }
    
    @Test
    public void assertGetExecutionInfoWithMonitorExecution() {
        when(registryCenter.isExisted("/test_job/execution")).thenReturn(true);
        when(registryCenter.getChildrenKeys("/test_job/execution")).thenReturn(Arrays.asList("0", "1", "2"));
        when(registryCenter.isExisted("/test_job/execution/0/running")).thenReturn(true);
        when(registryCenter.isExisted("/test_job/execution/1/running")).thenReturn(false);
        when(registryCenter.isExisted("/test_job/execution/1/completed")).thenReturn(true);
        when(registryCenter.isExisted("/test_job/execution/2/running")).thenReturn(false);
        when(registryCenter.isExisted("/test_job/execution/2/completed")).thenReturn(false);
        when(registryCenter.isExisted("/test_job/execution/0/failover")).thenReturn(false);
        when(registryCenter.isExisted("/test_job/execution/1/failover")).thenReturn(false);
        when(registryCenter.isExisted("/test_job/execution/2/failover")).thenReturn(true);
        when(registryCenter.get("/test_job/execution/2/failover")).thenReturn("ip0");
        when(registryCenter.get("/test_job/execution/0/lastBeginTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/1/lastBeginTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/2/lastBeginTime")).thenReturn(null);
        when(registryCenter.get("/test_job/execution/0/nextFireTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/1/nextFireTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/2/nextFireTime")).thenReturn(null);
        when(registryCenter.get("/test_job/execution/0/lastCompleteTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/1/lastCompleteTime")).thenReturn("0");
        when(registryCenter.get("/test_job/execution/2/lastCompleteTime")).thenReturn(null);
        int i = 0;
        for (ExecutionInfo each : jobStatisticsAPI.getExecutionInfo("test_job")) {
            i++;
            assertThat(each.getItem(), is(i - 1));
            switch (i) {
                case 1:
                    assertNull(each.getFailoverIp());
                    assertThat(each.getLastBeginTime(), is(new Date(0L)));
                    assertThat(each.getNextFireTime(), is(new Date(0L)));
                    assertThat(each.getLastCompleteTime(), is(new Date(0L)));
                    assertThat(each.getStatus(), is(ExecutionInfo.ExecutionStatus.RUNNING));
                    break;
                case 2:
                    assertNull(each.getFailoverIp());
                    assertThat(each.getLastBeginTime(), is(new Date(0L)));
                    assertThat(each.getNextFireTime(), is(new Date(0L)));
                    assertThat(each.getLastCompleteTime(), is(new Date(0L)));
                    assertThat(each.getStatus(), is(ExecutionInfo.ExecutionStatus.COMPLETED));
                    break;
                case 3:
                    assertThat(each.getFailoverIp(), is("ip0"));
                    assertNull(each.getLastBeginTime());
                    assertNull(each.getNextFireTime());
                    assertNull(each.getLastCompleteTime());
                    assertThat(each.getStatus(), is(ExecutionInfo.ExecutionStatus.PENDING));
                    break;
                default:
                    fail();
            }
        }
    }
}
