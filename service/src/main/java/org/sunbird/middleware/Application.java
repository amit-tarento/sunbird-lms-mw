package org.sunbird.middleware;

import org.sunbird.actor.core.CoreActorRegistry;
import org.sunbird.actor.router.BackgroundRequestRouter;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.badge.BadgeActorRegistry;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.learner.actors.BackgroundRequestRouterActor;
import org.sunbird.learner.actors.RequestRouterActor;
import org.sunbird.learner.util.SchedulerManager;
import org.sunbird.learner.util.Util;
import org.sunbird.user.UserActorRegistry;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * @author Amit Kumar
 * @author arvind
 * 
 *         Remote actor Application start point .
 */
public class Application {
	private static ActorSystem system;
	private static final String ACTOR_CONFIG_NAME = "RemoteMWConfig";
	private static final String ACTOR_LOCAL_CONFIG_NAME = "LocaleMWConfig";
	private static final String LOCAL_ACTOR_SYSTEM_NAME = "LocalMiddlewareActorSystem";
	private static final String REMOTE_ACTOR_SYSTEM_NAME = "RemoteMiddlewareActorSystem";
	private static final String BKG_ACTOR_CONFIG_NAME = "BackgroundRemoteMWConfig";
	private static final String BKG_ACTOR_LOCAL_CONFIG_NAME = "BackGroundLocalMWConfig";
	private static final String BKG_LOCAL_ACTOR_SYSTEM_NAME = "BackGroundLocalMiddlewareActorSystem";
	private static final String BKG_REMOTE_ACTOR_SYSTEM_NAME = "BackGroundRemoteMiddlewareActorSystem";
	private static PropertiesCache cache = PropertiesCache.getInstance();

	public static void main(String[] args) {
		SunbirdMWService.init();
		new ActorRegistry();
		checkCassandraConnection();
	}

	/**
	 * This method will do the basic setup for actors.
	 */
	private static void startRemoteActorSystem() {
		ProjectLogger.log("startRemoteCreationSystem method called....");
		Config con = null;
		String host = System.getenv(JsonKey.SUNBIRD_ACTOR_SERVICE_IP);
		String port = System.getenv(JsonKey.SUNBIRD_ACTOR_SERVICE_PORT);

		if (!ProjectUtil.isStringNullOREmpty(host) && !ProjectUtil.isStringNullOREmpty(port)) {
			con = ConfigFactory
					.parseString("akka.remote.netty.tcp.hostname=" + host + ",akka.remote.netty.tcp.port=" + port + "")
					.withFallback(ConfigFactory.load().getConfig(ACTOR_CONFIG_NAME));
		} else {
			con = ConfigFactory.load().getConfig(ACTOR_CONFIG_NAME);
		}
		system = ActorSystem.create(REMOTE_ACTOR_SYSTEM_NAME, con);
		ActorRef learnerActorSelectorRef = system.actorOf(Props.create(RequestRouterActor.class),
				RequestRouterActor.class.getSimpleName());

		RequestRouterActor.setSystem(system);

		ProjectLogger.log("normal remote ActorSelectorRef " + learnerActorSelectorRef);
		ProjectLogger.log("NORMAL ACTOR REMOTE SYSTEM STARTED " + learnerActorSelectorRef, LoggerEnum.INFO.name());
		checkCassandraConnection();
	}

	public static void checkCassandraConnection() {
		Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
		Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
		SchedulerManager.schedule();
	}

	public static ActorRef startLocalActorSystem() {
		try {
			system = ActorSystem.create(LOCAL_ACTOR_SYSTEM_NAME,
					ConfigFactory.load().getConfig(ACTOR_LOCAL_CONFIG_NAME));
			ActorRef learnerActorSelectorRef = system.actorOf(Props.create(RequestRouterActor.class),
					RequestRouterActor.class.getSimpleName());
			ProjectLogger.log("normal local ActorSelectorRef " + learnerActorSelectorRef);
			ProjectLogger.log("NORNAL ACTOR LOCAL SYSTEM STARTED " + learnerActorSelectorRef, LoggerEnum.INFO.name());
			checkCassandraConnection();
			PropertiesCache cache = PropertiesCache.getInstance();
			if ("local".equalsIgnoreCase(cache.getProperty("background_actor_provider"))) {
				ProjectLogger.log("Initializing Local Background Actor System");
				startBackgroundLocalActorSystem();
			}
			return learnerActorSelectorRef;
		} catch (Exception ex) {
			ProjectLogger.log(
					"Exception occurred while starting local Actor System in Application.java startLocalActorSystem method "
							+ ex);
		}
		return null;
	}

	/**
	 * This method will do the basic setup for actors.
	 */
	private static void startBackgroundRemoteActorSystem() {
		try {
			Config con = null;
			String host = System.getenv(JsonKey.BKG_SUNBIRD_ACTOR_SERVICE_IP);
			String port = System.getenv(JsonKey.BKG_SUNBIRD_ACTOR_SERVICE_PORT);

			if (!ProjectUtil.isStringNullOREmpty(host) && !ProjectUtil.isStringNullOREmpty(port)) {
				con = ConfigFactory
						.parseString(
								"akka.remote.netty.tcp.hostname=" + host + ",akka.remote.netty.tcp.port=" + port + "")
						.withFallback(ConfigFactory.load().getConfig(BKG_ACTOR_CONFIG_NAME));
			} else {
				con = ConfigFactory.load().getConfig(BKG_ACTOR_CONFIG_NAME);
			}
			ActorSystem system = ActorSystem.create(BKG_REMOTE_ACTOR_SYSTEM_NAME, con);
			ActorRef learnerActorSelectorRef = system.actorOf(Props.create(BackgroundRequestRouterActor.class),
					BackgroundRequestRouterActor.class.getSimpleName());
			
			system.actorOf(Props.create(BackgroundRequestRouter.class),
					BackgroundRequestRouter.class.getSimpleName());

			new CoreActorRegistry();
			new BadgeActorRegistry();
			
			ProjectLogger.log("start BkgRemoteCreationSystem method called....");
			ProjectLogger.log("bkgActorSelectorRef " + learnerActorSelectorRef);
			ProjectLogger.log("BACKGROUND ACTORS STARTED " + learnerActorSelectorRef, LoggerEnum.INFO.name());
			checkCassandraConnection();
		} catch (Exception ex) {
			ex.printStackTrace();
			ProjectLogger
					.log("Exception occurred while starting BackgroundRemoteActorSystem  in Application.java " + ex);
		}
	}

	public static ActorRef startBackgroundLocalActorSystem() {
		ActorSystem system = ActorSystem.create(BKG_LOCAL_ACTOR_SYSTEM_NAME,
				ConfigFactory.load().getConfig(BKG_ACTOR_LOCAL_CONFIG_NAME));
		ActorRef learnerActorSelectorRef = system.actorOf(Props.create(BackgroundRequestRouterActor.class),
				BackgroundRequestRouterActor.class.getSimpleName());
		ProjectLogger.log("BACKGROUND ACTOR LOCAL SYSTEM STARTED " + learnerActorSelectorRef, LoggerEnum.INFO.name());
		checkCassandraConnection();
		return learnerActorSelectorRef;
	}

}
