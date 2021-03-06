package org.sunbird.learner.actors.coursebatch.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.dao.UserCoursesDao;
import org.sunbird.models.user.courses.UserCourses;

/** Created by rajatgupta on 09/04/19. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectUtil.class, ElasticSearchUtil.class, ServiceFactory.class})
@PowerMockIgnore("javax.management.*")
public class UserCourseServiceTest {

  private CassandraOperationImpl cassandraOperation;
  @Mock private UserCoursesDao userCoursesDao;
  private UserCoursesService userCoursesService;

  @BeforeClass
  public static void setup() {}

  @Before
  public void beforeEachTest() throws Exception {

    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(ElasticSearchUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.whenNew(CassandraOperationImpl.class)
        .withNoArguments()
        .thenReturn(cassandraOperation);
    userCoursesService = new UserCoursesService();
  }

  @Test
  public void validateUserUnenrollEmptyUserCoursesTest() {
    try {
      UserCoursesService.validateUserUnenroll(null);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(ResponseCode.userNotEnrolledCourse.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateUserUnenrollAlreadyUnrolledTest() {
    try {
      UserCourses userCourses = new UserCourses();
      userCourses.setActive(false);
      UserCoursesService.validateUserUnenroll(userCourses);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(ResponseCode.userNotEnrolledCourse.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateUserUnenrollAlreadyCompletedTest() {
    try {
      UserCourses userCourses = new UserCourses();
      userCourses.setActive(true);
      userCourses.setLeafNodesCount(1);
      userCourses.setProgress(1);
      UserCoursesService.validateUserUnenroll(userCourses);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(ResponseCode.userAlreadyCompletedCourse.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void getPrimaryKeyTest() {
    Assert.assertEquals(
        UserCoursesService.getPrimaryKey(JsonKey.USER_ID, JsonKey.COURSE_ID, JsonKey.BATCH_ID),
        OneWayHashing.encryptVal(
            JsonKey.USER_ID
                + JsonKey.PRIMARY_KEY_DELIMETER
                + JsonKey.COURSE_ID
                + JsonKey.PRIMARY_KEY_DELIMETER
                + JsonKey.BATCH_ID));
  }

  @Test
  public void getPrimaryKeyMapTest() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, JsonKey.USER_ID);
    map.put(JsonKey.COURSE_ID, JsonKey.COURSE_ID);
    map.put(JsonKey.BATCH_ID, JsonKey.BATCH_ID);
    Assert.assertEquals(
        UserCoursesService.getPrimaryKey(map),
        OneWayHashing.encryptVal(
            JsonKey.USER_ID
                + JsonKey.PRIMARY_KEY_DELIMETER
                + JsonKey.COURSE_ID
                + JsonKey.PRIMARY_KEY_DELIMETER
                + JsonKey.BATCH_ID));
  }

  @Test
  public void getBatchSizeTest() {
    when(ProjectUtil.getConfigValue(JsonKey.CASSANDRA_WRITE_BATCH_SIZE)).thenReturn("100");
    Assert.assertEquals(
        new Integer(100), userCoursesService.getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE));
  }

  @Test
  public void getBatchSizeDefaultTest() {
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("");
    Assert.assertEquals(
        new Integer(10), userCoursesService.getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE));
  }

  @Test
  public void getActiveUserCourseTestSuccess() {
    when(ElasticSearchUtil.complexSearch(
            Mockito.anyObject(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new HashMap<>());
    Assert.assertNotEquals(null, userCoursesService.getActiveUserCourses(JsonKey.USER_ID));
  }

  @Test
  public void getActiveUserCourseFailure() {
    when(ElasticSearchUtil.complexSearch(
            Mockito.anyObject(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(null);
    Assert.assertEquals(null, userCoursesService.getActiveUserCourses(JsonKey.USER_ID));
  }
}
