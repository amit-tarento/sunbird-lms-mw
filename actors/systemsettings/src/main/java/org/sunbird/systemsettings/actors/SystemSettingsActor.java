package org.sunbird.systemsettings.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.systemsettings.dao.impl.SystemSettingDaoImpl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ActorConfig(
        tasks = {"getSystemSetting", "getAllSystemSettings", "setSystemSetting"},
        asyncTasks = {}
)
public class SystemSettingsActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final SystemSettingDaoImpl systemSettingDaoImpl =
            new SystemSettingDaoImpl(cassandraOperation);

  @Override
  public void onReceive(Request request) throws Throwable {
    switch (request.getOperation()) {
      case "getSystemSetting":
        getSystemSetting(request);
        break;
      case "getAllSystemSettings":
        getAllSystemSettings();
        break;
      case "setSystemSetting":
        setSystemSetting(request);
        break;
      default:
        onReceiveUnsupportedOperation(request.getOperation());
        break;
    }
  }

  private void getSystemSetting(Request actorMessage) {
    String value = DataCacheHandler.getConfigSettings().get(actorMessage.getContext().get(JsonKey.FIELD));
    ProjectLogger.log("SystemSettingsActor:getSystemSetting: got field value from cache.",LoggerEnum.INFO.name());
    SystemSetting setting = null;
    if (value != null) {
      setting =
              new SystemSetting(
                      (String) actorMessage.getContext().get(JsonKey.FIELD),
                      (String) actorMessage.getContext().get(JsonKey.FIELD),
                      value);
    } else {
      setting =
              systemSettingDaoImpl.readByField((String) actorMessage.getContext().get(JsonKey.FIELD));
      ProjectLogger.log("SystemSettingsActor:getSystemSetting:got field value from db",LoggerEnum.INFO.name());
      if(null == setting){
          throw new ProjectCommonException(
                  ResponseCode.resourceNotFound.getErrorCode(),
                  ResponseCode.resourceNotFound.getErrorMessage(),
                  ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());

      }
      DataCacheHandler.getConfigSettings().put((String) actorMessage.getContext().get(JsonKey.FIELD), setting.getValue());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, setting);
    sender().tell(response, self());
  }

  private void getAllSystemSettings() {
    ProjectLogger.log("SystemSettingsActor: getAllSystemSettings called", LoggerEnum.DEBUG.name());
    Map<String, String> systemSettings = DataCacheHandler.getConfigSettings();
    Response response = new Response();
      List<SystemSetting> allSystemSettings = null;
    if (MapUtils.isNotEmpty(systemSettings)) {
        allSystemSettings = new ArrayList<>();
        for (Map.Entry setting : systemSettings.entrySet()) {
            allSystemSettings.add(new SystemSetting(
                    (String)setting.getKey(),
                    (String)setting.getKey(),
                    (String)setting.getValue()));
        }
    } else {
        allSystemSettings = systemSettingDaoImpl.readAll();
    }
      response.put(JsonKey.RESPONSE, allSystemSettings);
      sender().tell(response, self());
  }

  private void setSystemSetting(Request actorMessage) {
    ProjectLogger.log("SystemSettingsActor: setSystemSetting called", LoggerEnum.DEBUG.name());

    Map<String, Object> request = actorMessage.getRequest();
    String id = (String) request.get(JsonKey.ID);
    String field = (String) request.get(JsonKey.FIELD);
    if (JsonKey.PHONE_UNIQUE.equalsIgnoreCase(field)
            || JsonKey.EMAIL_UNIQUE.equalsIgnoreCase(field)
            || JsonKey.PHONE_UNIQUE.equalsIgnoreCase(id)
            || JsonKey.EMAIL_UNIQUE.equalsIgnoreCase(id)) {
      ProjectCommonException.throwClientErrorException(
              ResponseCode.errorUpdateSettingNotAllowed,
              MessageFormat.format(ResponseCode.errorUpdateSettingNotAllowed.getErrorMessage(), field));
    }
    ObjectMapper mapper = new ObjectMapper();
    SystemSetting systemSetting = mapper.convertValue(request, SystemSetting.class);
    Response response = systemSettingDaoImpl.write(systemSetting);
    sender().tell(response, self());
  }
}