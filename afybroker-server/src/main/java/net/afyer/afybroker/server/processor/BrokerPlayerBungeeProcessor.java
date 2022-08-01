package net.afyer.afybroker.server.processor;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AsyncUserProcessor;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.afyer.afybroker.core.BrokerGlobalConfig;
import net.afyer.afybroker.core.message.BrokerPlayerBungeeMessage;
import net.afyer.afybroker.server.BrokerServer;
import net.afyer.afybroker.server.aware.BrokerServerAware;
import net.afyer.afybroker.server.proxy.BrokerPlayer;
import net.afyer.afybroker.server.proxy.BrokerPlayerManager;

import java.util.concurrent.Executor;

/**
 * @author Nipuru
 * @since 2022/8/1 11:41
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrokerPlayerBungeeProcessor extends AsyncUserProcessor<BrokerPlayerBungeeMessage> implements BrokerServerAware {

    @Setter
    BrokerServer brokerServer;

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, BrokerPlayerBungeeMessage request) {

        if (BrokerGlobalConfig.openLog) {
            log.info("Received player message (player:{}, state:{}, clientName:{}",
                    request.getUid(), request.getState(), request.getClientName());
        }

        BrokerPlayerManager playerManager = brokerServer.getBrokerPlayerManager();

        switch (request.getState()) {
            case JOIN -> {
                //TODO 待验证：可能导致加入请求比连接请求更先送达？
                BrokerPlayer brokerPlayer = playerManager.getPlayer(request.getUid());
                if (brokerPlayer == null) {
                    return;
                }
                brokerPlayer.setBukkitServer(request.getClientName());
            }
            case CONNECT -> {
                BrokerPlayer brokerPlayer = new BrokerPlayer(brokerServer, request.getUid());
                brokerPlayer.setBungeeProxy(request.getClientName());
                playerManager.addPlayer(brokerPlayer);
            }
            case DISCONNECT -> playerManager.removePlayer(request.getUid());
        }
    }

    @Override
    public String interest() {
        return BrokerPlayerBungeeMessage.class.getName();
    }

    @Override
    public Executor getExecutor() {
        return brokerServer.getBizThread();
    }
}
